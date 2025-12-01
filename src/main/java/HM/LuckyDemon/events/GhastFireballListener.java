package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class GhastFireballListener implements Listener {

    private final Random random = new Random();
    private final NamespacedKey DEMONIC_GHAST_KEY = new NamespacedKey(HMPlugin.getInstance(), "demonic_ghast");

    @EventHandler
    public void onFireballLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        if (!(e.getEntity().getShooter() instanceof Ghast)) return;

        Fireball fireball = (Fireball) e.getEntity();
        Ghast ghast = (Ghast) fireball.getShooter();

        // Verificar si es un Ghast Demoníaco
        if (ghast.getPersistentDataContainer().has(DEMONIC_GHAST_KEY, PersistentDataType.BYTE)) {
            // Explosion power aleatorio entre 3, 4 y 5
            int explosionPower = 3 + random.nextInt(3); // 3, 4 o 5
            fireball.setYield(explosionPower);
        }
    }
}
