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
        currentDay = difficultyConfig.getInt("currentDay", 0);
        difficultyLevel = difficultyConfig.getInt("difficultyLevel", 0);
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
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void reset() {
        currentDay = 0;
        difficultyLevel = 0;
        saveDifficultyData();
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

                    // Escanear entidades cercanas (15 bloques)
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(15, 15, 15)) {
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

                                // A) SI NO TIENE LA MARCA, DARLE LA IA AGRESIVA AHORA MISMO
                                if (!mob.getPersistentDataContainer().has(
                                        new org.bukkit.NamespacedKey(plugin, "is_aggressive"),
                                        org.bukkit.persistence.PersistentDataType.BYTE)) {
                                    makeAggressive(mob, 2.0);
                                }

                                // B) Si no tiene objetivo, forzarlo a atacar al jugador
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
        if (currentDay >= 30) {
            applyDay30Effects(entity);
        } else if (currentDay >= 25) {
            applyDay25Effects(entity);
        } else if (currentDay >= 20) {
            applyDay20Effects(entity);
        } else if (difficultyLevel >= 1) {
            applyBaseEffects(entity);
        }
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
        if (entity instanceof Animals || entity instanceof org.bukkit.entity.Bat || entity instanceof Fish
                || entity instanceof Axolotl || entity instanceof Squid || entity instanceof Dolphin
                || entity instanceof AbstractVillager) {
            makeAggressive((Mob) entity, 2.0);
        }
        if (entity instanceof PigZombie) {
            makePigmanAngry((PigZombie) entity);
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
        if (entity instanceof PigZombie) {
            makePigmanAngry((PigZombie) entity);
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

        // DÍA 30: Creepers eléctricos
        if (entity instanceof org.bukkit.entity.Creeper) {
            makeCreperCharged((org.bukkit.entity.Creeper) entity);
        }
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

        // DÍA 30: Todos los esqueletos tienen clase aleatoria
        if (entity instanceof org.bukkit.entity.Skeleton || entity instanceof org.bukkit.entity.WitherSkeleton) {
            // Verificar si ya tiene una clase asignada (para no sobrescribir jinetes de
            // arañas)
            org.bukkit.NamespacedKey classKey = new org.bukkit.NamespacedKey(plugin, "skeleton_class");
            if (!entity.getPersistentDataContainer().has(classKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                // Asignar clase aleatoria (1-5)
                int skeletonClass = random.nextInt(5) + 1;
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
     * DÍA 30: Transformar calamar en guardián con Speed II
     */
    private void transformSquidToGuardian(org.bukkit.entity.Squid squid) {
        org.bukkit.Location loc = squid.getLocation();
        org.bukkit.World world = squid.getWorld();

        // Remover el calamar INMEDIATAMENTE
        squid.remove();

        // Spawnear guardián INMEDIATAMENTE (sin delay)
        org.bukkit.entity.Guardian guardian = (org.bukkit.entity.Guardian) world.spawnEntity(
                loc,
                org.bukkit.entity.EntityType.GUARDIAN);

        // MARCAR INMEDIATAMENTE como transformado (ANTES de que se procese)
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "transformed_mob");
        guardian.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // Aplicar Speed II
        guardian.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

        // Nombre especial (sin nametag visible)
        guardian.customName(net.kyori.adventure.text.Component.text("Guardián Acuático")
                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        guardian.setCustomNameVisible(false);

        // Marcar también con un segundo marcador para evitar re-procesamiento
        guardian.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skip_effects"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);
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
     * DÍA 30: Transformar murciélago en blaze con Resistencia II
     */
    private void transformBatToBlaze(org.bukkit.entity.Bat bat) {
        org.bukkit.Location loc = bat.getLocation();
        org.bukkit.World world = bat.getWorld();

        // Remover el murciélago INMEDIATAMENTE
        bat.remove();

        // Spawnear blaze INMEDIATAMENTE (sin delay)
        org.bukkit.entity.Blaze blaze = (org.bukkit.entity.Blaze) world.spawnEntity(
                loc,
                org.bukkit.entity.EntityType.BLAZE);

        // MARCAR INMEDIATAMENTE como transformado
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "transformed_mob");
        blaze.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // Aplicar Resistencia II
        blaze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));

        // Nombre especial (sin nametag visible)
        blaze.customName(net.kyori.adventure.text.Component.text("Blaze Infernal")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        blaze.setCustomNameVisible(false);

        // Marcar también con un segundo marcador para evitar re-procesamiento
        blaze.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skip_effects"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1);
    }

    /**
     * DÍA 30: Hacer creeper eléctrico (charged)
     */
    private void makeCreperCharged(org.bukkit.entity.Creeper creeper) {
        creeper.setPowered(true);

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
        int skeletonClass = random.nextInt(5) + 1;
        org.bukkit.World world = spider.getWorld();

        LivingEntity skeleton;
        if (skeletonClass == 1 || skeletonClass == 5) {
            skeleton = (org.bukkit.entity.WitherSkeleton) world.spawnEntity(
                    spider.getLocation(),
                    org.bukkit.entity.EntityType.WITHER_SKELETON);
        } else {
            skeleton = (org.bukkit.entity.Skeleton) world.spawnEntity(
                    spider.getLocation(),
                    org.bukkit.entity.EntityType.SKELETON);
        }

        spider.addPassenger(skeleton);
        // Marcar que ya tiene clase asignada
        org.bukkit.NamespacedKey classKey = new org.bukkit.NamespacedKey(plugin, "skeleton_class");
        skeleton.getPersistentDataContainer().set(classKey, org.bukkit.persistence.PersistentDataType.INTEGER,
                skeletonClass);
        equipSkeletonByClass(skeleton, skeletonClass);
    }

    private void equipSkeletonByClass(LivingEntity skeletonEntity, int skeletonClass) {
        org.bukkit.inventory.EntityEquipment equipment = skeletonEntity.getEquipment();
        if (equipment == null)
            return;

        if (skeletonClass == 1) {
            // ESQUELETO TÁCTICO - Wither - Cota Malla - Punch 30 + Power 25 - 20❤
            org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PUNCH, 30);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 25);
            equipment.setItemInMainHand(bow);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_BOOTS));
            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        } else if (skeletonClass == 2) {
            // ESQUELETO INFERNAL - Normal - Hierro - Hacha de Diamante Fire Aspect X - 20❤
            org.bukkit.inventory.ItemStack axe = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_AXE);
            axe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 10);
            equipment.setItemInMainHand(axe);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS));
            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        } else if (skeletonClass == 3) {
            // ESQUELETO GUERRERO - Normal - Diamante Full Protección 4 - Arco - 20❤
            equipment.setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));

            org.bukkit.inventory.ItemStack helmet = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_HELMET);
            org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_CHESTPLATE);
            org.bukkit.inventory.ItemStack leggings = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_LEGGINGS);
            org.bukkit.inventory.ItemStack boots = new org.bukkit.inventory.ItemStack(
                    org.bukkit.Material.DIAMOND_BOOTS);

            helmet.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
            chestplate.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
            leggings.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
            boots.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);

            equipment.setHelmet(helmet);
            equipment.setChestplate(chestplate);
            equipment.setLeggings(leggings);
            equipment.setBoots(boots);

            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        } else if (skeletonClass == 4) {
            // ESQUELETO ASESINO - Normal - Oro - Ballesta Sharpness 25 + Velocidad 2 - 20❤
            org.bukkit.inventory.ItemStack crossbow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CROSSBOW);
            crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 25);
            equipment.setItemInMainHand(crossbow);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_BOOTS));

            // Aplicar Velocidad 2
            skeletonEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        } else if (skeletonClass == 5) {
            // ESQUELETO PESADILLA - Wither - Cuero Rojo - Power L (50) - 20❤
            org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 50);
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

            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        }

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

    private void makeAggressive(Mob mob, double damage) {
        // 1. Marcar al mob como "Agresivo" para no repetir esto
        mob.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "is_aggressive"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        // 2. Configurar daño físico
        try {
            org.bukkit.attribute.AttributeInstance damageAttr = mob
                    .getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE"));
            if (damageAttr != null) {
                damageAttr.setBaseValue(damage);
            }
        } catch (IllegalArgumentException ignored) {
        }

        // 3. Borrar la IA original (Pánico, Huir)
        if (mob instanceof Animals || mob instanceof AbstractVillager || mob instanceof Fish || mob instanceof Dolphin
                || mob instanceof Axolotl || mob instanceof Squid) {
            org.bukkit.Bukkit.getMobGoals().removeAllGoals(mob);
        }

        // 4. Inyectar IA asesina (Prioridad 0)
        org.bukkit.Bukkit.getMobGoals().addGoal(mob, 0, new AggressiveMobGoal(mob, plugin, 20.0, damage));
    }

    // Clase interna UNIFICADA para IA de ataque en cualquier Mob
    private static class AggressiveMobGoal implements com.destroystokyo.paper.entity.ai.Goal<Mob> {
        private final Mob entity;
        private final com.destroystokyo.paper.entity.ai.GoalKey<Mob> key;
        private int attackCooldown = 0;
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
            LivingEntity target = entity.getTarget();

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
                entity.setTarget(nearestPlayer);
                return true;
            }

            return false;
        }

        @Override
        public boolean shouldStayActive() {
            LivingEntity target = entity.getTarget();

            if (target == null || target.isDead()) {
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
        }

        @Override
        public void stop() {
            entity.getPathfinder().stopPathfinding();
            entity.setTarget(null);
        }

        @Override
        public void tick() {
            if (attackCooldown > 0) {
                attackCooldown--;
            }

            LivingEntity target = entity.getTarget();
            if (target == null)
                return;

            entity.lookAt(target);

            double distSq = entity.getLocation().distanceSquared(target.getLocation());

            if (distSq > 1.0) {
                entity.getPathfinder().moveTo(target);
            }

            // Atacar si está cerca (3.0 bloques cuadrados = ~1.7 bloques de distancia)
            if (distSq < 3.0 && attackCooldown == 0) {
                double finalDamage = damage;
                // Intentar obtener daño del atributo si es mayor (por si el mob tiene fuerza)
                try {
                    org.bukkit.attribute.AttributeInstance attr = entity
                            .getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE"));
                    if (attr != null && attr.getValue() > finalDamage) {
                        finalDamage = attr.getValue();
                    }
                } catch (Exception ignored) {
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

    private void makePigmanAngry(PigZombie pigman) {
        org.bukkit.entity.Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            double distance = player.getLocation().distance(pigman.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        if (nearestPlayer != null) {
            pigman.setAngry(true);
            pigman.setAnger(Integer.MAX_VALUE);
            pigman.setTarget(nearestPlayer);
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

        // Solo spawnear skeleton si NO es una araña de cueva
        if (!(spider instanceof org.bukkit.entity.CaveSpider)) {
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
