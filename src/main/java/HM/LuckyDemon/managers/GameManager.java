package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameManager {

    private static GameManager instance;
    private int currentDay;

    private GameManager() {
        this.currentDay = HMPlugin.getInstance().getConfig().getInt("game.day", 1);
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
        HMPlugin.getInstance().getConfig().set("game.day", day);
        HMPlugin.getInstance().saveConfig();

        HMPlugin.getInstance().getDifficultyManager().setCurrentDay(day);

        Bukkit.broadcast(MessageUtils.format("<yellow>¡El tiempo ha cambiado! Ahora es el día <red><bold>" + day));

        int diffLevel = HMPlugin.getInstance().getDifficultyManager().getDifficultyLevel();
        if (diffLevel > 0 && day % 10 == 0) {
            Bukkit.broadcast(MessageUtils.format("<red><bold>⚠ ¡NIVEL DE DIFICULTAD AUMENTADO! ⚠"));
            Bukkit.broadcast(MessageUtils.format("<gold>La supervivencia será más difícil..."));
        }

        // DÍA 20: Activar KeepInventory y anunciar nuevas mecánicas
        if (day == 20) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.KEEP_INVENTORY, true);
            }
            Bukkit.broadcast(MessageUtils.format("<green><bold>✓ KeepInventory activado - No perderás items al morir"));
            Bukkit.broadcast(MessageUtils.format("<dark_red><bold>⚠ DÍA 20: NUEVAS MECÁNICAS DESBLOQUEADAS"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Ancient Cities dan oro"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Trial Chambers dan tótems"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Animales pasivos son agresivos"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Murciélagos atacan y dan ceguera"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Ghasts son hostiles normales"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Wardens 2x velocidad"));
            Bukkit.broadcast(MessageUtils.format("<gold>- Bad Omen da Weakness"));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public void advanceDay() {
        setDay(currentDay + 1);
    }

    public void reset() {
        this.currentDay = 0;
        HMPlugin.getInstance().getConfig().set("game.day", 0);
        HMPlugin.getInstance().saveConfig();

        HMPlugin.getInstance().getDifficultyManager().reset();

        // Desactivar KeepInventory al resetear
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }

        Bukkit.broadcast(MessageUtils.format("<gold>⚠ El juego ha sido reseteado al día 0"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        }
    }

    public void showInfo(Player player) {
        World world = player.getWorld();

        String weatherStatus = "";
        if (world.hasStorm()) {
            int secondsLeft = world.getWeatherDuration() / 20;
            weatherStatus = " <gray>| <gradient:aqua:blue>⛈ DEATH TRAIN: <white>"
                    + MessageUtils.formatTime(secondsLeft);
        }

        Component actionBar = MessageUtils.format(
                "<gradient:red:gold><bold>PERMADEATH</gradient> <gray>» <yellow>Día " + currentDay + weatherStatus);

        player.sendActionBar(actionBar);
    }
}