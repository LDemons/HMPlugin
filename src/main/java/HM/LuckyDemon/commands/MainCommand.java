package HM.LuckyDemon.commands;

import HM.LuckyDemon.items.HMItems;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MainCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "<red>Solo jugadores pueden usar esto.");
            return true;
        }

        // Si no escribe nada, mostramos ayuda
        if (args.length == 0) {
            MessageUtils.send(player, "<yellow>Usa: /hm day [numero] o /hm items");
            return true;
        }

        // --- Comando: /hm items (Solo OP) ---
        if (args[0].equalsIgnoreCase("items")) {
            if (!player.isOp()) {
                MessageUtils.send(player, "<red>No tienes permisos.");
                return true;
            }

            // Entregamos los items
            player.getInventory().addItem(HMItems.createLifeOrb());
            player.getInventory().addItem(HMItems.createEndRelic());
            player.getInventory().addItem(HMItems.craftInfernalHelmet());
            player.getInventory().addItem(HMItems.craftInfernalChestplate());
            // Puedes añadir el resto si quieres (leggings, boots, elytra)

            MessageUtils.send(player, "<green>¡Ítems entregados! Revisa tu inventario.");
            return true;
        }

        // --- Comando: /hm day ---
        if (args[0].equalsIgnoreCase("day")) {

            // Caso 1: /hm day (Sin argumentos) -> Muestra la info (Público)
            if (args.length == 1) {
                GameManager.getInstance().showInfo(player);
                return true;
            }

            // Caso 2: /hm day <numero> -> Cambia el día (Solo OP)
            if (args.length == 2) {
                if (!player.isOp()) {
                    MessageUtils.send(player, "<red>No tienes permisos para cambiar el día.");
                    return true;
                }
                try {
                    int newDay = Integer.parseInt(args[1]);
                    GameManager.getInstance().setDay(newDay);
                    MessageUtils.send(player, "<green>Día actualizado correctamente a " + newDay);
                } catch (NumberFormatException e) {
                    MessageUtils.send(player, "<red>El día debe ser un número válido.");
                }
                return true;
            }
        }

        MessageUtils.send(player, "<yellow>Comando desconocido. Usa: /hm day");
        return true;
    }
}