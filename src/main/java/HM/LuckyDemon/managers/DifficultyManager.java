package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPlugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DifficultyManager {
    private final HMPlugin plugin;
    private File difficultyFile;
    private FileConfiguration difficultyConfig;
    private int currentDay;
    private int difficultyLevel;
    private final Random random;
    // private final java.util.Set<java.util.UUID> transformedEntities = new
    // java.util.HashSet<>();

    public DifficultyManager(HMPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        loadDifficultyData();

        startAggressionTask();
    }

    private void loadDifficultyData() {
        difficultyFile = new File(plugin.getDataFolder(), "difficulty.yml");
        if (!difficultyFile.exists()) {
            try {
                difficultyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        difficultyConfig = YamlConfiguration.loadConfiguration(difficultyFile);
        // El día lo manda GameManager (fecha real): difficulty.yml queda atrasado si el día cambió con el server apagado
        setCurrentDay(GameManager.getInstance().getDay());
    }

    public void saveDifficultyData() {
        difficultyConfig.set("currentDay", currentDay);
        difficultyConfig.set("difficultyLevel", difficultyLevel);
        try {
            difficultyConfig.save(difficultyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void incrementDay() {
        currentDay++;
        if (currentDay >= 10) {
            difficultyLevel = (currentDay / 10);
        }
        saveDifficultyData();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int day) {
        this.currentDay = day;
        if (currentDay >= 10) {
            difficultyLevel = (currentDay / 10);
        } else {
            difficultyLevel = 0;
        }
        saveDifficultyData();

        // DÍA 30: murciélagos y calamares se cancelan al nacer, su tope nunca se llena y el juego lo reintenta
        // cada tick. Intentarlo cada 10 s quita ese trabajo (-1 = valor por defecto del server)
        int ticks = currentDay >= 30 ? TRANSFORMED_SPAWN_TICKS : -1;
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            world.setTicksPerSpawns(org.bukkit.entity.SpawnCategory.AMBIENT, ticks);
            world.setTicksPerSpawns(org.bukkit.entity.SpawnCategory.WATER_ANIMAL, ticks);
        }
    }

    private static final int TRANSFORMED_SPAWN_TICKS = 200;

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void startAggressionTask() {
        // Tarea repetitiva que corre cada 2 segundos
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (currentDay < 20)
                    return;

                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL
                            && player.getGameMode() != org.bukkit.GameMode.ADVENTURE)
                        continue;

                    // DÍA 40: ceguera 1 min bajo la lluvia. La tarea corre cada 2 s: 1/5000 = 1/10000 por segundo
                    if (currentDay >= 40 && player.getWorld().hasStorm()
                            && player.getLocation().getBlockY() >= player.getWorld()
                                    .getHighestBlockYAt(player.getLocation())
                            && random.nextInt(5000) == 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60, 0));
                    }

                    // DÍA 40: Ravagers de Granja que ya tuvieron un jugador a 96 bloques vuelven a despawnear lejos
                    if (currentDay >= 40) {
                        org.bukkit.NamespacedKey farmKey = new org.bukkit.NamespacedKey(plugin, "farm_ravager");
                        for (Ravager ravager : player.getLocation().getNearbyEntitiesByType(Ravager.class, 96)) {
                            if (!ravager.getRemoveWhenFarAway() && ravager.getPersistentDataContainer()
                                    .has(farmKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                                ravager.setRemoveWhenFarAway(true);
                            }
                        }
                    }

                    // Escanear entidades cercanas (15 bloques)
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(15, 15, 15)) {
                        // DÍA 40: endermans hostiles van por el jugador sin que los mires
                        if (entity instanceof Enderman enderman && enderman.getPersistentDataContainer().has(
                                new org.bukkit.NamespacedKey(plugin, "hostile_enderman"),
                                org.bukkit.persistence.PersistentDataType.BYTE)) {
                            if (enderman.getTarget() == null || !enderman.getTarget().isValid()) {
                                enderman.setTarget(player);
                            }
                            continue;
                        }


                        // Pigmans siempre enojados con el jugador que tengan cerca
                        if (entity instanceof PigZombie pigman) {
                            if (pigman.getTarget() == null || !pigman.getTarget().isValid()) {
                                pigman.setAngry(true);
                                pigman.setTarget(player);
                            }
                            continue;
                        }

                        if (entity instanceof Mob) {
                            Mob mob = (Mob) entity;

                            // Verificar si es un mob que debería ser agresivo
                            // En día 30+, NO procesar Squids ni Bats (se transforman)
                            boolean isDay30OrLater = currentDay >= 30;
                            boolean isSquidOrBat = (mob instanceof org.bukkit.entity.Squid)
                                    || (mob instanceof org.bukkit.entity.Bat);

                            if (mob instanceof Animals || mob instanceof AbstractVillager || mob instanceof Fish
                                    || mob instanceof Axolotl || mob instanceof Squid || mob instanceof Dolphin
                                    || mob instanceof org.bukkit.entity.Bat) {

                                // SKIP Squids y Bats en día 30+ (se transforman)
                                if (isDay30OrLater && isSquidOrBat) {
                                    continue;
                                }

                                // A) Darle la IA agresiva si no la tiene (se pierde al reiniciar o recargar el chunk)
                                makeAggressive(mob, 2.0);

                                // B) Si no tiene objetivo, forzarlo a atacar al jugador (no a su dueño)
                                if (isTamed(mob))
                                    continue;
                                if (mob.getTarget() == null || !mob.getTarget().isValid()) {
                                    mob.setTarget(player);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 40L);
    }

    // ========== EFECTOS A MOBS ==========

    public void applyMobEffects(LivingEntity entity) {
        if (currentDay >= 40 && replaceDay40Mob(entity)) {
            return; // Reemplazado por otro mob (ya se le aplicaron sus efectos)
        }
        if (currentDay >= 30) {
            applyDay30Effects(entity);
        } else if (currentDay >= 25) {
            applyDay25Effects(entity);
        } else if (currentDay >= 20) {
            applyDay20Effects(entity);
        } else if (difficultyLevel >= 1) {
            applyBaseEffects(entity);
        }
        // DÍA 20: Wardens el doble de rápidos (base fija, así aplicarlo dos veces no lo cuadruplica)
        if (currentDay >= 20 && entity.getType() == EntityType.WARDEN) {
            org.bukkit.attribute.AttributeInstance speed = entity
                    .getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getDefaultValue() * 2);
            }
        }
        // Después del 30 para que el nombre del día 40 no lo pise el del 30
        if (currentDay >= 40) {
            applyDay40Effects(entity);
        }
    }

    /** Nombre de mob especial (desde el día 30 cada mob modificado lleva el suyo). Oculto, sale en las muertes. */
    public static void setSpecialName(LivingEntity entity, String name,
            net.kyori.adventure.text.format.NamedTextColor color) {
        entity.customName(net.kyori.adventure.text.Component.text(name).color(color)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        entity.setCustomNameVisible(false);
    }

    private void applyBaseEffects(LivingEntity entity) {
        if (entity instanceof Spider) {
            applySpiderEffects((Spider) entity);
        }
        if (entity.getType().name().equals("CREAKING")) {
            applyCreakingEffects(entity);
        }
    }

    private void applyDay20Effects(LivingEntity entity) {
        if (entity instanceof Spider) {
            applyDay20SpiderEffects((Spider) entity);
            return;
        }
        if (entity instanceof Phantom) {
            applyPhantomEffects((Phantom) entity);
        }
        // DÍA 10 sigue hasta que el 25 lo reemplaza
        if (entity.getType().name().equals("CREAKING")) {
            applyCreakingEffects(entity);
        }
        if (entity instanceof Animals || entity instanceof org.bukkit.entity.Bat || entity instanceof Fish
                || entity instanceof Axolotl || entity instanceof Squid || entity instanceof Dolphin
                || entity instanceof AbstractVillager) {
            makeAggressive((Mob) entity, 2.0);
        }
        if (entity instanceof Ravager) {
            setupRavagerLoot((Ravager) entity);
        }
    }

    private void applyDay25Effects(LivingEntity entity) {
        // Aplicar efectos base del día 20 primero
        if (entity instanceof Spider) {
            applyDay25SpiderEffects((Spider) entity);
            return;
        }
        if (entity instanceof Phantom) {
            applyPhantomEffects((Phantom) entity);
        }
        // En día 30+, NO hacer agresivos a Squids y Bats (se transforman)
        boolean isDay30OrLater = currentDay >= 30;
        boolean isSquidOrBat = (entity instanceof org.bukkit.entity.Squid) || (entity instanceof org.bukkit.entity.Bat);

        if (!isDay30OrLater || !isSquidOrBat) {
            if (entity instanceof Animals || entity instanceof org.bukkit.entity.Bat || entity instanceof Fish
                    || entity instanceof Axolotl || entity instanceof Squid || entity instanceof Dolphin
                    || entity instanceof AbstractVillager) {
                makeAggressive((Mob) entity, 2.0);
            }
        }

        // DÍA 25: Creaking mejorado (Fuerza II, Velocidad III)
        if (entity.getType().name().equals("CREAKING")) {
            applyDay25CreakingEffects(entity);
        }

        // DÍA 25: Ravagers mejorados
        if (entity instanceof Ravager) {
            applyDay25RavagerEffects((Ravager) entity);
        }

        // DÍA 25: Giga Slimes (solo si es spawn natural, no división)
        if (entity instanceof Slime && !(entity instanceof MagmaCube)) {
            Slime slime = (Slime) entity;

            // 1. Verificar si ya fue marcado como "división" (SKIP)
            org.bukkit.NamespacedKey divisionKey = new org.bukkit.NamespacedKey(plugin, "slime_division");
            if (slime.getPersistentDataContainer().has(divisionKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                return; // Es una división, NO aplicar efectos gigantes
            }

            // 2. Verificar si ya fue marcado como "giga" (evitar doble aplicación)
            org.bukkit.NamespacedKey gigaKey = new org.bukkit.NamespacedKey(plugin, "giga_slime");
            if (slime.getPersistentDataContainer().has(gigaKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                return; // Ya fue procesado
            }

            // 3. Solo aplicar si es spawn natural (tamaño pequeño)
            if (slime.getSize() <= 4) {
                applyGigaSlimeEffects(slime);
            }
        }

        // DÍA 25: Giga Magma Cubes (solo si es spawn natural, no división)
        if (entity instanceof MagmaCube) {
            MagmaCube magmaCube = (MagmaCube) entity;

            // 1. Verificar si ya fue marcado como "división" (SKIP)
            org.bukkit.NamespacedKey divisionKey = new org.bukkit.NamespacedKey(plugin, "magma_division");
            if (magmaCube.getPersistentDataContainer().has(divisionKey,
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                return; // Es una división, NO aplicar efectos gigantes
            }

            // 2. Verificar si ya fue marcado como "giga" (evitar doble aplicación)
            org.bukkit.NamespacedKey gigaKey = new org.bukkit.NamespacedKey(plugin, "giga_magma");
            if (magmaCube.getPersistentDataContainer().has(gigaKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                return; // Ya fue procesado
            }

            // 3. Solo aplicar si es spawn natural (tamaño pequeño)
            if (magmaCube.getSize() <= 4) {
                applyGigaMagmaCubeEffects(magmaCube);
            }
        }

        // DÍA 25: Ghasts Demoníacos
        if (entity instanceof Ghast) {
            applyDemonicGhastEffects((Ghast) entity);
        }
    }

    // ========== MECÁNICAS DÍA 40 ==========

    private static final int HOSTILE_ENDERMAN_PERCENT = 10;
    private static final int MAX_ZOMBIE_NAUTILUS = 4;
    private static final double ZOMBIE_NAUTILUS_RADIUS = 64;

    /**
     * DÍA 40: al quitar el Nautilus su tope (criaturas de agua) nunca se llena y el juego sigue intentando generar
     * sin parar; con este tope de Zombie Nautilus cerca ya no se crean más.
     */
    public boolean zombieNautilusCapReached(org.bukkit.Location loc) {
        return currentDay >= 40 && loc.getNearbyEntitiesByType(org.bukkit.entity.ZombieNautilus.class,
                ZOMBIE_NAUTILUS_RADIUS).size() >= MAX_ZOMBIE_NAUTILUS;
    }

    /** DÍA 40: Nautilus ya existente -> Zombie Nautilus (el Drowned se lo pone applyDay40Effects), o nada si está el tope. */
    public void replaceNautilus(LivingEntity nautilus) {
        if (zombieNautilusCapReached(nautilus.getLocation())) {
            nautilus.remove();
        } else {
            replaceMob(nautilus, EntityType.ZOMBIE_NAUTILUS);
        }
    }

    /** DÍA 40: cambia el mob por otro. Devuelve true si lo reemplazó. Corre antes de los efectos del 30. */
    private boolean replaceDay40Mob(LivingEntity entity) {
        if (entity.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "transformed_mob"),
                org.bukkit.persistence.PersistentDataType.BYTE)) {
            return false;
        }
        // getType() y no instanceof: CaveSpider es Spider y Husk/Drowned son Zombie (se reemplazarían en bucle)
        switch (entity.getType()) {
            case SPIDER -> replaceMob(entity, EntityType.CAVE_SPIDER); // el nombre se lo pone applyDay40Effects
            case ZOMBIE -> {
                LivingEntity vindicator = replaceMob(entity, EntityType.VINDICATOR);
                vindicator.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0));
                org.bukkit.attribute.AttributeInstance maxHealth = vindicator
                        .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                maxHealth.setBaseValue(maxHealth.getBaseValue() * 2);
                vindicator.setHealth(maxHealth.getBaseValue());
                setSpecialName(vindicator, "Vindicator Furioso", net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN);
            }
            case WOLF -> replaceMob(entity, EntityType.CAT); // el gato sale Supernova (applyDay40Effects)
            case NAUTILUS -> replaceNautilus(entity); // los naturales ni llegan: PlayerListener.onCreatureSpawn
            case COW, MOOSHROOM, SHEEP, PIG, CHICKEN -> {
                LivingEntity ravager = replaceMob(entity, EntityType.RAVAGER);
                // Los chunks nuevos se generan a ~160 bloques y un monstruo a más de 128 de un jugador se borra al
                // instante: nace sin despawn y el escáner se lo devuelve cuando un jugador pasa cerca
                ravager.setRemoveWhenFarAway(false);
                ravager.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "farm_ravager"),
                        org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                setSpecialName(ravager, "Ravager de Granja", net.kyori.adventure.text.format.NamedTextColor.GOLD);
            }
            case ENDERMAN -> {
                if (entity.getWorld().getEnvironment() != org.bukkit.World.Environment.NETHER) {
                    return false;
                }
                applyEnderCreeperEffects((Creeper) replaceMob(entity, EntityType.CREEPER));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** DÍA 40: buffs y nombres. Corre después de los efectos del 30. */
    private void applyDay40Effects(LivingEntity entity) {
        int forever = Integer.MAX_VALUE;
        switch (entity.getType()) {
            case ENDERMAN -> {
                if (random.nextInt(100) < HOSTILE_ENDERMAN_PERCENT) {
                    entity.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "hostile_enderman"),
                            org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                    setSpecialName(entity, "Enderman Hostil", net.kyori.adventure.text.format.NamedTextColor.DARK_RED);
                }
            }
            case CREEPER -> {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, forever, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, forever, 1));
            }
            case IRON_GOLEM -> entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, forever, 0));
            case GUARDIAN, ELDER_GUARDIAN -> {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, forever, 2));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, forever, 1));
            }
            case PHANTOM -> {
                if (entity.getPassengers().isEmpty()) {
                    entity.addPassenger(spawnClassSkeleton(entity.getLocation(), random.nextInt(5) + 1));
                }
                setSpecialName(entity, "Phantom Jinete", net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE);
            }
            case CAVE_SPIDER -> setSpecialName(entity, "Araña de Cueva Jinete",
                    net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE);
            case CAT, OCELOT -> makeSupernovaCat(entity);
            case RABBIT -> {
                // 1 de vida pero Speed 50 y 10 de daño (AggressiveMobGoal usa el atributo si pasa de 2)
                entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(1);
                entity.setHealth(1);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, forever, 49));
                if (entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE) == null) {
                    entity.registerAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                }
                entity.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(10);
            }
            case ARMADILLO -> {
                // 500 de vida; al morir suelta un stack de Blaze Rod o de bloques de Lapis (Day40MobListener)
                entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(500);
                entity.setHealth(500);
                entity.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "rich_armadillo"),
                        org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                setSpecialName(entity, "Armadillo Acorazado", net.kyori.adventure.text.format.NamedTextColor.GOLD);
            }
            case SULFUR_CUBE -> {
                ((SulfurCube) entity).setEquipped(new org.bukkit.inventory.ItemStack(org.bukkit.Material.MAGMA_BLOCK));
                setSpecialName(entity, "Sulfur Cube Ardiente", net.kyori.adventure.text.format.NamedTextColor.GOLD);
            }
            case ZOMBIE_NAUTILUS -> {
                if (entity.getPassengers().isEmpty()) {
                    entity.addPassenger(entity.getWorld().spawnEntity(entity.getLocation(), EntityType.DROWNED));
                }
                setSpecialName(entity, "Nautilus Zombie Jinete", net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA);
            }
            case WITCH -> {
                // Pociones: Day40MobListener
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, forever, 1));
                org.bukkit.attribute.AttributeInstance maxHealth = entity
                        .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                maxHealth.setBaseValue(maxHealth.getBaseValue() * 2);
                entity.setHealth(maxHealth.getBaseValue());
                setSpecialName(entity, "Bruja Imposible", net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE);
            }
            case GHAST -> {
                // 75% de los Ghasts Demoníacos (los del End son Ender Ghast). Bolas: Day40MobListener
                if (entity.getWorld().getEnvironment() != org.bukkit.World.Environment.THE_END
                        && random.nextInt(100) < 75) {
                    entity.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "floating_demon"),
                            org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                    setSpecialName(entity, "Demonio Flotante", net.kyori.adventure.text.format.NamedTextColor.GOLD);
                }
            }
            case ZOMBIFIED_PIGLIN -> {
                if (random.nextInt(100) < MOUNTED_PIGMAN_PERCENT) {
                    mountPigman(entity);
                }
            }
            default -> {
            }
        }
    }

    private static final int MOUNTED_PIGMAN_PERCENT = 5;

    /** DÍA 40: Pigman montado en Cerdo, Abeja, Ghast, Magma Cube o Ultra Ravager (20% cada uno, como el original). */
    private void mountPigman(LivingEntity pigman) {
        org.bukkit.Location loc = pigman.getLocation();
        String name;
        LivingEntity mount;
        switch (random.nextInt(5)) {
            case 4 -> {
                spawnUltraRavager(pigman);
                return;
            }
            case 0 -> {
                mount = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.PIG);
                name = "Pigman Jinete de Cerdo";
            }
            case 1 -> {
                Bee bee = (Bee) loc.getWorld().spawnEntity(loc, EntityType.BEE);
                bee.setAnger(Integer.MAX_VALUE);
                mount = bee;
                name = "Pigman Jinete de Abeja";
                // Armadura de cuero amarilla en vez de la de diamante (como en el original)
                org.bukkit.inventory.EntityEquipment eq = pigman.getEquipment();
                org.bukkit.Material[] pieces = { org.bukkit.Material.LEATHER_HELMET,
                        org.bukkit.Material.LEATHER_CHESTPLATE, org.bukkit.Material.LEATHER_LEGGINGS,
                        org.bukkit.Material.LEATHER_BOOTS };
                org.bukkit.inventory.ItemStack[] armor = new org.bukkit.inventory.ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armor[i] = new org.bukkit.inventory.ItemStack(pieces[i]);
                    armor[i].editMeta(org.bukkit.inventory.meta.LeatherArmorMeta.class,
                            m -> m.setColor(org.bukkit.Color.YELLOW));
                }
                eq.setHelmet(armor[0]);
                eq.setChestplate(armor[1]);
                eq.setLeggings(armor[2]);
                eq.setBoots(armor[3]);
            }
            case 2 -> {
                mount = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.GHAST);
                name = "Pigman Jinete de Ghast";
                pigman.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
                pigman.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(8.0);
            }
            default -> {
                MagmaCube cube = (MagmaCube) loc.getWorld().spawnEntity(loc, EntityType.MAGMA_CUBE);
                cube.setSize(1);
                mount = cube;
                name = "Pigman Jinete de Magma";
            }
        }
        mount.addPassenger(pigman);
        setSpecialName(pigman, name, net.kyori.adventure.text.format.NamedTextColor.GOLD);
    }

    /**
     * DÍA 40: Ultra Ravager (250 corazones, Speed II, Fuerza II) con Carlos el Esclavo (el pigman, 75 corazones,
     * cota Protección II y espada Empuje X) y encima Jess la Emperatriz (aldeana, 120 corazones).
     * Drops por la marca day40_boss en Day40MobListener: totem, 32 lingotes de oro y 2 manzanas doradas.
     */
    private void spawnUltraRavager(LivingEntity carlos) {
        org.bukkit.Location loc = carlos.getLocation();
        LivingEntity ravager = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
        LivingEntity jess = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        setBoss(ravager, "ultra_ravager", "Ultra Ravager", 500);
        setBoss(carlos, "carlos", "Carlos el Esclavo", 150);
        setBoss(jess, "jess", "Jess la Emperatriz", 240);
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));

        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_SWORD);
        sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK, 10);
        org.bukkit.inventory.EntityEquipment eq = carlos.getEquipment();
        // Cota de malla completa con Protección II
        org.bukkit.inventory.ItemStack[] armor = {
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_BOOTS),
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_LEGGINGS),
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_CHESTPLATE),
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_HELMET) };
        for (org.bukkit.inventory.ItemStack piece : armor) {
            piece.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 2);
        }
        eq.setArmorContents(armor);
        eq.setItemInMainHand(sword);
        eq.setHelmetDropChance(0);
        eq.setChestplateDropChance(0);
        eq.setLeggingsDropChance(0);
        eq.setBootsDropChance(0);
        eq.setItemInMainHandDropChance(0);

        carlos.addPassenger(jess);
        ravager.addPassenger(carlos);
    }

    private void setBoss(LivingEntity mob, String id, String name, double health) {
        mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(health);
        mob.setHealth(health);
        mob.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "day40_boss"),
                org.bukkit.persistence.PersistentDataType.STRING, id);
        setSpecialName(mob, name, net.kyori.adventure.text.format.NamedTextColor.GREEN);
    }

    // Potencia 200 como el original: congela el server unos segundos al explotar (Nacho lo quiere así)
    private static final float SUPERNOVA_POWER = 200f;
    private final java.util.Set<java.util.UUID> supernovaCats = new java.util.HashSet<>();

    /** DÍA 40: Gato Supernova. Se anuncia y explota a los 30 s si sigue vivo. Máximo 2 a la vez. */
    private void makeSupernovaCat(LivingEntity cat) {
        if (supernovaCats.contains(cat.getUniqueId())) {
            return;
        }
        if (Bukkit.getOnlinePlayers().isEmpty() || supernovaCats.size() >= 2) {
            cat.remove();
            return;
        }
        supernovaCats.add(cat.getUniqueId());
        setSpecialName(cat, "Gato Supernova", net.kyori.adventure.text.format.NamedTextColor.GOLD);

        org.bukkit.Location loc = cat.getLocation();
        org.bukkit.Chunk chunk = loc.getChunk();
        chunk.addPluginChunkTicket(plugin); // que no se descargue antes de explotar
        Bukkit.broadcast(HM.LuckyDemon.utils.MessageUtils.format("<red>Un Gato Supernova va a explotar en: "
                + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            supernovaCats.remove(cat.getUniqueId());
            chunk.removePluginChunkTicket(plugin);
            if (!cat.isValid()) {
                return; // lo mataron a tiempo
            }
            cat.getWorld().createExplosion(cat.getLocation(), SUPERNOVA_POWER, true, true, cat);
            cat.remove();
        }, 20L * 30);
    }

    /** Cambia un mob por otro del tipo dado en el mismo lugar y le aplica los efectos del día. */
    private LivingEntity replaceMob(LivingEntity old, EntityType type) {
        LivingEntity mob = (LivingEntity) old.getWorld().spawnEntity(old.getLocation(), type);
        old.remove();
        applyMobEffects(mob);
        return mob;
    }

    private void applyDay30Effects(LivingEntity entity) {
        // Verificar si el mob ya fue transformado (evitar loops infinitos)
        org.bukkit.NamespacedKey transformedKey = new org.bukkit.NamespacedKey(plugin, "transformed_mob");
        if (entity.getPersistentDataContainer().has(transformedKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
            return; // Ya fue procesado, no hacer nada
        }

        // IMPORTANTE: NO marcar Bats ni Squids aquí, porque deben transformarse en
        // PlayerListener
        if (!(entity instanceof org.bukkit.entity.Bat) && !(entity instanceof org.bukkit.entity.Squid)) {
            // Marcar como procesado ANTES de aplicar efectos del día 25
            entity.getPersistentDataContainer().set(transformedKey, org.bukkit.persistence.PersistentDataType.BYTE,
                    (byte) 1);
        }

        // Aplicar efectos del día 25
        applyDay25Effects(entity);

        // DÍA 30: Creepers eléctricos (solo fuera del End)
        if (entity instanceof org.bukkit.entity.Creeper) {
            org.bukkit.entity.Creeper creeper = (org.bukkit.entity.Creeper) entity;
            // Solo hacer eléctrico si NO está en el End (los del End son Ender Creepers)
            if (creeper.getWorld().getEnvironment() != org.bukkit.World.Environment.THE_END) {
                makeCreperCharged(creeper);
            }
        }

        // DÍA 30: Pillagers invisibles con ballestas de Quick Charge X
        if (entity instanceof org.bukkit.entity.Pillager) {
            applyPillagerEffects((org.bukkit.entity.Pillager) entity);
        }

        // DÍA 30: Pigmans con armadura de diamante
        if (entity instanceof PigZombie) {
            applyPigmanDiamondArmor((PigZombie) entity);
        }

        // DÍA 30: Iron Golems con Velocidad IV
        if (entity instanceof IronGolem) {
            applyIronGolemSpeed((IronGolem) entity);
        }

        // DÍA 30: Endermans con Fuerza II
        if (entity instanceof org.bukkit.entity.Enderman) {
            applyEndermanEffects((org.bukkit.entity.Enderman) entity);
        }

        // DÍA 30: Silverfish con 5 efectos aleatorios
        if (entity instanceof org.bukkit.entity.Silverfish) {
            applySilverfishEffects((org.bukkit.entity.Silverfish) entity);
        }

        // SHULKERS EXPLOSIVOS
        if (entity instanceof org.bukkit.entity.Shulker) {
            applyExplosiveShulkerEffects((org.bukkit.entity.Shulker) entity);
        }

        // ENDER CREEPER - Solo en el End
        if (entity instanceof org.bukkit.entity.Creeper) {
            org.bukkit.entity.Creeper creeper = (org.bukkit.entity.Creeper) entity;
            // Solo aplicar efectos especiales si está en el End
            if (creeper.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
                applyEnderCreeperEffects(creeper);
            }
            // Si no está en el End, solo hacerlo eléctrico (ya se hace en línea 298)
        }

        // ENDER GHAST - Solo en el End (en el Nether siguen siendo Ghast Demoníacos del
        // día 25)
        if (entity instanceof org.bukkit.entity.Ghast) {
            org.bukkit.entity.Ghast ghast = (org.bukkit.entity.Ghast) entity;
            // Solo aplicar efectos de Ender Ghast si está en el End
            if (ghast.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
                applyEnderGhastEffects(ghast);
            }
            // Si está en el Nether, ya tiene efectos de Ghast Demoníaco (día 25)
        }

        // DÍA 30: Zombies montados en caballo zombie (solo si cabe el jinete, para que no se asfixie en cuevas)
        if (entity.getType() == EntityType.ZOMBIE && entity.getVehicle() == null
                && entity.getLocation().add(0, 2, 0).getBlock().isPassable()) {
            ZombieHorse horse = entity.getWorld().spawn(entity.getLocation(), ZombieHorse.class);
            horse.setRemoveWhenFarAway(true); // si no, quedan caballos sueltos cuando el zombie despawnea
            horse.addPassenger(entity);
            setSpecialName(entity, "Zombie Jinete", net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN);
        }

        // DÍA 30: Todos los esqueletos tienen clase aleatoria
        if (entity instanceof org.bukkit.entity.Skeleton ||entity instanceof org.bukkit.entity.WitherSkeleton) {
            // Verificar si ya tiene una clase asignada (para no sobrescribir jinetes de
            // arañas)
            org.bukkit.NamespacedKey classKey = new org.bukkit.NamespacedKey(plugin, "skeleton_class");
            if (!entity.getPersistentDataContainer().has(classKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                // Wither skeletons solo toman clases wither (1 o 5); esqueletos normales cualquiera (1-5)
                boolean isWither = entity instanceof org.bukkit.entity.WitherSkeleton;
                int skeletonClass = isWither ? (random.nextBoolean() ? 1 : 5) : random.nextInt(5) + 1;

                if (!isWither && isWitherClass(skeletonClass)) {
                    // Clase wither en un esqueleto normal: reemplazarlo por un wither skeleton (y re-montarlo)
                    Entity vehicle = entity.getVehicle();
                    LivingEntity replacement = spawnClassSkeleton(entity.getLocation(), skeletonClass);
                    entity.remove();
                    if (vehicle != null) {
                        vehicle.addPassenger(replacement);
                    }
                    return;
                }

                entity.getPersistentDataContainer().set(classKey, org.bukkit.persistence.PersistentDataType.INTEGER,
                        skeletonClass);
                equipSkeletonByClass(entity, skeletonClass);
            }
        }

        // DÍA 30: Piglins se transforman en Hoglins
        if (entity instanceof org.bukkit.entity.Piglin) {
            transformPiglinToHoglin((org.bukkit.entity.Piglin) entity);
            return;
        }

        // DÍA 30: Allays invisibles con TNT que explotan
        if (entity instanceof org.bukkit.entity.Allay) {
            applyExplosiveAllayEffects((org.bukkit.entity.Allay) entity);
        }

        // Aplicar nombres especiales a mobs especiales
        applySpecialMobNames(entity);
    }

    /**
     * DÍA 30: Transformar Piglin a Hoglin
     */
    private void transformPiglinToHoglin(org.bukkit.entity.Piglin piglin) {
        org.bukkit.Location loc = piglin.getLocation();
        org.bukkit.World world = piglin.getWorld();

        // Remover el piglin INMEDIATAMENTE
        piglin.remove();

        // Spawnear hoglin INMEDIATAMENTE (sin delay)
        org.bukkit.entity.Hoglin hoglin = (org.bukkit.entity.Hoglin) world.spawnEntity(
                loc,
                org.bukkit.entity.EntityType.HOGLIN);

        // MARCAR INMEDIATAMENTE como transformado
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "transformed_mob");
        hoglin.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // Evitar que se convierta en Zoglin
        hoglin.setImmuneToZombification(true);

        // Nombre especial (sin nametag visible)
        hoglin.customName(net.kyori.adventure.text.Component.text("Hoglin Salvaje")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_RED)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        hoglin.setCustomNameVisible(false);

        // Marcar también con un segundo marcador para evitar re-procesamiento
        hoglin.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skip_effects"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);
    }

    /**
     * DÍA 30: Allay explosivo - Invisible, Speed I, TNT en mano, explota al morir o
     * al golpear
     */
    private void applyExplosiveAllayEffects(org.bukkit.entity.Allay allay) {
        // Marcar como Allay explosivo
        org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(plugin, "explosive_allay");
        allay.getPersistentDataContainer().set(explosiveKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // Invisibilidad permanente
        allay.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

        // Speed II (amplifier = 1)
        allay.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

        // Hacer más grande (escala 2.0 = tamaño de araña aproximadamente)
        org.bukkit.attribute.AttributeInstance scaleAttr = allay.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(2.0);
        }

        // TNT en la mano
        org.bukkit.inventory.EntityEquipment equipment = allay.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TNT));
            equipment.setItemInMainHandDropChance(0.0f);
        }

        // Nombre especial (sin nametag visible porque es invisible)
        allay.customName(net.kyori.adventure.text.Component.text("Allay Explosivo")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        allay.setCustomNameVisible(false);

        // Hacer agresivo usando el sistema ya existente
        makeAggressive((org.bukkit.entity.Mob) allay, 0.0); // 0 daño porque explota
    }

    /**
     * DÍA 30: Hacer creeper eléctrico (charged)
     */
    private void makeCreperCharged(org.bukkit.entity.Creeper creeper) {
        creeper.setPowered(true);

        // Configurar el radio de explosión de creeper eléctrico
        creeper.setExplosionRadius(6);

        // Nombre especial
        creeper.customName(net.kyori.adventure.text.Component.text("Creeper Eléctrico")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        creeper.setCustomNameVisible(false);
    }

    /**
     * DÍA 30: Aplicar nombres especiales a mobs especiales
     */
    private void applySpecialMobNames(LivingEntity entity) {
        // Arañas con jinete (día 20+)
        if (entity instanceof org.bukkit.entity.Spider) {
            if (!entity.getPassengers().isEmpty()) {
                entity.customName(net.kyori.adventure.text.Component.text("Araña Jinete")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                entity.setCustomNameVisible(false);
            }
        }

        // Ravagers especiales (día 25+)
        if (entity instanceof org.bukkit.entity.Ravager) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "totem_drop_chance");
            if (entity.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                entity.customName(net.kyori.adventure.text.Component.text("Ravager Élite")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_RED)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                entity.setCustomNameVisible(false);
            }
        }

        // Mobs especiales del 20-30 que no tenían nombre (los que solo llevan pociones no cuentan)
        String name = switch (entity.getType()) {
            case PHANTOM -> "Phantom Gigante";
            case PILLAGER -> "Pillager Invisible";
            case ZOMBIFIED_PIGLIN -> "Pigman Blindado";
            default -> null;
        };
        if (name != null) {
            setSpecialName(entity, name, net.kyori.adventure.text.format.NamedTextColor.RED);
        }
    }

    private void applySpiderEffects(Spider spider) {
        int effectCount = random.nextInt(3) + 1;
        List<PotionEffectType> availableEffects = getAvailableEffects();
        for (int i = 0; i < effectCount && !availableEffects.isEmpty(); i++) {
            int index = random.nextInt(availableEffects.size());
            PotionEffectType effectType = availableEffects.remove(index);
            int duration = Integer.MAX_VALUE;
            int amplifier = getAmplifierForEffect(effectType);
            spider.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
        }
    }

    private List<PotionEffectType> getAvailableEffects() {
        List<PotionEffectType> effects = new ArrayList<>();
        effects.add(PotionEffectType.SPEED);
        effects.add(PotionEffectType.STRENGTH);
        effects.add(PotionEffectType.JUMP_BOOST);
        effects.add(PotionEffectType.GLOWING);
        effects.add(PotionEffectType.REGENERATION);
        effects.add(PotionEffectType.INVISIBILITY);
        effects.add(PotionEffectType.SLOW_FALLING);
        effects.add(PotionEffectType.RESISTANCE);
        return effects;
    }

    private int getAmplifierForEffect(PotionEffectType type) {
        if (type.equals(PotionEffectType.SPEED))
            return 2;
        if (type.equals(PotionEffectType.STRENGTH))
            return 3;
        if (type.equals(PotionEffectType.JUMP_BOOST))
            return 4;
        if (type.equals(PotionEffectType.REGENERATION))
            return 3;
        if (type.equals(PotionEffectType.RESISTANCE))
            return 2;
        return 0;
    }

    private void applyCreakingEffects(LivingEntity creaking) {
        int duration = Integer.MAX_VALUE;
        creaking.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
        creaking.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0));
    }

    // ========== MECÁNICAS DÍA 20 ==========

    private void applyDay20SpiderEffects(Spider spider) {
        // Limpiar jinetes naturales
        spider.getPassengers().forEach(Entity::remove);

        int effectCount = random.nextInt(3) + 3;
        List<PotionEffectType> availableEffects = getAvailableEffects();
        for (int i = 0; i < effectCount && !availableEffects.isEmpty(); i++) {
            int index = random.nextInt(availableEffects.size());
            PotionEffectType effectType = availableEffects.remove(index);
            int duration = Integer.MAX_VALUE;
            int amplifier = getAmplifierForEffect(effectType);
            spider.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
        }
        spawnSkeletonRider(spider);
    }

    private void spawnSkeletonRider(Spider spider) {
        spider.addPassenger(spawnClassSkeleton(spider.getLocation(), random.nextInt(5) + 1));
    }

    /** Clases 1 (cota de malla) y 5 (cuero rojo) son wither skeleton; 2, 3 y 4 esqueleto normal. */
    private static boolean isWitherClass(int skeletonClass) {
        return skeletonClass == 1 || skeletonClass == 5;
    }

    private LivingEntity spawnClassSkeleton(org.bukkit.Location loc, int skeletonClass) {
        LivingEntity skeleton = (LivingEntity) loc.getWorld().spawnEntity(loc,
                isWitherClass(skeletonClass) ? org.bukkit.entity.EntityType.WITHER_SKELETON
                        : org.bukkit.entity.EntityType.SKELETON);
        // Marcar que ya tiene clase asignada
        org.bukkit.NamespacedKey classKey = new org.bukkit.NamespacedKey(plugin, "skeleton_class");
        skeleton.getPersistentDataContainer().set(classKey, org.bukkit.persistence.PersistentDataType.INTEGER,
                skeletonClass);
        equipSkeletonByClass(skeleton, skeletonClass);
        return skeleton;
    }

    private void equipSkeletonByClass(LivingEntity skeletonEntity, int skeletonClass) {
        org.bukkit.inventory.EntityEquipment equipment = skeletonEntity.getEquipment();
        if (equipment == null)
            return;

        setSpecialName(skeletonEntity, new String[] { "Esqueleto Táctico", "Esqueleto Infernal", "Esqueleto Guerrero",
                "Esqueleto Asesino", "Esqueleto Pesadilla" }[skeletonClass - 1],
                net.kyori.adventure.text.format.NamedTextColor.GRAY);

        // Del 20 al 29 version facil; desde el 30 se vuelven mas dificiles (imagenes de la lista)
        boolean hard = currentDay >= 30;
        double health = 40.0;

        if (skeletonClass == 1) {
            // ESQUELETO TÁCTICO - Wither - Cota Malla - Punch XX (dia 30: Punch XXX + Power XXV) - 20❤
            org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PUNCH, hard ? 30 : 20);
            if (hard)
                bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 25);
            equipment.setItemInMainHand(bow);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_BOOTS));
        } else if (skeletonClass == 2) {
            // ESQUELETO INFERNAL - Normal - Hierro - Hacha de hierro Fire Aspect II, 10❤ (dia 30: diamante FA X, 20❤)
            org.bukkit.inventory.ItemStack axe = new org.bukkit.inventory.ItemStack(
                    hard ? org.bukkit.Material.DIAMOND_AXE : org.bukkit.Material.IRON_AXE);
            axe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, hard ? 10 : 2);
            equipment.setItemInMainHand(axe);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS));
            if (!hard)
                health = 20.0;
        } else if (skeletonClass == 3) {
            // ESQUELETO GUERRERO - Normal - Diamante - Arco - 10❤ (dia 30: Proteccion IV, 20❤)
            equipment.setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));

            org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_HELMET);
            org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_CHESTPLATE);
            org.bukkit.inventory.ItemStack leggings = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_LEGGINGS);
            org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_BOOTS);

            if (hard) {
                helmet.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
                chestplate.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
                leggings.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
                boots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
            } else {
                health = 20.0;
            }

            equipment.setHelmet(helmet);
            equipment.setChestplate(chestplate);
            equipment.setLeggings(leggings);
            equipment.setBoots(boots);

        } else if (skeletonClass == 4) {
            // ESQUELETO ASESINO - Normal - Oro - Ballesta Sharpness XX (dia 30: XXV + Velocidad II) - 20❤
            org.bukkit.inventory.ItemStack crossbow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CROSSBOW);
            crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, hard ? 25 : 20);
            equipment.setItemInMainHand(crossbow);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_BOOTS));

            if (hard)
                skeletonEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

        } else if (skeletonClass == 5) {
            // ESQUELETO PESADILLA - Wither - Cuero Rojo - Power X (dia 30: Power L) - 20❤
            org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, hard ? 50 : 10);
            equipment.setItemInMainHand(bow);

            org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.LEATHER_HELMET);
            org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.LEATHER_CHESTPLATE);
            org.bukkit.inventory.ItemStack leggings = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.LEATHER_LEGGINGS);
            org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.LEATHER_BOOTS);

            org.bukkit.inventory.meta.LeatherArmorMeta helmetMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet
                    .getItemMeta();
            org.bukkit.inventory.meta.LeatherArmorMeta chestplateMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate
                    .getItemMeta();
            org.bukkit.inventory.meta.LeatherArmorMeta leggingsMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) leggings
                    .getItemMeta();
            org.bukkit.inventory.meta.LeatherArmorMeta bootsMeta = (org.bukkit.inventory.meta.LeatherArmorMeta) boots
                    .getItemMeta();

            org.bukkit.Color red = org.bukkit.Color.RED;
            helmetMeta.setColor(red);
            chestplateMeta.setColor(red);
            leggingsMeta.setColor(red);
            bootsMeta.setColor(red);

            helmet.setItemMeta(helmetMeta);
            chestplate.setItemMeta(chestplateMeta);
            leggings.setItemMeta(leggingsMeta);
            boots.setItemMeta(bootsMeta);

            equipment.setHelmet(helmet);
            equipment.setChestplate(chestplate);
            equipment.setLeggings(leggings);
            equipment.setBoots(boots);

        }

        skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(health);
        skeletonEntity.setHealth(health);

        // DÍA 30: Flechas de Daño II en mano izquierda para todos los skeleton riders
        if (currentDay >= 30) {
            org.bukkit.inventory.ItemStack arrow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.TIPPED_ARROW);
            org.bukkit.inventory.meta.PotionMeta arrowMeta = (org.bukkit.inventory.meta.PotionMeta) arrow.getItemMeta();
            if (arrowMeta != null) {
                arrowMeta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);
                arrow.setItemMeta(arrowMeta);
            }
            equipment.setItemInOffHand(arrow);
            equipment.setItemInOffHandDropChance(0.0f);
        }

        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
        equipment.setItemInMainHandDropChance(0.0f);
    }

    private void applyPhantomEffects(Phantom phantom) {
        phantom.setSize(9);
        org.bukkit.attribute.AttributeInstance maxHealth = phantom
                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double currentMax = maxHealth.getBaseValue();
            maxHealth.setBaseValue(currentMax * 2);
            phantom.setHealth(currentMax * 2);
        }
    }

    private static boolean isTamed(Mob mob) {
        return mob instanceof org.bukkit.entity.Tameable t && t.isTamed();
    }

    private void makeAggressive(Mob mob, double damage) {
        AggressiveMobGoal goal = new AggressiveMobGoal(mob, plugin, 20.0, damage);
        if (org.bukkit.Bukkit.getMobGoals().hasGoal(mob, goal.getKey()))
            return;

        org.bukkit.attribute.AttributeInstance damageAttr = mob
                .getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damage);
        }

        // Prioridad 0: mientras persigue le gana a huir/panico; sin jugador cerca conserva su IA normal (nadar,
        // pasear)
        org.bukkit.Bukkit.getMobGoals().addGoal(mob, 0, goal);
    }

    // Clase interna UNIFICADA para IA de ataque en cualquier Mob
    private static class AggressiveMobGoal implements com.destroystokyo.paper.entity.ai.Goal<Mob> {
        private final Mob entity;
        private final com.destroystokyo.paper.entity.ai.GoalKey<Mob> key;
        private static final double CHASE_SPEED = 1.3;
        private int attackCooldown = 0;
        private int pathTicks = 10;
        // Victima propia: ajolotes (y otros mobs con brain) leen getTarget() de su brain e ignoran setTarget()
        private LivingEntity victim;
        private final double range;
        private final double damage;

        public AggressiveMobGoal(Mob entity, HM.LuckyDemon.HMPlugin plugin, double range, double damage) {
            this.entity = entity;
            this.range = range;
            this.damage = damage;
            this.key = com.destroystokyo.paper.entity.ai.GoalKey.of(Mob.class,
                    new org.bukkit.NamespacedKey(plugin, "aggressive_mob"));
        }

        @Override
        public boolean shouldActivate() {
            if (isTamed(entity))
                return false;

            LivingEntity target = victim;

            // Si ya tiene un target válido, mantenerlo
            if (target != null && !target.isDead()) {
                if (target instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player p = (org.bukkit.entity.Player) target;
                    if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL
                            || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                        return true;
                    }
                }
            }

            // Buscar jugador cercano
            org.bukkit.entity.Player nearestPlayer = null;
            double nearestDistance = Double.MAX_VALUE;

            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL
                        && player.getGameMode() != org.bukkit.GameMode.ADVENTURE)
                    continue;

                if (player.getWorld() != entity.getWorld())
                    continue;

                double distance = player.getLocation().distance(entity.getLocation());
                if (distance < nearestDistance && distance < range) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }

            if (nearestPlayer != null) {
                victim = nearestPlayer;
                entity.setTarget(nearestPlayer);
                return true;
            }

            return false;
        }

        @Override
        public boolean shouldStayActive() {
            LivingEntity target = victim;

            if (target == null || target.isDead() || isTamed(entity)) {
                return false;
            }

            if (target.getWorld() != entity.getWorld()) {
                return false;
            }

            double distance = entity.getLocation().distance(target.getLocation());
            if (distance > range + 5.0) { // Margen extra para no perderlo inmediatamente
                return false;
            }

            if (target instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player p = (org.bukkit.entity.Player) target;
                if (p.getGameMode() != org.bukkit.GameMode.SURVIVAL
                        && p.getGameMode() != org.bukkit.GameMode.ADVENTURE) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void start() {
            attackCooldown = 0;
            pathTicks = 10;
        }

        @Override
        public void stop() {
            entity.getPathfinder().stopPathfinding();
            entity.setTarget(null);
            victim = null;
        }

        @Override
        public void tick() {
            if (attackCooldown > 0) {
                attackCooldown--;
            }

            LivingEntity target = victim;
            if (target == null)
                return;

            entity.lookAt(target);

            double distSq = entity.getLocation().distanceSquared(target.getLocation());

            // Recalcular el camino cada 10 ticks, no cada tick
            if (distSq > 1.0 && ++pathTicks >= 10) {
                pathTicks = 0;
                entity.getPathfinder().moveTo(target, CHASE_SPEED);
            }

            // Atacar si está cerca (3.0 bloques cuadrados = ~1.7 bloques) y lo ve (no a través de paredes)
            if (distSq < 3.0 && attackCooldown == 0 && entity.hasLineOfSight(target)) {
                double finalDamage = damage;
                // Usar el daño del atributo si es mayor (por si el mob tiene fuerza)
                org.bukkit.attribute.AttributeInstance attr = entity
                        .getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                if (attr != null && attr.getValue() > finalDamage) {
                    finalDamage = attr.getValue();
                }

                target.damage(finalDamage, entity);
                entity.swingMainHand();

                attackCooldown = 20; // 1 segundo de cooldown entre golpes
            }
        }

        @Override
        public com.destroystokyo.paper.entity.ai.GoalKey<Mob> getKey() {
            return key;
        }

        @Override
        public java.util.EnumSet<com.destroystokyo.paper.entity.ai.GoalType> getTypes() {
            return java.util.EnumSet.of(com.destroystokyo.paper.entity.ai.GoalType.MOVE,
                    com.destroystokyo.paper.entity.ai.GoalType.LOOK);
        }
    }

    private void setupRavagerLoot(Ravager ravager) {
        // Ravager especial sin nametag (1% chance de dropear Tótem)
    }

    // ========== MECÁNICAS DÍA 25 ==========

    /**
     * DÍA 25: Creaking mejorado con Fuerza II y Velocidad III
     */
    private void applyDay25CreakingEffects(LivingEntity creaking) {
        int duration = Integer.MAX_VALUE;
        // Strength II (amplifier = 1) y Speed III (amplifier = 2)
        creaking.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1));
        creaking.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2));
    }

    private void applyDay25SpiderEffects(Spider spider) {
        // Limpiar jinetes naturales
        spider.getPassengers().forEach(Entity::remove);

        // 5 efectos aleatorios (en lugar de 3)
        int effectCount = 5;
        List<PotionEffectType> availableEffects = getAvailableEffects();

        for (int i = 0; i < effectCount && !availableEffects.isEmpty(); i++) {
            int index = random.nextInt(availableEffects.size());
            PotionEffectType effectType = availableEffects.remove(index);
            int duration = Integer.MAX_VALUE;
            int amplifier = getAmplifierForEffect(effectType);
            spider.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
        }

        // Arañas de cueva sin jinete, salvo desde el día 40 (todas las arañas pasan a ser de cueva)
        if (!(spider instanceof org.bukkit.entity.CaveSpider) || currentDay >= 40) {
            spawnSkeletonRider(spider);
        }
    }

    private void applyDay25RavagerEffects(Ravager ravager) {
        // Strength II (amplifier 1) y Speed I (amplifier 0)
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));

        // Marcar con 1% de drop de tótem
        ravager.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "totem_drop_chance"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                1);
    }

    private void applyGigaSlimeEffects(Slime slime) {
        // Marcar como Giga Slime original
        slime.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "giga_slime"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);

        // Nombre personalizado
        slime.customName(net.kyori.adventure.text.Component.text("Giga Slime")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        slime.setCustomNameVisible(false);

        // Tamaño 15
        slime.setSize(15);

        // Doble de vida
        org.bukkit.attribute.AttributeInstance maxHealth = slime
                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double currentMax = maxHealth.getBaseValue();
            maxHealth.setBaseValue(currentMax * 2);
            slime.setHealth(currentMax * 2);
        }
    }

    private void applyGigaMagmaCubeEffects(MagmaCube magmaCube) {
        // Marcar como Giga Magma Cube original
        magmaCube.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "giga_magma"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);

        // Nombre personalizado
        magmaCube.customName(net.kyori.adventure.text.Component.text("Giga Magma Cube")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        magmaCube.setCustomNameVisible(false);

        // Tamaño 16
        magmaCube.setSize(16);
    }

    private void applyDemonicGhastEffects(Ghast ghast) {
        // Vida entre 40 y 60
        int health = 40 + random.nextInt(21); // 40-60
        org.bukkit.attribute.AttributeInstance maxHealth = ghast
                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            ghast.setHealth(health);
        }

        // Nombre personalizado
        ghast.customName(net.kyori.adventure.text.Component.text("Ghast Demoníaco")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        ghast.setCustomNameVisible(false);

        // Marcar que es un Ghast Demoníaco para las bolas de fuego
        ghast.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "demonic_ghast"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);

        // Asignar ExplosionPower aleatorio (3, 4 o 5)
        int explosionPower = 3 + new java.util.Random().nextInt(3); // 3, 4 o 5
        org.bukkit.NamespacedKey explosionKey = new org.bukkit.NamespacedKey(plugin, "explosion_power");
        ghast.getPersistentDataContainer().set(explosionKey, org.bukkit.persistence.PersistentDataType.INTEGER,
                explosionPower);
    }

    // ========== REGLAS SEGÚN DÍA ==========

    public double getMobSpawnMultiplier() {
        if (difficultyLevel >= 1) {
            return 2.0;
        }
        return 1.0;
    }

    public int getMinimumPlayersRequired() {
        if (difficultyLevel >= 1) {
            return 4;
        }
        return 1;
    }

    public boolean canSkipNight() {
        return currentDay < 20;
    }

    public boolean shouldDropItems(Entity entity) {
        if (currentDay < 20) {
            return true;
        }
        return !(entity instanceof IronGolem ||
                entity instanceof PigZombie ||
                entity instanceof Ghast ||
                entity instanceof Guardian ||
                entity instanceof MagmaCube ||
                entity instanceof Enderman ||
                entity instanceof Witch ||
                entity instanceof WitherSkeleton ||
                entity instanceof Evoker ||
                entity instanceof Phantom ||
                entity instanceof Slime ||
                entity instanceof Drowned ||
                entity instanceof Blaze);
    }

    public int getSpecialRulesDay() {
        return 20;
    }

    public boolean isDay25OrLater() {
        return currentDay >= 25;
    }

    public boolean isDay30OrLater() {
        return currentDay >= 30;
    }

    public boolean isDay40OrLater() {
        return currentDay >= 40;
    }

    /**
     * Calcula las horas de Death Train según el sistema de reset cada 25 días
     * Día 25 = 1hr, Día 26 = 2hr, ..., Día 49 = 24hr, Día 50 = 1hr, etc.
     */
    public int getStormHoursForCurrentDay() {
        // Si es día 0, no hay tormenta
        if (currentDay == 0) {
            return 0;
        }

        // Reset cada 25 días: día 1-24 = 1-24hr, día 25 = 1hr, día 26 = 2hr, etc.
        int effectiveDay = ((currentDay - 1) % 25) + 1;
        return effectiveDay;
    }

    /**
     * DÍA 30: Pillagers invisibles con ballesta Quick Charge X
     */
    private void applyPillagerEffects(org.bukkit.entity.Pillager pillager) {
        // Invisibilidad permanente
        pillager.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

        // Ballesta con Quick Charge X
        org.bukkit.inventory.EntityEquipment equipment = pillager.getEquipment();
        if (equipment != null) {
            org.bukkit.inventory.ItemStack crossbow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CROSSBOW);
            crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.QUICK_CHARGE, 10);
            equipment.setItemInMainHand(crossbow);
            equipment.setItemInMainHandDropChance(0.0f);
        }
    }

    /**
     * DÍA 30: Pigmans con armadura de diamante
     */
    private void applyPigmanDiamondArmor(PigZombie pigman) {
        org.bukkit.inventory.EntityEquipment equipment = pigman.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BOOTS));

            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
        }
    }

    /**
     * DÍA 30: Iron Golems con Velocidad IV
     */
    private void applyIronGolemSpeed(IronGolem golem) {
        golem.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
    }

    /**
     * DÍA 30: Endermans con Fuerza II
     */
    private void applyEndermanEffects(org.bukkit.entity.Enderman enderman) {
        enderman.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
    }

    /**
     * DÍA 30: Silverfish con 5 efectos aleatorios (igual que Day 25 Spiders)
     */
    private void applySilverfishEffects(org.bukkit.entity.Silverfish silverfish) {
        int effectCount = 5;
        List<PotionEffectType> availableEffects = getAvailableEffects();

        for (int i = 0; i < effectCount && !availableEffects.isEmpty(); i++) {
            int index = random.nextInt(availableEffects.size());
            PotionEffectType effectType = availableEffects.remove(index);
            int duration = Integer.MAX_VALUE;
            int amplifier = getAmplifierForEffect(effectType);
            silverfish.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
        }
    }

    /**
     * SHULKERS EXPLOSIVOS - 20% drop Shulker Shell
     */
    private void applyExplosiveShulkerEffects(org.bukkit.entity.Shulker shulker) {
        // Marcar como explosivo
        org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(plugin, "explosive_shulker");
        shulker.getPersistentDataContainer().set(explosiveKey, org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);

        // Nombre especial
        shulker.customName(net.kyori.adventure.text.Component.text("Shulker Explosivo")
                .color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        shulker.setCustomNameVisible(false);
    }

    /**
     * ENDER GHAST - Spawn en el End, explota al morir
     */
    private void applyEnderGhastEffects(org.bukkit.entity.Ghast ghast) {
        // Marcar como Ender Ghast
        org.bukkit.NamespacedKey enderGhastKey = new org.bukkit.NamespacedKey(plugin, "ender_ghast");
        ghast.getPersistentDataContainer().set(enderGhastKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // Nombre especial con nametag visible
        ghast.customName(net.kyori.adventure.text.Component.text("Ender Ghast")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        ghast.setCustomNameVisible(false);

        // Asignar ExplosionPower aleatorio (3, 4 o 5)
        int explosionPower = 3 + new java.util.Random().nextInt(3); // 3, 4 o 5
        org.bukkit.NamespacedKey explosionKey = new org.bukkit.NamespacedKey(plugin, "explosion_power");
        ghast.getPersistentDataContainer().set(explosionKey, org.bukkit.persistence.PersistentDataType.INTEGER,
                explosionPower);
    }

    /**
     * ENDER CREEPER - Eléctrico e Invisible, se teletransporta al recibir flechas
     */
    private void applyEnderCreeperEffects(org.bukkit.entity.Creeper creeper) {
        // Marcar como Ender Creeper
        org.bukkit.NamespacedKey enderCreeperKey = new org.bukkit.NamespacedKey(plugin, "ender_creeper");
        creeper.getPersistentDataContainer().set(enderCreeperKey, org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);

        // Hacer eléctrico
        creeper.setPowered(true);

        // Hacer invisible
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

        // Nombre especial con nametag visible
        creeper.customName(net.kyori.adventure.text.Component.text("Ender Creeper")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        creeper.setCustomNameVisible(false);
    }
}
