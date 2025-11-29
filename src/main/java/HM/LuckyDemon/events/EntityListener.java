package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.event.Listener;

/**
 * EntityListener - DEPRECADO
 * Toda la lógica de spawn de mobs se maneja ahora en
 * PlayerListener.onCreatureSpawn()
 * Este listener se mantiene vacío para evitar conflictos
 */
public class EntityListener implements Listener {
    private final HMPlugin plugin;

    public EntityListener(HMPlugin plugin) {
        this.plugin = plugin;
    }

    // No hay eventos aquí - todo se maneja en PlayerListener
}
