package HM.LuckyDemon.managers;

import HM.LuckyDemon.HMPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.object.ObjectContents;
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

        // Configurar para mostrar corazones en lugar de números (siempre)
        healthObjective.setRenderType(org.bukkit.scoreboard.RenderType.HEARTS);

        // Mostrar en el tabulador (player list)
        healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

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
        updateTabName(player);
    }

    /** Cabeza con la skin + corazon de vidas + nombre, en el tab. */
    public void updateTabName(Player player) {
        player.playerListName(prefix(player).append(Component.text(player.getName())));
    }

    /**
     * Cabeza con la skin y corazon del color de las vidas (verde 3, amarillo 2, rojo 1, gris 0).
     * El server es offline-mode: el tab no dibuja cabezas solo, por eso va como objeto de texto.
     */
    public static Component prefix(Player player) {
        int lives = LivesManager.getInstance().getLives(player);
        NamedTextColor color = lives >= 3 ? NamedTextColor.GREEN
                : lives == 2 ? NamedTextColor.YELLOW
                : lives == 1 ? NamedTextColor.RED
                : NamedTextColor.GRAY;
        // Sin texturas (offline-mode sin plugin de skins) solo el nombre: el cliente busca la skin en Mojang
        ObjectContents head = player.getPlayerProfile().hasTextures()
                ? ObjectContents.playerHead(player)
                : ObjectContents.playerHead(player.getName());
        return Component.object(head)
                .append(Component.text(" ❤ ", color))
                .colorIfAbsent(NamedTextColor.WHITE);
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