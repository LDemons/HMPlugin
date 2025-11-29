package HM.LuckyDemon.tasks;

import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class StormTask extends BukkitRunnable {

    @Override
    public void run() {
        // Obtenemos el mundo principal (el default)
        World world = Bukkit.getWorlds().get(0);

        // SOLO mostramos mensaje si hay tormenta
        if (world.hasStorm()) {
            // Convertimos ticks a segundos (1 segundo = 20 ticks)
            int secondsLeft = world.getWeatherDuration() / 20;

            // Si la tormenta está a punto de acabar (menos de 1s), no mostramos nada
            if (secondsLeft <= 0) return;

            // Creamos el mensaje con el tiempo formateado
            Component actionBar = MessageUtils.format(
                    "<gradient:aqua:blue><bold>⛈ DEATH TRAIN</gradient> <gray>» <yellow>Tiempo restante: <white>" +
                            MessageUtils.formatTime(secondsLeft)
            );

            // Se lo enviamos a todos los jugadores conectados
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(actionBar);
            }
        }
    }
}