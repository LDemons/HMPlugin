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
            if (secondsLeft <= 0)
                return;

            // >>> CAMBIO: Obtenemos también el DÍA actual <<<
            int currentDay = GameManager.getInstance().getDay();

            // DÍA 25+: Durante Death Train, todos los mobs tienen Fuerza I, Velocidad I y
            // Resistencia I
            if (HMPlugin.getInstance().getDifficultyManager().isDay25OrLater()) {
                applyDeathTrainEffects(world);
            }

            // >>> CAMBIO: Fusionamos el mensaje (Día + Tormenta) <<<
            // Así el jugador ve toda la info importante de un vistazo
            Component actionBar = MessageUtils.format(
                    "<gradient:red:gold><bold>PERMADEATH</gradient> <gray>» <yellow>Día " + currentDay +
                            " <gray>| <gradient:aqua:blue>⛈ DEATH TRAIN: <white>"
                            + MessageUtils.formatTime(secondsLeft));

            // Se lo enviamos a todos
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(actionBar);
            }
        }
    }

    /**
     * DÍA 25+: Durante Death Train, aplicar Fuerza I, Velocidad I y Resistencia I a
     * todos los mobs
     */
    private void applyDeathTrainEffects(World world) {
        // Aplicar efectos a todos los mobs hostiles durante la tormenta
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity instanceof Mob && !(entity instanceof Player)) {
                Mob mob = (Mob) entity;

                // Solo aplicar a mobs hostiles
                if (isHostileMob(mob)) {
                    // Aplicar efectos solo si no los tiene o están por expirar (< 40 ticks)
                    applyEffectIfNeeded(mob, PotionEffectType.STRENGTH, 0);
                    applyEffectIfNeeded(mob, PotionEffectType.SPEED, 0);
                    applyEffectIfNeeded(mob, PotionEffectType.RESISTANCE, 0);
                }
            }
        }
    }

    /**
     * Aplica un efecto de poción solo si el mob no lo tiene o está por expirar
     */
    private void applyEffectIfNeeded(Mob mob, PotionEffectType type, int amplifier) {
        PotionEffect currentEffect = mob.getPotionEffect(type);

        // Si no tiene el efecto o le quedan menos de 40 ticks (2 segundos), renovarlo
        if (currentEffect == null || currentEffect.getDuration() < 40) {
            mob.addPotionEffect(new PotionEffect(type, 100, amplifier, false, false));
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