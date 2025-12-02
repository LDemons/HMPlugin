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
 * Listener para manejar el fallo de tótems en día 30+
 */
public class TotemFailListener implements Listener {

    private final Random random = new Random();

    /**
     * DÍA 30+: Los tótems tienen 1% de probabilidad de fallar
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent e) {
        // Solo aplicar desde día 30
        if (GameManager.getInstance().getDay() < 30) {
            return;
        }

        // Solo para jugadores
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getEntity();

        // Verificar que sea un tótem
        ItemStack hand = e.getEntity().getEquipment().getItemInMainHand();
        ItemStack offHand = e.getEntity().getEquipment().getItemInOffHand();

        boolean isTotem = (hand != null && hand.getType() == Material.TOTEM_OF_UNDYING) ||
                (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING);

        if (!isTotem) {
            return;
        }

        // Generar número aleatorio (1-100)
        int roll = random.nextInt(100) + 1; // 1-100
        boolean failed = roll == 100; // 1% de fallo (100)

        if (failed) {
            // CANCELAR la resurrección
            e.setCancelled(true);

            // Mensaje de fallo en broadcast
            Bukkit.broadcast(MessageUtils.format(
                    "<gray>El jugador <white>" + player.getName()
                            + "<gray> ha consumido un tótem <red>(Probabilidad: <bold>" + roll
                            + " <gray>[= <red>100<gray>])"));

            // Mensaje personal de fallo
            MessageUtils.send(player, "<dark_red><bold>💀 ¡EL TÓTEM HA FALLADO!");

            // Sonido de fallo
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);

            // Log del fallo
            HMPlugin.getInstance().getLogger()
                    .info("Tótem de " + player.getName() + " ha fallado (roll: " + roll + "/100)");
        } else {
            // Tótem funcionó correctamente
            Bukkit.broadcast(MessageUtils.format(
                    "<gray>El jugador <white>" + player.getName()
                            + "<gray> ha consumido un tótem <green>(Probabilidad: <bold>" + roll
                            + " <gray>[= <green>99<gray>])"));
        }
    }
}