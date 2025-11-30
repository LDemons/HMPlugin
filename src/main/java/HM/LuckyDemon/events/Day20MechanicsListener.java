package HM.LuckyDemon.events;

import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Listener para mecánicas especiales del día 20+
 */
public class Day20MechanicsListener implements Listener {

    private final Random random = new Random();

    /**
     * DÍA 20: Murciélagos dan ceguera al golpear jugadores
     */
    @EventHandler
    public void onBatAttack(EntityDamageByEntityEvent e) {
        if (GameManager.getInstance().getDay() < 20)
            return;

        if (e.getDamager() instanceof Bat && e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 15, 0)); // 15 segundos
        }
    }

    /**
     * DÍA 10: 3% chance de araña de cueva al romper vasijas
     */
    @EventHandler
    public void onVaseBreak(BlockBreakEvent e) {
        if (GameManager.getInstance().getDay() < 10)
            return;

        if (e.getBlock().getType() == Material.DECORATED_POT) {
            if (random.nextInt(100) < 3) { // 3%
                Location loc = e.getBlock().getLocation().add(0.5, 0, 0.5);
                loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
                // MessageUtils.send(e.getPlayer(), "<red>🕷 ¡Una araña de cueva ha salido de la
                // vasija!");
            }
        }
    }

    /**
     * DÍA 20: Bad Omen también da Weakness
     */
    @EventHandler
    public void onBadOmen(EntityPotionEffectEvent e) {
        if (GameManager.getInstance().getDay() < 20)
            return;

        if (!(e.getEntity() instanceof Player))
            return;
        if (e.getNewEffect() == null)
            return;
        if (e.getNewEffect().getType() != PotionEffectType.BAD_OMEN)
            return;

        Player player = (Player) e.getEntity();
        int duration = e.getNewEffect().getDuration();

        // Aplicar Weakness I con la misma duración que Bad Omen
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0));
    }

    /**
     * DÍA 20: 50% chance de Bad Omen nivel 5 en muerte de Pillagers/Raiders
     */
    @EventHandler
    public void onRaiderDeath(EntityDeathEvent e) {
        if (GameManager.getInstance().getDay() < 20)
            return;

        // Verificar si es un raider
        boolean isRaider = e.getEntity() instanceof org.bukkit.entity.Pillager ||
                e.getEntity() instanceof org.bukkit.entity.Vindicator ||
                e.getEntity() instanceof org.bukkit.entity.Evoker ||
                e.getEntity() instanceof org.bukkit.entity.Illusioner;

        if (!isRaider)
            return;

        Player killer = e.getEntity().getKiller();
        if (killer == null)
            return;

        // 50% chance de Bad Omen nivel 5
        if (random.nextBoolean()) {
            killer.addPotionEffect(new PotionEffect(
                    PotionEffectType.BAD_OMEN,
                    20 * 60 * 100, // 100 minutos
                    4)); // Nivel 5 (amplifier = 4)
        }
    }
}
