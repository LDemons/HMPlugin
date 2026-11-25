package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.items.HMItems;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Mecánicas del día 60 que no son efectos de mobs al aparecer (esas están en DifficultyManager.applyDay60Effects). */
public class Day60Listener implements Listener {

    /** Hora a la que empieza el contador del Orbe de Vida el día 60 (dura hasta las 00:00, cuando entra el 61). */
    private static final int ORB_START_HOUR = 16;

    private final BossBar orbBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    private boolean orbRunning;

    public Day60Listener() {
        Bukkit.getScheduler().runTaskTimer(HMPlugin.getInstance(), this::tickLifeOrb, 20L, 20L);
    }

    private static int day() {
        return GameManager.getInstance().getDay();
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(HMPlugin.getInstance(), name);
    }

    // ========== ORBE DE VIDA ==========

    /** El contador corre el día 60 desde las 16:00 hasta las 00:00. */
    public static boolean isLifeOrbEventRunning() {
        return day() == 60 && LocalTime.now().getHour() >= ORB_START_HOUR;
    }

    /** Desde el 61, quien no lleve el Orbe encima pierde 8 corazones (PlayerListener.updateHealth). */
    public static boolean lifeOrbPenalty(Player p) {
        return day() >= 61 && !HMItems.carries(p, "life_orb");
    }

    /** Cada segundo: barra con el tiempo que queda y, después del contador, la vida según si llevan el Orbe. */
    private void tickLifeOrb() {
        boolean running = isLifeOrbEventRunning();
        if (running) {
            long left = Duration.between(LocalDateTime.now(), LocalTime.MIDNIGHT.atDate(java.time.LocalDate.now()
                    .plusDays(1))).getSeconds();
            orbBar.name(Component.text(String.format("%02d:%02d:%02d para obtener el Orbe de Vida", left / 3600,
                    left % 3600 / 60, left % 60), NamedTextColor.GOLD, TextDecoration.BOLD));
            orbBar.progress(Math.max(0f, Math.min(1f, left / (float) ((24 - ORB_START_HOUR) * 3600))));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(orbBar);
            }
            if (!orbRunning) {
                Bukkit.broadcast(MessageUtils.format("<gold><bold>¡Empieza el contador del Orbe de Vida!</bold> "
                        + "<yellow>Tienen hasta las 00:00 para craftearlo. Quien no lo lleve encima al terminar "
                        + "perderá <red>8 corazones</red>."));
            }
        } else if (orbRunning) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(orbBar);
            }
            if (day() >= 61) {
                Bukkit.broadcast(MessageUtils.format("<dark_red><bold>Se acabó el tiempo del Orbe de Vida.</bold> "
                        + "<red>Quien no lo lleve encima pierde 8 corazones."));
            }
        }
        orbRunning = running;
        if (day() >= 61) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerListener.updateHealth(p);
            }
        }
    }

    // ========== RELIQUIA DEL COMIENZO Y ORBE DE VIDA: CANTIDADES ==========

    /**
     * Reliquia del Comienzo (día 60+): la Reliquia del Fin al centro y 32 bloques de diamante en cada casilla.
     * Orbe de Vida (solo durante el contador): 64 de cada cosa salvo el corazón del mar.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent e) {
        String recipe = recipeKey(e.getRecipe());
        if (recipe == null) return;
        ItemStack[] matrix = e.getInventory().getMatrix();
        boolean ok = switch (recipe) {
            case "beginning_relic" -> day() >= 60 && matrix.length == 9 && HMItems.isItem(matrix[4], "end_relic")
                    && enough(matrix);
            case "life_orb" -> isLifeOrbEventRunning() && enough(matrix);
            default -> enough(matrix); // hyper_gap desde el 50
        };
        if (!ok) {
            e.getInventory().setResult(null);
        }
    }

    /**
     * El crafteo se hace a mano: se cancela el del juego, se entrega el resultado y se gasta lo que pide cada
     * casilla. Cambiar la matriz dentro del crafteo del juego hacía que recalculara la receta (ya sin los 32) y se
     * llevaba los materiales sin dar nada. Se entrega al tick siguiente: al cancelar el clic, el servidor vuelve a
     * poner el cursor como estaba y borraba lo que se dejara ahí en el mismo evento.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (recipeKey(e.getRecipe()) == null || e.getInventory().getResult() == null) return;
        e.setCancelled(true);
        org.bukkit.inventory.CraftingInventory inv = e.getInventory();
        ItemStack result = inv.getResult().clone();
        org.bukkit.entity.HumanEntity who = e.getWhoClicked();
        boolean toInventory = e.isShiftClick();
        Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> craftByHand(who, inv, result, toInventory));
    }

    private void craftByHand(org.bukkit.entity.HumanEntity who, org.bukkit.inventory.CraftingInventory inv,
            ItemStack result, boolean toInventory) {
        // En este tick pudo cerrar la mesa o sacar materiales: se vuelve a mirar
        if (!result.isSimilar(inv.getResult()) || !enough(inv.getMatrix())) return;
        if (!toInventory && who.getItemOnCursor().getType().isAir()) {
            who.setItemOnCursor(result);
        } else {
            who.getInventory().addItem(result).values()
                    .forEach(left -> who.getWorld().dropItemNaturally(who.getLocation(), left));
            if (who instanceof Player p) {
                Day40InventoryListener.updateLockedSlots(p); // la Reliquia del Comienzo libera los slots al momento
            }
        }
        ItemStack[] matrix = inv.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null) continue;
            int left = item.getAmount() - cost(item);
            if (left > 0) {
                item.setAmount(left);
            } else {
                matrix[i] = null;
            }
        }
        inv.setMatrix(matrix);
    }

    private static String recipeKey(org.bukkit.inventory.Recipe recipe) {
        if (recipe instanceof org.bukkit.Keyed keyed) {
            String key = keyed.getKey().getKey();
            if (key.equals("beginning_relic") || key.equals("life_orb")) return key;
            // DÍA 50: la Hyper Golden Apple + pide 8 bloques de oro por casilla (antes, 1)
            if (key.equals("hyper_gap") && GameManager.getInstance().getDay() >= 50) return key;
        }
        return null;
    }

    /** Cuánto pide cada casilla: 32 bloques de diamante, 8 de oro, 64 de lo demás del Orbe, 1 el resto. */
    private static int cost(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        return switch (item.getType()) {
            case DIAMOND_BLOCK -> 32;
            case GOLD_BLOCK -> 8;
            case HEART_OF_THE_SEA, SHULKER_SHELL, LIGHT_BLUE_DYE, GOLDEN_APPLE -> 1;
            default -> 64;
        };
    }

    private static boolean enough(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item != null && item.getAmount() < cost(item)) return false;
        }
        return true;
    }

    // ========== MOBS ==========

    /** Ultra Esqueleto Demoníaco: sus flechas explotan (como el original: potencia 3 con fuego). */
    @EventHandler
    public void onDemonArrow(ProjectileHitEvent e) {
        if (!(e.getEntity().getShooter() instanceof LivingEntity shooter)
                || !shooter.getPersistentDataContainer().has(key("demon_skeleton"))) {
            return;
        }
        Projectile arrow = e.getEntity();
        arrow.getWorld().createExplosion(arrow.getLocation(), 3f, true, true, shooter);
        arrow.remove();
    }

    /** Las ballestas no aceptan Flame: si un mob dispara con una que lo tenga, la flecha sale ardiendo igual. */
    @EventHandler(ignoreCancelled = true)
    public void onMobShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getBow() != null
                && e.getBow().containsEnchantment(org.bukkit.enchantments.Enchantment.FLAME)) {
            e.getProjectile().setFireTicks(2000);
        }
        // Ultra Esqueleto Definitivo: daño de Power 32765 (el juego: 2 base + 0,5 por nivel + 0,5)
        if (e.getEntity().getPersistentDataContainer().has(key("definitive_skeleton"))
                && e.getProjectile() instanceof org.bukkit.entity.AbstractArrow arrow) {
            arrow.setDamage(2.0 + 0.5 * DEFINITIVE_POWER + 0.5);
        }
    }

    private static final int DEFINITIVE_POWER = 32765;

    /** Los Drowned hacen el triple de daño (golpe y tridente). */
    @EventHandler(ignoreCancelled = true)
    public void onDrownedHit(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        if (day() >= 60 && damager instanceof Drowned) {
            e.setDamage(e.getDamage() * 3);
        }
    }

    /** Los aldeanos que ya existían (también los de jugadores) pasan a Vindicator al cargarse. No Jess. */
    @EventHandler
    public void onVillagerLoad(EntitiesLoadEvent e) {
        if (day() < 60) return;
        for (Entity entity : e.getEntities()) {
            if (entity.getType() != org.bukkit.entity.EntityType.VILLAGER
                    || entity.getPersistentDataContainer().has(key("day40_boss"))) {
                continue;
            }
            org.bukkit.Location loc = entity.getLocation();
            entity.remove();
            HMPlugin.getInstance().getDifficultyManager().applyMobEffects((LivingEntity)
                    loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.VINDICATOR));
        }
    }

    // ========== JUGADOR ==========

    /** Las perlas de ender tardan el doble en recargar (vanilla: 1 s). */
    @EventHandler(ignoreCancelled = true)
    public void onPearl(ProjectileLaunchEvent e) {
        if (day() >= 60 && e.getEntity() instanceof EnderPearl && e.getEntity().getShooter() instanceof Player p) {
            Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> p.setCooldown(Material.ENDER_PEARL, 40));
        }
    }
}
