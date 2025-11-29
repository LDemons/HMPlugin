package HM.LuckyDemon.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtils {

    // Instancia de MiniMessage (el motor de texto moderno)
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Convierte un String con formato MiniMessage a un Componente de Paper.
     * Ejemplo: "<red>Hola <bold>Mundo" -> Texto rojo con Mundo en negrita.
     */
    public static Component format(String text) {
        if (text == null) return Component.empty();
        // Soporte retroactivo básico: Si el usuario usa "&", intentamos reemplazarlo (opcional)
        // Pero lo ideal es usar tags como <red>, <green>, <gradient:red:blue>, etc.
        return MM.deserialize(text);
    }

    /**
     * Formatea una lista de Strings.
     */
    public static List<Component> formatList(List<String> list) {
        return list.stream().map(MessageUtils::format).collect(Collectors.toList());
    }

    /**
     * Envía un mensaje formateado a un jugador o consola.
     */
    public static void send(CommandSender sender, String text) {
        sender.sendMessage(format(text));
    }

    /**
     * Convierte segundos a formato legible (Migrado de TextUtils original)
     */
    public static String formatTime(int secs) {
        if (secs < 0) return "0s";
        int remainder = secs % 86400;
        int days = secs / 86400;
        int hours = remainder / 3600;
        int minutes = (remainder / 60) - (hours * 60);
        int seconds = (remainder % 3600) - (minutes * 60);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}