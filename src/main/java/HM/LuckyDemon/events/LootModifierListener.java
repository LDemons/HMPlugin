package HM.LuckyDemon.events;

import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Cambios al loot de cofres de estructuras. Se decide por la loot table, asi solo afecta a los
 * cofres que genera la estructura (no a los que ponen los jugadores) y no hay que buscar bloques.
 */
public class LootModifierListener implements Listener {

    @EventHandler
    public void onLootGenerate(LootGenerateEvent e) {
        String table = e.getLootTable().getKey().getKey();

        // DÍA 1+: sin netherite en los bastiones
        if (table.startsWith("chests/bastion_")) {
            e.getLoot().removeIf(item -> item != null && (item.getType() == Material.NETHERITE_INGOT
                    || item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        }

        if (GameManager.getInstance().getDay() < 20) {
            return;
        }

        // DÍA 20+: 64 de oro en Ancient Cities y un totem en los cofres de Trial Chambers (no en las vaults)
        if (table.startsWith("chests/ancient_city")) {
            e.getLoot().add(new ItemStack(Material.GOLD_INGOT, 64));
        } else if (table.startsWith("chests/trial_chambers/") && e.getInventoryHolder() instanceof Chest) {
            e.getLoot().add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
    }
}
