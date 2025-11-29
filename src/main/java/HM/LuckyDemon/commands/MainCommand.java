package HM.LuckyDemon.commands;

import HM.LuckyDemon.HMPluggin;
import HM.LuckyDemon.items.HMItems;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MainCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "<red>Solo jugadores pueden usar esto.");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.send(player,
                    "<yellow>Usa: /hm day [numero], /hm items, /hm awake, /hm mensaje <texto>, /hm lives");
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
            if (args.length < 2) {
                String currentMessage = HMPluggin.getInstance().getConfig()
                        .getString("death_messages." + player.getUniqueId().toString(), null);
                if (currentMessage != null) {
                    MessageUtils.send(player, "<green>Tu mensaje de muerte actual: <white>" + currentMessage);
                } else {
                    MessageUtils.send(player,
                            "<yellow>No tienes un mensaje de muerte personalizado. Usa: /hm mensaje <texto>");
                }
                return true;
            }

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
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

            HMPluggin.getInstance().getConfig().set("death_messages." + player.getUniqueId().toString(), customMessage);
            HMPluggin.getInstance().saveConfig();

            MessageUtils.send(player,
                    "<green>¡Mensaje de muerte personalizado guardado! <gray>Se mostrará cuando mueras.");
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
                    livesBar += "<dark_gray>❤";
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
                double hours = Double.parseDouble(args[1]);
                int ticksToReduce = (int) (hours * 3600 * 20);

                if (player.getWorld().hasStorm()) {
                    int currentDuration = player.getWorld().getWeatherDuration();
                    int newDuration = Math.max(0, currentDuration - ticksToReduce);
                    player.getWorld().setWeatherDuration(newDuration);

                    double remainingHours = newDuration / 20.0 / 3600.0;
                    MessageUtils.send(player, "<green>Tormenta reducida en " + hours + " horas. Tiempo restante: "
                            + String.format("%.1f", remainingHours) + " horas");
                    Bukkit.broadcast(
                            MessageUtils.format("<blue>⛈ La tormenta ha sido reducida en <bold>" + hours + " horas"));
                } else {
                    MessageUtils.send(player, "<red>No hay ninguna tormenta activa.");
                }
            } catch (NumberFormatException e) {
                MessageUtils.send(player, "<red>Debes especificar un número válido de horas.");
            }
            return true;
        }

        MessageUtils.send(player,
                "<yellow>Comando desconocido. Usa: /hm day, /hm items, /hm awake, /hm mensaje, /hm lives");
        return true;
    }
}