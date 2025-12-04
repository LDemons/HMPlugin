package HM.LuckyDemon.tasks;

import HM.LuckyDemon.utils.WebhookUtils;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tarea periódica para enviar un "latido" (heartbeat) al bot de Discord, 
 * indicando que el plugin de Minecraft está en línea.
 */
public class HeartbeatTask extends BukkitRunnable {

    @Override
    public void run() {
        // Ejecutar la función de envío de heartbeat
        WebhookUtils.sendHeartbeat();
    }
}