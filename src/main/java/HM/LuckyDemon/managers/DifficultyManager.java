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
            // TODOS los animales pasivos son agresivos (sin excluir Tameables)
            if (entity instanceof Animals) {
                makeAnimalAggressive((Animals) entity);
            }
            // Bats también son agresivos (no son Animals, son Ambient)
            if (entity instanceof org.bukkit.entity.Bat) {
                makeBatAggressive((org.bukkit.entity.Bat) entity);
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

    @SuppressWarnings("deprecation")
    private void makeAnimalAggressive(Animals animal) {
        // Intentar establecer atributo de daño si existe
        try {
            org.bukkit.attribute.AttributeInstance damageAttr = animal
                    .getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE"));
            if (damageAttr != null) {
                damageAttr.setBaseValue(2.0); // 1 Corazón de daño base
            }
        } catch (IllegalArgumentException ignored) {
        }

        // Inyectar IA de ataque usando Paper API
        org.bukkit.Bukkit.getMobGoals().addGoal(animal, 1, new AggressiveAnimalGoal(animal, plugin));
    }

    @SuppressWarnings("deprecation")
    private void makeBatAggressive(org.bukkit.entity.Bat bat) {
        // Los Bats no tienen atributo de daño por defecto, pero sí pueden atacar
        // La IA se encargará de aplicar el daño

        // Inyectar IA de ataque usando Paper API
        org.bukkit.Bukkit.getMobGoals().addGoal(bat, 1, new AggressiveBatGoal(bat, plugin));
    }

    // Clase interna para la IA de ataque
    private static class AggressiveAnimalGoal implements com.destroystokyo.paper.entity.ai.Goal<Animals> {
        private final Animals entity;
        private final com.destroystokyo.paper.entity.ai.GoalKey<Animals> key;
        private int attackCooldown = 0;

        public AggressiveAnimalGoal(Animals entity, HM.LuckyDemon.HMPlugin plugin) {
            this.entity = entity;
            this.key = com.destroystokyo.paper.entity.ai.GoalKey.of(Animals.class,
                    new org.bukkit.NamespacedKey(plugin, "aggressive_animal"));
        }

        @Override
        public boolean shouldActivate() {
            org.bukkit.entity.LivingEntity target = entity.getTarget();

            // Si tiene target, verificar si sigue siendo válido
            if (target != null && !target.isDead()) {
                if (target instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player p = (org.bukkit.entity.Player) target;
                    if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL
                            || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                        return true;
                    }
                }
            }

            // Si no tiene target válido, buscar uno nuevo cercano (5 bloques)
            org.bukkit.entity.Player nearestPlayer = null;
            double nearestDistance = Double.MAX_VALUE;
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL
                        && player.getGameMode() != org.bukkit.GameMode.ADVENTURE)
                    continue;

                if (player.getWorld() != entity.getWorld())
                    continue;

                double distance = player.getLocation().distance(entity.getLocation());
                if (distance < nearestDistance && distance < 5.0) { // Rango de visión 5 bloques
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
            org.bukkit.entity.LivingEntity target = entity.getTarget();

            // Si no tiene target, desactivar
            if (target == null || target.isDead()) {
                return false;
            }

            // Si el target está muy lejos (más de 8 bloques), dejar de perseguir
            double distance = entity.getLocation().distance(target.getLocation());
            if (distance > 8.0) {
                return false;
            }

            // Si el target cambió de gamemode, desactivar
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
            entity.setTarget(null); // Olvidar target inválido
        }

        @Override
        @SuppressWarnings("deprecation")
        public void tick() {
            if (attackCooldown > 0) {
                attackCooldown--;
            }

            org.bukkit.entity.LivingEntity target = entity.getTarget();
            if (target == null)
                return;

            entity.lookAt(target);

            double distSq = entity.getLocation().distanceSquared(target.getLocation());

            // Moverse hacia el objetivo si está a más de 1 bloque (distSq > 1.0)
            if (distSq > 1.0) {
                entity.getPathfinder().moveTo(target);
            }

            // Atacar si está cerca (Reducido a 3.0 = ~1.7 bloques para asegurar hit
            // cercano)
            // Y si el cooldown lo permite
            if (distSq < 3.0 && attackCooldown == 0) {
                // Usar damage() en lugar de attack() para evitar glitches
                double damage = 2.0; // 1 Corazón fijo
                try {
                    org.bukkit.attribute.AttributeInstance attr = entity
                            .getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE"));
                    if (attr != null) {
                        damage = attr.getValue();
                    }
                } catch (Exception ignored) {
                }

                target.damage(damage, entity);
                entity.swingMainHand();

                // Cooldown de 1 segundo (20 ticks)
                attackCooldown = 20;
            }
        }

        @Override
        public com.destroystokyo.paper.entity.ai.GoalKey<Animals> getKey() {
            return key;
        }

        @Override
        public java.util.EnumSet<com.destroystokyo.paper.entity.ai.GoalType> getTypes() {
            return java.util.EnumSet.of(com.destroystokyo.paper.entity.ai.GoalType.MOVE,
                    com.destroystokyo.paper.entity.ai.GoalType.LOOK);
        }
    }

    // Clase interna para la IA de Bats agresivos
    private static class AggressiveBatGoal implements com.destroystokyo.paper.entity.ai.Goal<org.bukkit.entity.Bat> {
        private final org.bukkit.entity.Bat entity;
        private final com.destroystokyo.paper.entity.ai.GoalKey<org.bukkit.entity.Bat> key;
        private int attackCooldown = 0;

        public AggressiveBatGoal(org.bukkit.entity.Bat entity, HM.LuckyDemon.HMPlugin plugin) {
            this.entity = entity;
            this.key = com.destroystokyo.paper.entity.ai.GoalKey.of(org.bukkit.entity.Bat.class,
                    new org.bukkit.NamespacedKey(plugin, "aggressive_bat"));
        }

        @Override
        public boolean shouldActivate() {
            org.bukkit.entity.LivingEntity target = entity.getTarget();

            if (target != null && !target.isDead()) {
                if (target instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player p = (org.bukkit.entity.Player) target;
                    if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL
                            || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                        return true;
                    }
                }
            }

            // Buscar jugador cercano (5 bloques)
            org.bukkit.entity.Player nearestPlayer = null;
            double nearestDistance = Double.MAX_VALUE;
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL
                        && player.getGameMode() != org.bukkit.GameMode.ADVENTURE)
                    continue;

                if (player.getWorld() != entity.getWorld())
                    continue;

                double distance = player.getLocation().distance(entity.getLocation());
                if (distance < nearestDistance && distance < 5.0) {
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
            org.bukkit.entity.LivingEntity target = entity.getTarget();

            if (target == null || target.isDead()) {
                return false;
            }

            // Dejar de perseguir si está muy lejos (8 bloques)
            double distance = entity.getLocation().distance(target.getLocation());
            if (distance > 8.0) {
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

            org.bukkit.entity.LivingEntity target = entity.getTarget();
            if (target == null)
                return;

            entity.lookAt(target);

            double distSq = entity.getLocation().distanceSquared(target.getLocation());

            if (distSq > 1.0) {
                entity.getPathfinder().moveTo(target);
            }

            // Atacar si está cerca (3.0 = ~1.7 bloques)
            if (distSq < 3.0 && attackCooldown == 0) {
                // Bats hacen 1 corazón de daño
                target.damage(2.0, entity);

                // Cooldown de 1 segundo (20 ticks)
                attackCooldown = 20;
            }
        }

        @Override
        public com.destroystokyo.paper.entity.ai.GoalKey<org.bukkit.entity.Bat> getKey() {
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
