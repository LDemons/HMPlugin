package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class BedrockListener implements Listener {
    private final HMPlugin plugin;

    public BedrockListener(HMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Solo aplicar desde día 30
        if (plugin.getDifficultyManager().getCurrentDay() < 30) {
            return;
        }

        // Verificar si el jugador está parado sobre bedrock
        if (player.getLocation().subtract(0, 1, 0).getBlock().getType() == Material.BEDROCK) {
            // Lanzar al jugador por los aires
            Vector velocity = new Vector(0, 3.0, 0); // Velocidad vertical hacia arriba
            player.setVelocity(velocity);
        }
    }
}