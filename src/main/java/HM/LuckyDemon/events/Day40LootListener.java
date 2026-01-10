package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * DÍA 40+: Modificaciones de loot y crafteo
 */
public class Day40LootListener implements Listener {

    /**
     * Las Elytras en cofres (End Ships) aparecen casi rotas
     */
    @EventHandler
    public void onLootGenerate(LootGenerateEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        for (ItemStack item : e.getLoot()) {
            if (item != null && item.getType() == Material.ELYTRA) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof Damageable) {
                    Damageable damageable = (Damageable) meta;
                    // Elytra tiene 432 de durabilidad, dejamos solo 1
                    damageable.setDamage(431);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    /**
     * Restringir receta de Chorus Flower -> Dragon's Breath a día 40+
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftDragonBreath(PrepareItemCraftEvent e) {
        if (e.getRecipe() == null || e.getRecipe().getResult() == null) {
            return;
        }

        // Verificar si el resultado es Dragon's Breath
        if (e.getRecipe().getResult().getType() != Material.DRAGON_BREATH) {
            return;
        }

        // Si no es día 40+, cancelar el crafteo
        if (GameManager.getInstance().getDay() < 40) {
            e.getInventory().setResult(null);
        }
    }

    /**
     * Elytras recogidas de marcos de items también se dañan
     */
    @EventHandler
    public void onPickupElytra(org.bukkit.event.player.PlayerPickupItemEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        ItemStack item = e.getItem().getItemStack();
        if (item.getType() == Material.ELYTRA) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                // Solo dañar si está nueva o casi nueva
                if (damageable.getDamage() < 400) {
                    damageable.setDamage(431);
                    item.setItemMeta(meta);
                }
            }
        }
    }
}