package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPluggin;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameManager {

    private static GameManager instance;
    private int currentDay;

    // Constructor privado (Singleton)
    private GameManager() {
        // Cargar el día desde la config. Si no existe, es día 1.
        this.currentDay = HMPluggin.getInstance().getConfig().getInt("game.day", 1);
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public int getDay() {
        return currentDay;
    }

    public void setDay(int day) {
        this.currentDay = day;
        // Guardar en config para no perder el progreso al reiniciar
        HMPluggin.getInstance().getConfig().set("game.day", day);
        HMPluggin.getInstance().saveConfig();

        // Anunciar cambio
        Bukkit.broadcast(MessageUtils.format("<yellow>¡El tiempo ha cambiado! Ahora es el día <red><bold>" + day));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public void advanceDay() {
        setDay(currentDay + 1);
    }

    // Nuevo metodo para mostrar la info a un jugador específico (Action Bar)
    public void showInfo(Player player) {
        World world = player.getWorld();

        String weatherStatus = "";
        if (world.hasStorm()) {
            int secondsLeft = world.getWeatherDuration() / 20;
            // Usamos el mismo formato que en StormTask
            weatherStatus = " <gray>| <gradient:aqua:blue>⛈ DEATH TRAIN: <white>" + MessageUtils.formatTime(secondsLeft);
        }

        Component actionBar = MessageUtils.format(
                "<gradient:red:gold><bold>PERMADEATH</gradient> <gray>» <yellow>Día " + currentDay + weatherStatus
        );

        player.sendActionBar(actionBar);
    }
}