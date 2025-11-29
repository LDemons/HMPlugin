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

        // Calcular nivel de dificultad basado en días
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

        // Calcular nivel de dificultad basado en días
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

    /**
     * Método centralizado para aplicar efectos a mobs
     * TODOS los cambios de mobs deben pasar por aquí
     */
    public void applyMobEffects(LivingEntity entity) {
        if (difficultyLevel >= 1) { // Día 10+
            if (entity instanceof Spider) {
                applySpiderEffects((Spider) entity);
            }
            // Aquí se pueden agregar más mobs en el futuro
            // if (entity instanceof Zombie) { applyZombieEffects((Zombie) entity); }
        }
    }

    /**
     * Aplicar efectos a arañas según dificultad
     * Día 10+: 1-3 efectos aleatorios
     */
    private void applySpiderEffects(Spider spider) {
        int effectCount = random.nextInt(3) + 1; // 1-3 efectos
        List<PotionEffectType> availableEffects = getAvailableEffects();

        for (int i = 0; i < effectCount && !availableEffects.isEmpty(); i++) {
            int index = random.nextInt(availableEffects.size());
            PotionEffectType effectType = availableEffects.remove(index);

            int duration = Integer.MAX_VALUE; // Efecto permanente
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
            return 2; // Velocidad III
        if (type.equals(PotionEffectType.STRENGTH))
            return 3; // Fuerza IV
        if (type.equals(PotionEffectType.JUMP_BOOST))
            return 4; // Salto V
        if (type.equals(PotionEffectType.REGENERATION))
            return 3; // Regeneración IV
        if (type.equals(PotionEffectType.RESISTANCE))
            return 2; // Resistencia III
        return 0; // Sin amplificador para otros efectos
    }

    // ========== REGLAS SEGÚN DÍA ==========

    public double getMobSpawnMultiplier() {
        if (difficultyLevel >= 1) { // Día 10+
            return 2.0; // Doble de mobs
        }
        return 1.0;
    }

    public int getMinimumPlayersRequired() {
        if (difficultyLevel >= 1) { // Día 10+
            return 4;
        }
        return 1;
    }

    /**
     * Verifica si se puede saltar la noche (dormir en cama)
     * A partir del día 20, el ciclo día/noche es constante
     */
    public boolean canSkipNight() {
        return currentDay < 20;
    }

    /**
     * Verifica si una entidad debe soltar drops
     * A partir del día 20, ciertas entidades no sueltan nada
     */
    public boolean shouldDropItems(Entity entity) {
        if (currentDay < 20) {
            return true; // Antes del día 20, todos sueltan drops normalmente
        }

        // Lista de entidades sin drops desde el día 20
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

    /**
     * Obtiene el día a partir del cual se aplican restricciones especiales
     */
    public int getSpecialRulesDay() {
        return 20;
    }
}
