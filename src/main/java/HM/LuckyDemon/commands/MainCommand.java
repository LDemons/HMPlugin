package HM.LuckyDemon.commands;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.items.HMItems;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "<red>Solo jugadores pueden usar esto.");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.send(player, "<gradient:gold:yellow><bold>═══ COMANDOS HM ═══</gradient>");
            MessageUtils.send(player, "<yellow>/hm day <white>- Muestra el día actual");
            MessageUtils.send(player, "<yellow>/hm day <numero> <white>- Cambia el día <gray>(Solo OP)");
            MessageUtils.send(player, "<yellow>/hm items <white>- Entrega ítems especiales <gray>(Solo OP)");
            MessageUtils.send(player, "<yellow>/hm awake <white>- Muestra tiempo sin dormir y riesgo de Phantoms");
            MessageUtils.send(player,
                    "<yellow>/hm mensaje <1|2> <texto> <white>- Establece mensaje de muerte por vida");
            MessageUtils.send(player, "<yellow>/hm lives <white>- Muestra tus vidas restantes");
            MessageUtils.send(player,
                    "<yellow>/hm resetlives <jugador> <white>- Restablece vidas de un jugador <gray>(Solo OP)");
            MessageUtils.send(player,
                    "<yellow>/hm addlife <jugador> <white>- Añade una vida a un jugador <gray>(Solo OP)");
            MessageUtils.send(player,
                    "<yellow>/hm reducestorm <horas> <white>- Reduce la duración de la tormenta <gray>(Solo OP)");
            return true;
        }

        if (args[0].equalsIgnoreCase("items")) {
            if (!player.isOp()) {
                MessageUtils.send(player, "<red>No tienes permisos.");
                return true;
            }

            player.getInventory().addItem(HMItems.createLifeOrb());
            player.getInventory().addItem(HMItems.createEndRelic());
            player.getInventory().addItem(HMItems.craftInfernalHelmet());
            player.getInventory().addItem(HMItems.craftInfernalChestplate());

            MessageUtils.send(player, "<green>¡Ítems entregados! Revisa tu inventario.");
            return true;
        }

        if (args[0].equalsIgnoreCase("day")) {
            if (args.length == 1) {
                GameManager.getInstance().showInfo(player);
                return true;
            }

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

        if (args[0].equalsIgnoreCase("awake")) {
            int timeSinceRest = player.getStatistic(org.bukkit.Statistic.TIME_SINCE_REST);

            long totalSeconds = timeSinceRest / 20;
            long minutes = totalSeconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            double minecraftDays = timeSinceRest / 24000.0;

            String dangerLevel;
            String dangerColor;
            if (timeSinceRest >= 72000) {
                dangerLevel = "¡ALTO RIESGO DE PHANTOMS!";
                dangerColor = "<red><bold>";
            } else if (timeSinceRest >= 48000) {
                dangerLevel = "Riesgo moderado de Phantoms";
                dangerColor = "<yellow>";
            } else if (timeSinceRest >= 24000) {
                dangerLevel = "Riesgo bajo de Phantoms";
                dangerColor = "<gold>";
            } else {
                dangerLevel = "Sin riesgo de Phantoms";
                dangerColor = "<green>";
            }

            hours = hours % 24;
            minutes = minutes % 60;
            long seconds = totalSeconds % 60;

            MessageUtils.send(player, "<gradient:aqua:blue><bold>TIEMPO DESPIERTO</gradient> <gray>» <white>"
                    + String.format("%.1f", minecraftDays) + " días MC");
            MessageUtils.send(player, dangerColor + dangerLevel);

            if (days > 0) {
                MessageUtils.send(player, "<gray>Tiempo real: <yellow>"
                        + String.format("%dd %dh %dm %ds", days, hours, minutes, seconds));
            } else if (hours > 0) {
                MessageUtils.send(player,
                        "<gray>Tiempo real: <yellow>" + String.format("%dh %dm %ds", hours, minutes, seconds));
            } else if (minutes > 0) {
                MessageUtils.send(player, "<gray>Tiempo real: <yellow>" + String.format("%dm %ds", minutes, seconds));
            } else {
                MessageUtils.send(player, "<gray>Tiempo real: <yellow>" + seconds + "s");
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("mensaje")) {
            // Mostrar los mensajes actuales
            if (args.length < 2) {
                String msg1 = HMPlugin.getInstance().getConfig()
                        .getString("death_messages." + player.getUniqueId().toString() + ".life1", null);
                String msg2 = HMPlugin.getInstance().getConfig()
                        .getString("death_messages." + player.getUniqueId().toString() + ".life2", null);

                MessageUtils.send(player, "<gradient:gold:yellow><bold>═══ TUS MENSAJES DE MUERTE ═══</gradient>");
                MessageUtils.send(player, "<red>❤❤<dark_gray>❤ <gray>(Primera vida perdida):");
                if (msg1 != null && !msg1.isEmpty()) {
                    MessageUtils.send(player, "  <white>" + msg1);
                } else {
                    MessageUtils.send(player, "  <gray><italic>No configurado");
                }

                MessageUtils.send(player, "<red>❤<dark_gray>❤❤ <gray>(Segunda vida perdida):");
                if (msg2 != null && !msg2.isEmpty()) {
                    MessageUtils.send(player, "  <white>" + msg2);
                } else {
                    MessageUtils.send(player, "  <gray><italic>No configurado");
                }

                MessageUtils.send(player, "<dark_gray>❤❤❤ <gray>(Tercera vida perdida):");
                MessageUtils.send(player, "  <dark_red><italic>Este es tu final... no hay vuelta atrás.");
                MessageUtils.send(player, "");
                MessageUtils.send(player, "<yellow>Usa: /hm mensaje <1|2> <texto> para configurar");
                return true;
            }

            // Configurar mensaje específico
            if (args.length < 3) {
                MessageUtils.send(player, "<red>Usa: /hm mensaje <1|2> <texto>");
                MessageUtils.send(player, "<gray>Ejemplo: /hm mensaje 1 Mi primer error...");
                return true;
            }

            int lifeNumber;
            try {
                lifeNumber = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                MessageUtils.send(player, "<red>El número de vida debe ser 1 o 2");
                return true;
            }

            if (lifeNumber != 1 && lifeNumber != 2) {
                MessageUtils.send(player, "<red>Solo puedes configurar mensajes para la vida 1 o 2");
                MessageUtils.send(player, "<gray>La tercera vida tiene un mensaje predeterminado");
                return true;
            }

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                messageBuilder.append(args[i]);
                if (i < args.length - 1) {
                    messageBuilder.append(" ");
                }
            }
            String customMessage = messageBuilder.toString();

            if (customMessage.length() > 100) {
                MessageUtils.send(player, "<red>El mensaje es demasiado largo (máximo 100 caracteres).");
                return true;
            }

            String configPath = "death_messages." + player.getUniqueId().toString() + ".life" + lifeNumber;
            HMPlugin.getInstance().getConfig().set(configPath, customMessage);
            HMPlugin.getInstance().saveConfig();

            MessageUtils.send(player, "<green>¡Mensaje de vida " + lifeNumber + " guardado!");
            MessageUtils.send(player, "<yellow>Vista previa: <white>" + customMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("lives")) {
            HM.LuckyDemon.managers.LivesManager livesManager = HM.LuckyDemon.managers.LivesManager.getInstance();
            int lives = livesManager.getLives(player);
            int maxLives = livesManager.getMaxLives();

            String livesBar = "";
            for (int i = 0; i < maxLives; i++) {
                if (i < lives) {
                    livesBar += "<red>❤";
                } else {
                    livesBar += "<gray>❤";
                }
            }

            MessageUtils.send(player, "<gradient:red:gold><bold>VIDAS</gradient> <gray>» " + livesBar + " <yellow>"
                    + lives + "/" + maxLives);
            return true;
        }

        if (args[0].equalsIgnoreCase("resetlives")) {
            if (!player.isOp()) {
                MessageUtils.send(player, "<red>No tienes permisos.");
                return true;
            }

            if (args.length < 2) {
                MessageUtils.send(player, "<yellow>Usa: /hm resetlives <jugador>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtils.send(player, "<red>Jugador no encontrado.");
                return true;
            }

            HM.LuckyDemon.managers.LivesManager livesManager = HM.LuckyDemon.managers.LivesManager.getInstance();
            livesManager.resetLives(target);

            MessageUtils.send(player,
                    "<green>Vidas de " + target.getName() + " restablecidas a " + livesManager.getMaxLives());
            MessageUtils.send(target, "<green>¡Tus vidas han sido restablecidas!");
            return true;
        }

        if (args[0].equalsIgnoreCase("addlife")) {
            if (!player.isOp()) {
                MessageUtils.send(player, "<red>No tienes permisos.");
                return true;
            }

            if (args.length < 2) {
                MessageUtils.send(player, "<yellow>Usa: /hm addlife <jugador>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtils.send(player, "<red>Jugador no encontrado.");
                return true;
            }

            HM.LuckyDemon.managers.LivesManager livesManager = HM.LuckyDemon.managers.LivesManager.getInstance();
            int newLives = livesManager.addLife(target);

            MessageUtils.send(player, "<green>Vida añadida a " + target.getName() + ". Ahora tiene " + newLives + "/"
                    + livesManager.getMaxLives() + " vidas");
            MessageUtils.send(target, "<green>¡Te han dado una vida extra! Ahora tienes " + newLives + " vidas.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reducestorm")) {
            if (!player.isOp()) {
                MessageUtils.send(player, "<red>No tienes permisos.");
                return true;
            }

            if (args.length < 2) {
                MessageUtils.send(player, "<yellow>Usa: /hm reducestorm <horas>");
                return true;
            }

            try {
                double stormHours = Double.parseDouble(args[1]);
                int ticksToReduce = (int) (stormHours * 3600 * 20);

                if (player.getWorld().hasStorm()) {
                    int currentDuration = player.getWorld().getWeatherDuration();
                    int newDuration = Math.max(0, currentDuration - ticksToReduce);
                    player.getWorld().setWeatherDuration(newDuration);

                    double remainingHours = newDuration / 20.0 / 3600.0;
                    MessageUtils.send(player, "<green>Tormenta reducida en " + stormHours + " horas. Tiempo restante: "
                            + String.format("%.1f", remainingHours) + " horas");
                    Bukkit.broadcast(MessageUtils
                            .format("<blue>⛈ La tormenta ha sido reducida en <bold>" + stormHours + " horas"));
                } else {
                    MessageUtils.send(player, "<red>No hay ninguna tormenta activa.");
                }
            } catch (NumberFormatException e) {
                MessageUtils.send(player, "<red>Debes especificar un número válido de horas.");
            }
            return true;
        }

        MessageUtils.send(player, "<yellow>Comando desconocido. Usa: /hm para ver la lista de comandos.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Sugerir subcomandos principales
            List<String> subcommands = Arrays.asList("day", "items", "awake", "mensaje", "lives", "resetlives",
                    "addlife", "reducestorm");
            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            // Comandos que requieren nombre de jugador
            if (subcommand.equals("resetlives") || subcommand.equals("addlife")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            // Comando mensaje - sugerir números de vida
            if (subcommand.equals("mensaje")) {
                return Arrays.asList("1", "2").stream()
                        .filter(num -> num.startsWith(args[1]))
                        .collect(Collectors.toList());
            }

            // Comando day - sugerir algunos días comunes
            if (subcommand.equals("day") && sender.isOp()) {
                return Arrays.asList("1", "2", "3", "5", "10", "15", "20", "30").stream()
                        .filter(day -> day.startsWith(args[1]))
                        .collect(Collectors.toList());
            }

            // Comando reducestorm - sugerir horas comunes
            if (subcommand.equals("reducestorm") && sender.isOp()) {
                return Arrays.asList("1", "2", "3", "5", "10", "24").stream()
                        .filter(hour -> hour.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            // Comando mensaje - sugerir texto de ejemplo
            if (subcommand.equals("mensaje")) {
                return Arrays.asList("<texto>", "Mi", "mensaje", "personalizado");
            }
        }

        return completions;
    }
}