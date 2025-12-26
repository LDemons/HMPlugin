package HM.LuckyDemon.tasks;

import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * DÍA 40+: Spawnea mobs hostiles en Mushroom Islands
 */
public class MushroomMobTask extends BukkitRunnable {

    private final Random random = new Random();
    private final EntityType[] HOSTILE_MOBS = {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.WITCH,
    };

    @Override
    public void run() {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            // Verificar si está en Mushroom Fields
            Biome biome = player.getLocation().getBlock().getBiome();
            if (biome != Biome.MUSHROOM_FIELDS) {
                continue;
            }

            // 50% de probabilidad cada ejecución (cada 30 segundos)
            if (random.nextInt(100) < 50) {
                spawnRandomMobNear(player);
            }
        }
    }

    private void spawnRandomMobNear(Player player) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Buscar ubicación aleatoria cerca del jugador (20-40 bloques)
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 20 + random.nextDouble() * 20;

        double x = playerLoc.getX() + Math.cos(angle) * distance;
        double z = playerLoc.getZ() + Math.sin(angle) * distance;

        // Encontrar el bloque más alto
        int y = world.getHighestBlockYAt((int) x, (int) z);
        Location spawnLoc = new Location(world, x, y + 1, z);

        // Verificar que sigue siendo Mushroom Fields
        if (spawnLoc.getBlock().getBiome() != Biome.MUSHROOM_FIELDS) {
            return;
        }

        // Spawnear mob aleatorio
        EntityType mobType = HOSTILE_MOBS[random.nextInt(HOSTILE_MOBS.length)];
        org.bukkit.entity.Entity entity = world.spawnEntity(spawnLoc, mobType);

        // Aplicar efectos del DifficultyManager
        if (entity instanceof org.bukkit.entity.LivingEntity) {
            HM.LuckyDemon.HMPlugin.getInstance().getDifficultyManager()
                    .applyMobEffects((org.bukkit.entity.LivingEntity) entity);
        }
    }
}