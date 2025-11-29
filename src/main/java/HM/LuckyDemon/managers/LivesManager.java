package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPluggin;
import org.bukkit.entity.Player;

public class LivesManager {

    private static LivesManager instance;
    private static final int MAX_LIVES = 3;

    private LivesManager() {
    }

    public static LivesManager getInstance() {
        if (instance == null) {
            instance = new LivesManager();
        }
        return instance;
    }

    /**
     * Obtiene las vidas restantes de un jugador
     */
    public int getLives(Player player) {
        return HMPluggin.getInstance().getConfig().getInt("player_lives." + player.getUniqueId().toString(), MAX_LIVES);
    }

    /**
     * Establece las vidas de un jugador
     */
    public void setLives(Player player, int lives) {
        HMPluggin.getInstance().getConfig().set("player_lives." + player.getUniqueId().toString(), lives);
        HMPluggin.getInstance().saveConfig();
    }

    /**
     * Resta una vida al jugador
     * 
     * @return vidas restantes después de restar
     */
    public int removeLife(Player player) {
        int currentLives = getLives(player);
        int newLives = Math.max(0, currentLives - 1);
        setLives(player, newLives);
        return newLives;
    }

    /**
     * Añade una vida al jugador (sin exceder el máximo)
     * 
     * @return vidas después de añadir
     */
    public int addLife(Player player) {
        int currentLives = getLives(player);
        int newLives = Math.min(MAX_LIVES, currentLives + 1);
        setLives(player, newLives);
        return newLives;
    }

    /**
     * Reinicia las vidas de un jugador al máximo
     */
    public void resetLives(Player player) {
        setLives(player, MAX_LIVES);
    }

    /**
     * Verifica si el jugador tiene vidas restantes
     */
    public boolean hasLives(Player player) {
        return getLives(player) > 0;
    }

    /**
     * Obtiene el número máximo de vidas
     */
    public int getMaxLives() {
        return MAX_LIVES;
    }
}
