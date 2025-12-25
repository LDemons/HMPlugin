package HM.LuckyDemon.events;

import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * DÍA 40+: Bloquea el crafteo de antorchas y antorchas de redstone
 */
public class Day40CraftListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        // Solo aplicar desde día 40
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        ItemStack result = e.getInventory().getResult();

        if (result == null) {
            return;
        }

        Material resultType = result.getType();

        // Bloquear antorchas normales, antorchas de alma y antorchas de redstone
        if (resultType == Material.TORCH ||
                resultType == Material.SOUL_TORCH ||
                resultType == Material.REDSTONE_TORCH) {

            // Cancelar el crafteo
            e.getInventory().setResult(null);

        }
    }
}