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
import org.bukkit.inventory.ItemStack;

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

        // Verificar que sea un tótem
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean totemInMainHand = mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING;
        boolean totemInOffHand = offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING;

        if (!totemInMainHand && !totemInOffHand) {
            return;
        }

        // DÍA 40+: Consumir 2 tótems en vez de 1
        if (currentDay >= 40) {
            // Contar tótems en el inventario
            int totemsInInventory = countTotemsInInventory(player);

            // Si el jugador tiene otro tótem, consumirlo también
            if (totemsInInventory >= 1) {
                // Buscar y remover el segundo tótem del inventario
                removeSecondTotemFromInventory(player, totemInMainHand, totemInOffHand);

                // Mensaje de consumo doble
                MessageUtils.send(player, "<gray>Se han consumido <yellow>2 tótems <gray>(Día 40+)");
            } else {
                // Solo tiene 1 tótem, avisar
                MessageUtils.send(player, "<gray>Solo tenías <yellow>1 tótem <gray>(Se requieren 2 en día 40+)");
            }
        }

        // Calcular probabilidad de fallo
        int failChance = currentDay >= 40 ? 3 : 1; // 3% en día 40+, 1% en día 30-39
        int roll = random.nextInt(100) + 1; // 1-100
        boolean failed = roll <= failChance;

        if (failed) {
            // CANCELAR la resurrección
            e.setCancelled(true);

            // Mensaje de fallo en broadcast
            Bukkit.broadcast(MessageUtils.format(
                    "<gray>El jugador <white>" + player.getName()
                            + "<gray> ha consumido un tótem <red>(Probabilidad: <bold>" + roll
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
            Bukkit.broadcast(MessageUtils.format(
                    "<gray>El jugador <white>" + player.getName()
                            + "<gray> ha consumido un tótem <green>(Probabilidad: <bold>" + roll
                            + " <gray>[> <green>" + failChance + "<gray>])"));
        }
    }

    /**
     * Cuenta cuántos tótems tiene el jugador en el inventario (excluyendo manos)
     */
    private int countTotemsInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Remueve el segundo tótem del inventario del jugador
     */
    private void removeSecondTotemFromInventory(Player player, boolean totemInMainHand, boolean totemInOffHand) {
        // Primero buscar en el inventario principal
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return;
            }
        }

        // Si no hay en el inventario, consumir de la otra mano
        if (totemInMainHand && totemInOffHand) {
            // Si tiene en ambas manos, consumir el de la mano secundaria
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.getAmount() > 1) {
                offHand.setAmount(offHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }
}