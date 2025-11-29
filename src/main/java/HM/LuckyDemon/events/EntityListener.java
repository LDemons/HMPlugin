package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class EntityListener implements Listener {
    private final HMPlugin plugin;

    public EntityListener(HMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Delegar al DifficultyManager para aplicar efectos a cualquier mob
        plugin.getDifficultyManager().applyMobEffects(event.getEntity());
    }
}
