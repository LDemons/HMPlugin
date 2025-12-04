package HM.LuckyDemon.commands;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ReloadCommand implements CommandExecutor {

    private final HMPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ReloadCommand(HMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permisos
        if (!sender.hasPermission("hmplugin.reload")) {
            String prefix = plugin.getConfig().getString("prefix", "<gradient:red:dark_red>PermaDeath</gradient> <gray>» ");
            String noPermissionMsg = prefix + "<red>No tienes permiso para ejecutar este comando.";
            sender.sendMessage(miniMessage.deserialize(noPermissionMsg));
            return true;
        }

        try {
            // Recargar configuración
            plugin.reloadConfig();
            
            String prefix = plugin.getConfig().getString("prefix", "<gradient:red:dark_red>PermaDeath</gradient> <gray>» ");
            String successMsg = prefix + "<green>¡Configuración recargada exitosamente!";
            sender.sendMessage(miniMessage.deserialize(successMsg));
            
            // Log en consola
            plugin.getLogger().info("Configuración recargada por " + sender.getName());
            
        } catch (Exception e) {
            String prefix = plugin.getConfig().getString("prefix", "<gradient:red:dark_red>PermaDeath</gradient> <gray>» ");
            String errorMsg = prefix + "<red>Error al recargar la configuración. Revisa la consola para más detalles.";
            sender.sendMessage(miniMessage.deserialize(errorMsg));
            
            // Log detallado en consola
            plugin.getLogger().severe("Error al recargar la configuración:");
            e.printStackTrace();
        }
        
        return true;
    }
}
