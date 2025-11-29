package HM.LuckyDemon.tasks;

import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class InfoTask extends BukkitRunnable {

    @Override
    public void run() {
        // Obtenemos el mundo principal (asumimos que es el default)
        World world = Bukkit.getWorlds().get(0);
        int day = GameManager.getInstance().getDay();

        // Calculamos tiempo de tormenta si está lloviendo
        String weatherStatus = "";
        if (world.hasStorm()) {
            int secondsLeft = world.getWeatherDuration() / 20;
            weatherStatus = " <gray>| <blue>⛈ " + MessageUtils.formatTime(secondsLeft);
        }

        // Creamos el mensaje
        Component actionBar = MessageUtils.format(
                "<gradient:red:gold><bold>PERMADEATH</gradient> <gray>» <yellow>Día " + day + weatherStatus
        );

        // Se lo enviamos a todos
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(actionBar);
        }
    }
}