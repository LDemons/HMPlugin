package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
                            if (mob instanceof Animals || mob instanceof AbstractVillager || mob instanceof Fish
                                    || mob instanceof Axolotl || mob instanceof Squid || mob instanceof Dolphin
                                    || mob instanceof org.bukkit.entity.Bat) {

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
        if (currentDay >= 20) {
            if (entity instanceof Spider) {
                applyDay20SpiderEffects((Spider) entity);
                return;
            }
            if (entity instanceof Phantom) {
                applyPhantomEffects((Phantom) entity);
            }
            // Ghasts: Convertir Happy Ghasts en Ghasts normales/hostiles
            if (entity instanceof org.bukkit.entity.Ghast) {
                makeGhastNormal((org.bukkit.entity.Ghast) entity);
            }

            // Lógica unificada para hacer agresivos a Mobs pasivos
            // Usamos "Mob" para cubrir Animales, Murciélagos y Aldeanos
            if (entity instanceof Animals || entity instanceof org.bukkit.entity.Bat || entity instanceof Fish
                    || entity instanceof Axolotl || entity instanceof Squid || entity instanceof Dolphin
                    || entity instanceof AbstractVillager) {
                // 2.0 de daño = 1 corazón
                makeAggressive((Mob) entity, 2.0);
            }

            if (entity instanceof PigZombie) {
                makePigmanAngry((PigZombie) entity);
            }
            if (entity instanceof Ravager) {
                setupRavagerLoot((Ravager) entity);
            }
        } else if (difficultyLevel >= 1) {
            if (entity instanceof Spider) {
                applySpiderEffects((Spider) entity);
            }
            if (entity.getType().name().equals("CREAKING")) {
                applyCreakingEffects(entity);
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
        equipSkeletonByClass(skeleton, skeletonClass);
    }

    private void equipSkeletonByClass(LivingEntity skeletonEntity, int skeletonClass) {
        org.bukkit.inventory.EntityEquipment equipment = skeletonEntity.getEquipment();
        if (equipment == null)
            return;

        if (skeletonClass == 1) {
            // Wither - Cota Malla - Punch 20 - 20❤
            org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PUNCH, 20);
            equipment.setItemInMainHand(bow);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_BOOTS));
            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        } else if (skeletonClass == 2) {
            // Normal - Hierro - Hacha Fire Aspect 2 - 10❤
            org.bukkit.inventory.ItemStack axe = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_AXE);
            axe.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
            equipment.setItemInMainHand(axe);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS));
            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
            skeletonEntity.setHealth(20.0);
        } else if (skeletonClass == 3) {
            // Normal - Diamante - Arco - 10❤
            equipment.setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BOOTS));
            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
            skeletonEntity.setHealth(20.0);
        } else if (skeletonClass == 4) {
            // Normal - Oro - Ballesta Sharpness 20 - 20❤
            org.bukkit.inventory.ItemStack crossbow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CROSSBOW);
            crossbow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 20);
            equipment.setItemInMainHand(crossbow);
            equipment.setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_HELMET));
            equipment.setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_CHESTPLATE));
            equipment.setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_LEGGINGS));
            equipment.setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLDEN_BOOTS));
            skeletonEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
            skeletonEntity.setHealth(40.0);
        } else if (skeletonClass == 5) {
            // Wither - Cuero Rojo - Power 10 - 20❤
            org.bukkit.inventory.ItemStack bow = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW);
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 10);
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

    /**
     * Convierte Happy Ghasts en Ghasts normales/hostiles.
     * Remueve cualquier efecto de poción que pueda hacerlos pacíficos
     * y elimina custom names que puedan identificarlos como "Happy".
     */
    private void makeGhastNormal(org.bukkit.entity.Ghast ghast) {
        // Remover todos los efectos de poción (Happy Ghasts pueden tener efectos
        // especiales)
        ghast.getActivePotionEffects().forEach(effect -> ghast.removePotionEffect(effect.getType()));

        // Remover custom name si tiene (Happy Ghasts suelen tener nombres)
        ghast.setCustomName(null);
        ghast.setCustomNameVisible(false);

        // Asegurar que el Ghast sea hostil (no AI)
        // Los Ghasts son naturalmente hostiles, solo necesitamos limpiar modificaciones
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
}
