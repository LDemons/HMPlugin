package HM.LuckyDemon.events;

import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Listener para spawnear TNT al picar Ancient Debris desde día 1
 */
public class AncientDebrisListener implements Listener {

    /**
     * DÍA 1+: Al picar Ancient Debris, spawnear TNT activada
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAncientDebrisBreak(BlockBreakEvent e) {
        // Solo aplicar desde día 1 en adelante
        if (GameManager.getInstance().getDay() < 1)
            return;

        Block block = e.getBlock();

        // Verificar que sea Ancient Debris
        if (block.getType() != Material.ANCIENT_DEBRIS)
            return;

        // Cancelar el drop normal del Ancient Debris
        e.setDropItems(false);

        // Spawnear TNT activada en la ubicación del bloque
        TNTPrimed tnt = block.getWorld().spawn(
                block.getLocation().add(0.5, 0.0, 0.5), // Centrar en el bloque
                TNTPrimed.class);

        // Configurar la TNT
        tnt.setFuseTicks(40); // 2 segundos (40 ticks = 2s)
        tnt.setYield(4.0f); // Potencia de explosión (4.0 = TNT normal)
        tnt.setIsIncendiary(false); // No crear fuego
    }
}