package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Random;

/**
 * Listener para manejar el fallo de tótems en día 30+ y mecánicas día 40+
 */
public class TotemFailListener implements Listener {

    private final Random random = new Random();

    /**
     * DÍA 30+: Los tótems tienen 1% de probabilidad de fallar
     * DÍA 40+: Los tótems tienen 3% de probabilidad de fallar y consume 2 tótems
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent e) {
        int currentDay = GameManager.getInstance().getDay();

        // Solo aplicar desde día 30
        if (currentDay < 30) {
            return;
        }

        // Solo para jugadores
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getEntity();
        PlayerInventory inv = player.getInventory();

        // Mano con el totem que usa el juego (null si no hay totem)
        EquipmentSlot hand = e.getHand();
        if (hand == null) {
            return;
        }

        // DÍA 40+: se gastan 2 tótems, DÍA 60+: 3
        int needed = currentDay >= 60 ? 3 : currentDay >= 40 ? 2 : 1;
        for (int i = 1; i < needed; i++) {
            if (!removeSecondTotem(inv, hand)) {
                MessageUtils.send(player, "<gray>Solo tenías <yellow>" + i + " tótem(s) <gray>(Se requieren " + needed
                        + " en día " + (currentDay >= 60 ? 60 : 40) + "+)");
                break;
            }
        }

        // Calcular probabilidad de fallo: 7% día 60+, 5% día 50+, 3% día 40+, 1% día 30-39
        int failChance = currentDay >= 60 ? 7 : currentDay >= 50 ? 5 : currentDay >= 40 ? 3 : 1;
        int roll = random.nextInt(100) + 1; // 1-100
        boolean failed = roll <= failChance;

        if (failed) {
            // CANCELAR la resurrección
            e.setCancelled(true);
            // Cancelada, el juego no gasta el totem de la mano: se quita aqui para que el fallo cueste
            inv.setItem(hand, null);

            // Determinar cantidad de tótems consumidos
            String totemCount = needed > 1 ? needed + " tótems" : "un tótem";

            // Mensaje de fallo en broadcast
            Bukkit.broadcast(MessageUtils.format(
                    "<gray>El jugador <white>" + player.getName()
                            + "<gray> ha consumido " + totemCount + " <red>(Probabilidad: <bold>" + roll
                            + " <gray>[<= <red>" + failChance + "<gray>])"));

            // Mensaje personal de fallo
            MessageUtils.send(player, "<dark_red><bold> ¡EL TÓTEM HA FALLADO!");

            // Sonido de fallo
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);

            // Log del fallo
            HMPlugin.getInstance().getLogger()
                    .info("Tótem de " + player.getName() + " ha fallado (roll: " + roll + "/" + failChance + "%)");
        } else {
            // Tótem funcionó correctamente
            String totemCount = needed > 1 ? needed + " tótems" : "un tótem";

            Bukkit.broadcast(MessageUtils.format(
                    "<gray>El jugador <white>" + player.getName()
                            + "<gray> ha consumido " + totemCount + " <green>(Probabilidad: <bold>" + roll
                            + " <gray>[> <green>" + failChance + "<gray>])"));
        }
    }

    /**
     * DÍA 60: ya no se consiguen tótems de ninguna forma. Se borran de los drops de mobs (no de jugadores: serían
     * sus propios tótems) y del loot de cofres. MONITOR para ir después de todos los que los agregan.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemDrop(org.bukkit.event.entity.EntityDeathEvent e) {
        if (GameManager.getInstance().getDay() >= 60 && !(e.getEntity() instanceof Player)) {
            e.getDrops().removeIf(item -> item.getType() == Material.TOTEM_OF_UNDYING);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemLoot(org.bukkit.event.world.LootGenerateEvent e) {
        if (GameManager.getInstance().getDay() >= 60) {
            e.getLoot().removeIf(item -> item != null && item.getType() == Material.TOTEM_OF_UNDYING);
        }
    }

    /**
     * Quita un segundo totem: del inventario (sin contar el que se esta usando) o de la otra mano.
     */
    private boolean removeSecondTotem(PlayerInventory inv, EquipmentSlot usedHand) {
        int usedSlot = usedHand == EquipmentSlot.HAND ? inv.getHeldItemSlot() : -1;
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (i != usedSlot && item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                item.setAmount(item.getAmount() - 1);
                inv.setItem(i, item); // se vuelve a poner por si getItem devuelve una copia
                return true;
            }
        }
        ItemStack offHand = inv.getItemInOffHand();
        if (usedHand == EquipmentSlot.HAND && offHand.getType() == Material.TOTEM_OF_UNDYING) {
            offHand.setAmount(offHand.getAmount() - 1);
            inv.setItemInOffHand(offHand);
            return true;
        }
        return false;
    }
}
