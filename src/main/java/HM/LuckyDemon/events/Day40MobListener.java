package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * DÍA 40: pociones de las Brujas Imposibles, bolas de los Demonios Flotantes, carga del rayo de los guardianes y drops.
 */
public class Day40MobListener implements Listener {

    private final Random random = new Random();
    private final NamespacedKey floatingDemonKey = new NamespacedKey(HMPlugin.getInstance(), "floating_demon");

    public Day40MobListener() {
        // No hay evento cuando un Copper Golem abre un cofre: se mira su estado cada 5 ticks
        org.bukkit.Bukkit.getScheduler().runTaskTimer(HMPlugin.getInstance(), this::checkCopperGolems, 20L, 5L);
    }

    /** Un Copper Golem sacando items está abriendo un cofre de cobre (solo saca de esos): ese cofre explota. */
    private void checkCopperGolems() {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            for (org.bukkit.entity.CopperGolem golem : world.getEntitiesByClass(org.bukkit.entity.CopperGolem.class)) {
                org.bukkit.entity.CopperGolem.State state = golem.getGolemState();
                if (state != org.bukkit.entity.CopperGolem.State.GETTING_ITEM
                        && state != org.bukkit.entity.CopperGolem.State.GETTING_NO_ITEM) {
                    continue;
                }
                org.bukkit.block.Block chest = findCopperChest(golem.getLocation().getBlock());
                if (chest != null) {
                    explodeCopperChest(chest, golem);
                }
            }
        }
    }

    /** Cofre de cobre a 2 bloques o menos (el golem lo abre desde al lado). */
    private static org.bukkit.block.Block findCopperChest(org.bukkit.block.Block center) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    org.bukkit.block.Block b = center.getRelative(x, y, z);
                    if (org.bukkit.Tag.COPPER_CHESTS.isTagged(b.getType())) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    private static void explodeCopperChest(org.bukkit.block.Block chest, org.bukkit.entity.Entity source) {
        chest.getWorld().createExplosion(chest.getLocation().add(0.5, 0.5, 0.5), COPPER_CHEST_POWER, false, true,
                source);
    }

    /** Brujas Imposibles: tiran Daño IV, Veneno III 5 min o Lentitud V 20 s. */
    @EventHandler(ignoreCancelled = true)
    public void onWitchThrow(ProjectileLaunchEvent e) {
        if (GameManager.getInstance().getDay() < 40 || !(e.getEntity() instanceof ThrownPotion potion)
                || !(potion.getShooter() instanceof Witch)) {
            return;
        }
        PotionEffect effect = switch (random.nextInt(3)) {
            case 0 -> new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 3);
            case 1 -> new PotionEffect(PotionEffectType.POISON, 20 * 60 * 5, 2);
            default -> new PotionEffect(PotionEffectType.SLOWNESS, 20 * 20, 4);
        };
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        item.editMeta(PotionMeta.class, meta -> meta.addCustomEffect(effect, true));
        potion.setItem(item);
    }

    /** x4 de daño al pisar bloques de magma y al tocar un Sulfur Cube (que desde el día 40 lleva magma dentro). */
    @EventHandler(ignoreCancelled = true)
    public void onMagmaDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }
        boolean magmaBlock = e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.HOT_FLOOR
                && !(e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent);
        boolean sulfurCube = e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof org.bukkit.entity.SulfurCube;
        if (magmaBlock || sulfurCube) {
            e.setDamage(e.getDamage() * 4);
        }
    }

    /**
     * Ranas: al comerse un Slime sueltan 5 Super Golden Apple +; al comerse un Magma Cube, 5 manzanas de Notch.
     * Armadillo Acorazado: al morir suelta un stack de Blaze Rod o uno de bloques de Lapis (50/50).
     */
    // HIGHEST: corre después de PlayerListener.onEntityDeath, que desde el día 20 borra los drops de Slime y Magma Cube
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onDay40Drops(org.bukkit.event.entity.EntityDeathEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }
        if (e.getEntity() instanceof org.bukkit.entity.Slime slime
                && e.getDamageSource().getCausingEntity() instanceof org.bukkit.entity.Frog) {
            ItemStack apples = slime instanceof org.bukkit.entity.MagmaCube
                    ? new ItemStack(Material.ENCHANTED_GOLDEN_APPLE)
                    : HM.LuckyDemon.items.HMItems.createSuperGoldenApple();
            apples.setAmount(5);
            e.getDrops().add(apples);
        }
        if (e.getEntity().getPersistentDataContainer().has(
                new NamespacedKey(HMPlugin.getInstance(), "rich_armadillo"), PersistentDataType.BYTE)) {
            e.getDrops().add(new ItemStack(random.nextBoolean() ? Material.BLAZE_ROD : Material.LAPIS_BLOCK, 64));
        }
        // Ultra Ravager, Carlos y Jess (DifficultyManager.spawnUltraRavager)
        String boss = e.getEntity().getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (boss != null) {
            e.getDrops().add(switch (boss) {
                case "ultra_ravager" -> new ItemStack(Material.TOTEM_OF_UNDYING);
                case "carlos" -> new ItemStack(Material.GOLD_INGOT, 32);
                default -> new ItemStack(Material.GOLDEN_APPLE, 2);
            });
        }
    }

    private final NamespacedKey bossKey = new NamespacedKey(HMPlugin.getInstance(), "day40_boss");

    private final java.util.Set<java.util.UUID> chargingGuardians = new java.util.HashSet<>();

    /**
     * Guardianes (y ancianos): el rayo carga el doble de rápido. Mientras tienen objetivo, cada tick se le suma un
     * tick más a la carga; solo corre para los guardianes que están atacando.
     */
    @EventHandler(ignoreCancelled = true)
    public void onGuardianTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.Guardian guardian) || e.getTarget() == null
                || GameManager.getInstance().getDay() < 40 || !chargingGuardians.add(guardian.getUniqueId())) {
            return;
        }
        org.bukkit.Bukkit.getScheduler().runTaskTimer(HMPlugin.getInstance(), task -> {
            if (!guardian.isValid() || guardian.getTarget() == null) {
                chargingGuardians.remove(guardian.getUniqueId());
                task.cancel();
            } else if (guardian.hasLaser() && guardian.getLaserTicks() < guardian.getLaserDuration()) {
                guardian.setLaserTicks(guardian.getLaserTicks() + 1);
            }
        }, 1L, 1L);
    }

    // Potencia 10 (una TNT es 4, un creeper eléctrico 6); Nacho la pidió mucho más fuerte que 4
    private static final float COPPER_CHEST_POWER = 10f;

    /** Los cofres de cobre (cualquier oxidación, encerados también) explotan al abrirlos. */
    @EventHandler(ignoreCancelled = true)
    public void onCopperChestOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        org.bukkit.Location loc = e.getInventory().getLocation();
        if (GameManager.getInstance().getDay() < 40 || loc == null
                || !org.bukkit.Tag.COPPER_CHESTS.isTagged(loc.getBlock().getType())) {
            return;
        }
        e.setCancelled(true);
        explodeCopperChest(loc.getBlock(), e.getPlayer());
    }

    /** Demonios Flotantes: la bola no explota ni hace daño; al que golpea le da Levitación 50 y Wither V. */
    @EventHandler(ignoreCancelled = true)
    public void onFloatingDemonFireball(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Fireball fireball) || !(fireball.getShooter() instanceof Ghast ghast)
                || !ghast.getPersistentDataContainer().has(floatingDemonKey, PersistentDataType.BYTE)) {
            return;
        }
        if (e.getHitEntity() instanceof LivingEntity hit) {
            hit.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 5, 49));
            hit.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 20, 4));
        }
        e.setCancelled(true);
        fireball.remove();
    }
}
