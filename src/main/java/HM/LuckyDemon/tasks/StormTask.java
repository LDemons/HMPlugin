package HM.LuckyDemon.tasks;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class StormTask extends BukkitRunnable {

    @Override
    public void run() {
        // Obtenemos el mundo principal
        World world = Bukkit.getWorlds().get(0);

        // SOLO mostramos mensaje si hay tormenta
        if (world.hasStorm()) {
            int secondsLeft = world.getWeatherDuration() / 20;

            // Si la tormenta está a punto de acabar, no mostramos nada
            if (secondsLeft <= 0) return;

            // >>> CAMBIO: Obtenemos también el DÍA actual <<<
            int currentDay = GameManager.getInstance().getDay();
            
            // Aplicar efectos a mobs durante Death Train si es día 25+
            if (HMPlugin.getInstance().getDifficultyManager().isDay25OrLater()) {
                applyDeathTrainEffects(world);
            }

            // >>> CAMBIO: Fusionamos el mensaje (Día + Tormenta) <<<
            // Así el jugador ve toda la info importante de un vistazo
            Component actionBar = MessageUtils.format(
                    "<gradient:red:gold><bold>PERMADEATH</gradient> <gray>» <yellow>Día " + currentDay +
                            " <gray>| <gradient:aqua:blue>⛈ DEATH TRAIN: <white>" + MessageUtils.formatTime(secondsLeft)
            );

            // Se lo enviamos a todos
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(actionBar);
            }
        }
    }

    private void applyDeathTrainEffects(World world) {
        // Aplicar Fuerza I, Velocidad I y Resistencia I a todos los mobs hostiles
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity instanceof Mob && !(entity instanceof Player)) {
                Mob mob = (Mob) entity;
                
                // Solo aplicar si es un mob hostil (tiene objetivo de ataque)
                if (mob.getTarget() != null || isHostileMob(mob)) {
                    // Verificar si ya tiene los efectos para no aplicarlos cada tick
                    if (!mob.hasPotionEffect(PotionEffectType.STRENGTH)) {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, false, false));
                    }
                    if (!mob.hasPotionEffect(PotionEffectType.SPEED)) {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, false, false));
                    }
                    if (!mob.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, false, false));
                    }
                }
            }
        }
    }

    private boolean isHostileMob(Mob mob) {
        // Lista de mobs hostiles comunes
        return mob instanceof org.bukkit.entity.Monster ||
               mob instanceof org.bukkit.entity.Slime ||
               mob instanceof org.bukkit.entity.Ghast ||
               mob instanceof org.bukkit.entity.Phantom;
    }
}