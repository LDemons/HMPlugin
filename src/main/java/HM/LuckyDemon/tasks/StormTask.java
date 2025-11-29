package HM.LuckyDemon.tasks;

import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
}