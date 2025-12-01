package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener para remover items de netherite de bastiones desde día 1
 */
public class BastionLootListener implements Listener {

    private final NamespacedKey BASTION_CHECKED_KEY = new NamespacedKey(HMPlugin.getInstance(), "hm_bastion_checked");

    /**
     * DÍA 1+: Remover netherite upgrade template y netherite ingot de bastiones
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBastionChestOpen(InventoryOpenEvent e) {
        // Solo aplicar desde día 1 en adelante
        if (GameManager.getInstance().getDay() < 1)
            return;

        if (!(e.getInventory().getHolder() instanceof Chest))
            return;

        Chest chest = (Chest) e.getInventory().getHolder();

        // Verificar si ya se procesó este cofre
        if (chest.getPersistentDataContainer().has(BASTION_CHECKED_KEY, PersistentDataType.BYTE)) {
            return;
        }

        // Verificar si está en un bastión
        if (!isInBastion(chest)) {
            return;
        }

        // Marcar como procesado
        chest.getPersistentDataContainer().set(BASTION_CHECKED_KEY, PersistentDataType.BYTE, (byte) 1);
        chest.update();

        // Remover items con 1 tick de delay para asegurar que el loot vanilla se generó
        HMPlugin.getInstance().getServer().getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
            removeNetheriteItems(e.getInventory());
        }, 1L);
    }

    /**
     * Verificar si el cofre está en un bastión
     * Criterios: Bioma Nether Wastes, Crimson Forest, Warped Forest o Basalt Deltas
     * Y altura entre Y=32 y Y=118 (rango típico de bastiones)
     */
    private boolean isInBastion(Chest chest) {
        // Verificar que esté en el Nether
        if (!chest.getWorld().getEnvironment().equals(org.bukkit.World.Environment.NETHER)) {
            return false;
        }

        int y = chest.getY();
        // Bastiones generan típicamente entre Y=32 y Y=118
        if (y < 32 || y > 118) {
            return false;
        }

        Biome biome = chest.getWorld().getBiome(chest.getLocation());

        // Bastiones pueden generar en estos biomas
        return biome == Biome.NETHER_WASTES ||
                biome == Biome.CRIMSON_FOREST ||
                biome == Biome.WARPED_FOREST ||
                biome == Biome.BASALT_DELTAS ||
                biome == Biome.SOUL_SAND_VALLEY;
    }

    /**
     * Remover netherite upgrade template y netherite ingot del inventario
     */
    private void removeNetheriteItems(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null)
                continue;

            Material type = item.getType();

            // Remover netherite upgrade template
            if (type == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                inventory.setItem(i, null);
            }

            // Remover netherite ingot
            if (type == Material.NETHERITE_INGOT) {
                inventory.setItem(i, null);
            }
        }
    }
}