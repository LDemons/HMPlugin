package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener para modificar loot de cofres en día 20+
 */
public class LootModifierListener implements Listener {

    private final NamespacedKey LOOT_GENERATED_KEY = new NamespacedKey(HMPlugin.getInstance(), "hm_loot_generated");

    /**
     * DÍA 20: Modificar loot de cofres en Ancient Cities y Trial Chambers
     */
    @EventHandler
    public void onChestOpen(InventoryOpenEvent e) {
        if (GameManager.getInstance().getDay() < 20)
            return;
        if (!(e.getPlayer() instanceof Player))
            return;
        if (!(e.getInventory().getHolder() instanceof Chest))
            return;

        Chest chest = (Chest) e.getInventory().getHolder();
        Player player = (Player) e.getPlayer();

        // Verificar si ya se generó loot en este cofre usando PDC (Persistente)
        if (chest.getPersistentDataContainer().has(LOOT_GENERATED_KEY, PersistentDataType.BYTE)) {
            return;
        }

        boolean isAncient = isInAncientCity(chest);
        boolean isTrial = !isAncient && isInTrialChamber(chest); // Solo buscar si no es Ancient City

        // Si se detectó alguna estructura
        if (isAncient || isTrial) {
            chest.getPersistentDataContainer().set(LOOT_GENERATED_KEY, PersistentDataType.BYTE, (byte) 1);
            chest.update(); // Importante para guardar los cambios en el bloque

            // Añadir items con 1 tick de delay para asegurar que el loot vanilla se generó
            HMPlugin.getInstance().getServer().getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                if (isAncient) {
                    addGoldToChest(e.getInventory(), player);
                } else {
                    addTotemToChest(e.getInventory(), player);
                }
            }, 1L);
        }
    }

    /**
     * Verificar si el cofre está en una Ancient City
     * Criterios: Y < 0 Y bioma Deep Dark
     */
    private boolean isInAncientCity(Chest chest) {
        if (chest.getY() >= 0)
            return false;

        Biome biome = chest.getWorld().getBiome(chest.getLocation());
        return biome == Biome.DEEP_DARK;
    }

    /**
     * Verificar si el cofre está en una Trial Chamber
     * Criterios: Buscar Trial Spawners en un radio de 50 bloques
     */
    private boolean isInTrialChamber(Chest chest) {
        int radius = 50;
        int chestX = chest.getX();
        int chestY = chest.getY();
        int chestZ = chest.getZ();

        // Buscar trial spawners cercanos
        for (int x = chestX - radius; x <= chestX + radius; x++) {
            for (int y = chestY - radius; y <= chestY + radius; y++) {
                for (int z = chestZ - radius; z <= chestZ + radius; z++) {
                    Material blockType = chest.getWorld().getBlockAt(x, y, z).getType();
                    if (blockType == Material.TRIAL_SPAWNER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Añadir 64 lingotes de oro al cofre
     */
    private void addGoldToChest(Inventory inventory, Player player) {
        ItemStack gold = new ItemStack(Material.GOLD_INGOT, 64);
        inventory.addItem(gold);
        // MessageUtils.send(player, "<gold>✨ Ancient City: +64 Lingotes de Oro");
    }

    /**
     * Añadir tótem al cofre
     */
    private void addTotemToChest(Inventory inventory, Player player) {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        inventory.addItem(totem);
        // MessageUtils.send(player, "<yellow>✨ Trial Chamber: +1 Tótem de
        // Inmortalidad");
    }
}
