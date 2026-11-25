package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Mecánicas del día 50 que no son de mobs. */
public class Day50Listener implements Listener {

    private static boolean day50() {
        return GameManager.getInstance().getDay() >= 50;
    }

    /**
     * Romper un bloque quita medio corazón (daño normal, como el original), salvo con una herramienta de netherite:
     * la netherite está baneada desde el día 1 y solo se consigue en el cofre de herramientas de The Beginning.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (day50() && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                && !p.getInventory().getItemInMainHand().getType().name().startsWith("NETHERITE_")) {
            p.damage(1.0);
        }
    }

    /** Hornos y altos hornos dan pepitas en vez de lingotes de hierro y oro. */
    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent e) {
        if (!day50()) return;
        ItemStack result = e.getResult();
        if (result.getType() == Material.IRON_INGOT) {
            e.setResult(result.withType(Material.IRON_NUGGET));
        } else if (result.getType() == Material.GOLD_INGOT) {
            e.setResult(result.withType(Material.GOLD_NUGGET));
        }
    }

    /**
     * El aire se acaba 5 veces más rápido. Solo mientras quedan burbujas: bajo 0 el juego hace daño cuando el aire
     * vale justo -20, y saltar ese valor dejaría de ahogar.
     */
    @EventHandler(ignoreCancelled = true)
    public void onAirChange(EntityAirChangeEvent e) {
        if (!day50() || !(e.getEntity() instanceof Player p)) return;
        int current = p.getRemainingAir();
        int lost = current - e.getAmount();
        if (lost > 0 && e.getAmount() > 0) {
            e.setAmount(Math.max(0, current - lost * 5));
        }
    }

    /** Gato Galáctico: al morir invoca un mob al azar (DifficultyManager.galacticCatDeath). */
    @EventHandler
    public void onGalacticCatDeath(EntityDeathEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(new NamespacedKey(HMPlugin.getInstance(), "galactic_cat"))) {
            HMPlugin.getInstance().getDifficultyManager().galacticCatDeath(e.getEntity().getLocation());
        }
    }

    /** No se puede recoger lava ni agua con un cubo (los llenos que ya tengas siguen sirviendo). */
    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        Material result = e.getItemStack() == null ? null : e.getItemStack().getType();
        if (day50() && (result == Material.WATER_BUCKET || result == Material.LAVA_BUCKET)) {
            e.setCancelled(true);
        }
    }

    /** Ganar una raid da Hero of the Village solo 5 minutos (vanilla, 40). Se cambia al tick siguiente. */
    @EventHandler
    public void onRaidFinish(RaidFinishEvent e) {
        if (!day50()) return;
        Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
            for (Player p : e.getWinners()) {
                PotionEffect hero = p.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
                if (hero != null) {
                    p.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE); // si no, el más largo gana
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 20 * 60 * 5,
                            hero.getAmplifier()));
                }
            }
        }, 1L);
    }
}
