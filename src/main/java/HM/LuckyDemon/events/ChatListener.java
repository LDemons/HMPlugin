package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChatListener - Maneja menciones de jugadores en el chat
 */
public class ChatListener implements Listener {
    private final HMPlugin plugin;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public ChatListener(HMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        
        Matcher matcher = MENTION_PATTERN.matcher(message);
        StringBuffer formattedMessage = new StringBuffer();
        
        while (matcher.find()) {
            String mentionedName = matcher.group(1);
            
            // Verificar si es @everyone
            if (mentionedName.equalsIgnoreCase("everyone")) {
                // Formatear @everyone con color
                String replacement = ChatColor.RED + "@everyone" + ChatColor.RESET;
                matcher.appendReplacement(formattedMessage, Matcher.quoteReplacement(replacement));
                
                // Reproducir sonido a todos los jugadores excepto el que envió el mensaje
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(sender)) {
                            onlinePlayer.playSound(
                                onlinePlayer.getLocation(),
                                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                                1.0f,
                                1.0f
                            );
                        }
                    }
                });
                continue;
            }
            
            // Verificar que el jugador no se mencione a sí mismo
            if (mentionedName.equalsIgnoreCase(sender.getName())) {
                continue;
            }
            
            // Buscar jugador mencionado (solo si está online)
            Player mentionedPlayer = Bukkit.getPlayerExact(mentionedName);
            
            if (mentionedPlayer != null && mentionedPlayer.isOnline()) {
                // Formatear la mención con color
                String replacement = ChatColor.GOLD + "@" + mentionedPlayer.getName() + ChatColor.RESET;
                matcher.appendReplacement(formattedMessage, Matcher.quoteReplacement(replacement));
                
                // Reproducir sonido al jugador mencionado (debe ejecutarse en el thread principal)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    mentionedPlayer.playSound(
                        mentionedPlayer.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        1.0f,
                        1.0f
                    );
                });
            } else {
                // Si el jugador no está online, dejar la mención sin formato
                matcher.appendReplacement(formattedMessage, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        
        matcher.appendTail(formattedMessage);
        event.setMessage(formattedMessage.toString());
    }
}