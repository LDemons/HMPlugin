package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.entity.Spider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class EntityListener implements Listener {
    private final HMPlugin plugin;

    public EntityListener(HMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpiderSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Spider) {
            Spider spider = (Spider) event.getEntity();
            plugin.getDifficultyManager().applySpiderEffects(spider);
        }
    }
}
