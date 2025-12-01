package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Manager para el scoreboard de vida de jugadores
 */
public class ScoreboardManager {

    private static ScoreboardManager instance;
    private final HMPlugin plugin;
    private Scoreboard scoreboard;
    private Objective healthObjective;

    private ScoreboardManager(HMPlugin plugin) {
        this.plugin = plugin;
    }

    public static ScoreboardManager getInstance() {
        if (instance == null) {
            instance = new ScoreboardManager(HMPlugin.getInstance());
        }
        return instance;
    }

    /**
     * Inicializar el scoreboard de vida
     */
    public void initialize() {
        // Obtener o crear el scoreboard principal
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            plugin.getLogger().warning("No se pudo obtener el ScoreboardManager");
            return;
        }

        scoreboard = manager.getMainScoreboard();

        // Crear objetivo de salud si no existe
        healthObjective = scoreboard.getObjective("health");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective(
                    "health",
                    Criteria.HEALTH,
                    net.kyori.adventure.text.Component.text("❤"));
        }

        // Mostrar debajo del nombre
        healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);

        // Aplicar scoreboard a todos los jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyScoreboard(player);
        }

        plugin.getLogger().info("Scoreboard de vida inicializado correctamente");
    }

    /**
     * Aplicar el scoreboard a un jugador específico
     */
    public void applyScoreboard(Player player) {
        if (scoreboard != null) {
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * Actualizar la vida de un jugador en el scoreboard
     * (Esto se hace automáticamente por el Criteria.HEALTH, pero lo dejamos por si
     * acaso)
     */
    public void updateHealth(Player player) {
        if (healthObjective != null) {
            int health = (int) Math.ceil(player.getHealth());
            healthObjective.getScore(player.getName()).setScore(health);
        }
    }

    /**
     * Remover el scoreboard (útil para reset o disable)
     */
    public void disable() {
        if (healthObjective != null) {
            healthObjective.unregister();
            healthObjective = null;
        }
        instance = null;
    }
}