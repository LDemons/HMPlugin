package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.DifficultyManager;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * DÍA 30: el dragón del End (el que ya esté o el que se respawnee) es el Permadeath Demon.
 * Recreado del Permadeath original (EndManager, EndTask y MovesTask) con sus mismos tiempos y ataques.
 */
public class PermadeathDemonListener implements Listener {

    private static final double HEALTH = 2000;
    private static final double ENRAGED_HEALTH = 600;
    // El doble que el original (Nacho los pidio mas lentos)
    private static final int[] CRYSTAL_REGEN_SECONDS = {60, 120, 180, 240, 300, 480};
    // Rayo al azar cerca del portal: 1 de cada N segundos (el original, 1 por segundo; eran demasiados)
    private static final int RANDOM_LIGHTNING_EVERY = 5;
    private static final float TNT_POWER = 15f;
    // Torres del End: centros a 42 bloques de (0,0), radio 2-5, altura máxima 103
    private static final int PILLAR_RING_MIN = 36;
    private static final int PILLAR_RING_MAX = 48;
    private static final int PILLAR_TOP = 110;
    // shortcut: el original lanzaba todo el end stone de la explosión (miles de entidades); aquí como mucho 20
    private static final int MAX_FLYING_BLOCKS = 20;
    private static final int SPIN_STEPS = 80; // 20 s en pasos de 5 ticks: 5 s quieto en el portal y 15 s girando

    private final HMPlugin plugin = HMPlugin.getInstance();
    private final NamespacedKey demonKey = new NamespacedKey(plugin, "permadeath_demon");
    private final NamespacedKey tntKey = new NamespacedKey(plugin, "demon_tnt");
    private final NamespacedKey smokeKey = new NamespacedKey(plugin, "demon_smoke");
    private final NamespacedKey healCloudKey = new NamespacedKey(plugin, "demon_heal_cloud");
    private final Random random = new Random();

    // Estado del demonio actual: se reinicia cuando aparece otro dragón
    private UUID dragonId;
    private int steps, tntTimer, attackTimer, lightningRain, nightVision, spin;
    private float spinYaw;
    private boolean enraged, decided;
    private final Map<Location, Integer> crystalRegen = new HashMap<>();

    public PermadeathDemonListener() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L);
    }

    private boolean isDemon(LivingEntity e) {
        return e instanceof EnderDragon && e.getPersistentDataContainer().has(demonKey, PersistentDataType.BYTE);
    }

    private void tick() {
        if (GameManager.getInstance().getDay() < 30) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            DragonBattle battle = world.getEnderDragonBattle();
            EnderDragon dragon = battle == null ? null : battle.getEnderDragon();
            if (dragon == null || dragon.isDead()) {
                continue;
            }
            if (!dragon.getUniqueId().equals(dragonId)) {
                start(dragon);
            }
            if (spin > 0) {
                spinStep(dragon);
            }
            if (steps++ % 4 == 0) {
                second(dragon);
            }
        }
    }

    /** Convierte al dragón (solo la primera vez: la marca queda guardada) y reinicia los tiempos. */
    private void start(EnderDragon dragon) {
        dragonId = dragon.getUniqueId();
        steps = 0;
        tntTimer = 30;
        attackTimer = 20;
        lightningRain = 0;
        nightVision = 0;
        spin = 0;
        decided = false;
        crystalRegen.clear();
        if (!isDemon(dragon)) {
            dragon.getPersistentDataContainer().set(demonKey, PersistentDataType.BYTE, (byte) 1);
            AttributeInstance max = dragon.getAttribute(Attribute.MAX_HEALTH);
            max.setBaseValue(HEALTH);
            dragon.setHealth(max.getValue());
            if (max.getValue() < HEALTH) {
                plugin.getLogger().warning("Permadeath Demon con " + max.getValue() + " de vida en vez de " + HEALTH
                        + ": sube settings.attribute.maxHealth.max en spigot.yml.");
            }
        }
        // Con cada dragón nuevo (al respawnearlo el juego rehace las torres en obsidiana) y al arrancar el server
        bedrockPillars(dragon.getWorld());
        enraged = dragon.getHealth() <= ENRAGED_HEALTH;
        updateName(dragon);
    }

    /** Las torres de obsidiana (anillo a ~42 bloques del centro, hasta Y 103) pasan a bedrock. */
    private static void bedrockPillars(World world) {
        for (int x = -PILLAR_RING_MAX; x <= PILLAR_RING_MAX; x++) {
            for (int z = -PILLAR_RING_MAX; z <= PILLAR_RING_MAX; z++) {
                int dist2 = x * x + z * z;
                if (dist2 < PILLAR_RING_MIN * PILLAR_RING_MIN || dist2 > PILLAR_RING_MAX * PILLAR_RING_MAX) {
                    continue;
                }
                for (int y = world.getMinHeight(); y < PILLAR_TOP; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.OBSIDIAN) {
                        block.setType(Material.BEDROCK, false);
                    }
                }
            }
        }
    }

    private void updateName(EnderDragon dragon) {
        if (enraged) {
            DifficultyManager.setSpecialName(dragon, "ENRAGED PERMADEATH DEMON", NamedTextColor.DARK_RED);
        } else {
            DifficultyManager.setSpecialName(dragon, "PERMADEATH DEMON", NamedTextColor.GOLD);
        }
    }

    private void second(EnderDragon dragon) {
        World world = dragon.getWorld();
        Location portal = world.getHighestBlockAt(0, 0).getLocation();
        if (!enraged && dragon.getHealth() <= ENRAGED_HEALTH) {
            enraged = true;
            updateName(dragon);
        }
        regenCrystals(world);
        randomLightning(world);
        tntAttack(dragon, portal);
        checkSpin(dragon, portal);
        attacks(world, portal);
    }

    /** Cada 30-90 s suelta 6 TNT de potencia 15 (si no está en el portal). */
    private void tntAttack(EnderDragon dragon, Location portal) {
        if (--tntTimer > 0) {
            return;
        }
        tntTimer = 30 + random.nextInt(61);
        if (dragon.getPhase() == EnderDragon.Phase.DYING || spin > 0
                || dragon.getLocation().distance(portal) < 15) {
            return;
        }
        for (int x : new int[] {3, -3}) {
            for (int z : new int[] {-3, 0, 3}) {
                dragon.getWorld().spawn(dragon.getLocation().add(x, 0, z), TNTPrimed.class, tnt -> {
                    tnt.setFuseTicks(60);
                    tnt.setYield(TNT_POWER);
                    tnt.getPersistentDataContainer().set(tntKey, PersistentDataType.BYTE, (byte) 1);
                });
            }
        }
    }

    /** Un rayo cada tanto cerca del portal. */
    private void randomLightning(World world) {
        if (random.nextInt(RANDOM_LIGHTNING_EVERY) != 0) {
            return;
        }
        Block b = world.getHighestBlockAt(random.nextInt(41) - 20, random.nextInt(41) - 20);
        if (!b.getType().isAir()) {
            world.strikeLightning(b.getLocation());
        }
    }

    /** Al posarse en el portal: 80% de hacer el ataque 360 (nubes de Daño IV alrededor). */
    private void checkSpin(EnderDragon dragon, Location portal) {
        double distance = dragon.getLocation().distance(portal);
        if (distance >= 10) {
            decided = false;
        }
        if (distance <= 3 && !decided && spin == 0) {
            decided = true;
            if (random.nextInt(10) <= 7) {
                spin = SPIN_STEPS;
                spinYaw = dragon.getLocation().getYaw();
            }
        }
    }

    private void spinStep(EnderDragon dragon) {
        if (--spin == 0) {
            dragon.setPhase(EnderDragon.Phase.LEAVE_PORTAL);
            return;
        }
        if (dragon.getPhase() != EnderDragon.Phase.LAND_ON_PORTAL) {
            dragon.setPhase(EnderDragon.Phase.LAND_ON_PORTAL);
        }
        if (spin >= SPIN_STEPS - 20) {
            dragon.teleport(dragon.getWorld().getHighestBlockAt(0, 0).getLocation().add(0.5, 2, 0.5));
            return;
        }
        if (spin == SPIN_STEPS - 21) {
            for (int dx = -7; dx <= 7; dx += 7) {
                for (int dz = -7; dz <= 7; dz += 7) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    dragon.getWorld().spawn(ground(dragon.getLocation().add(dx, 0, dz)), AreaEffectCloud.class, c -> {
                        c.setParticle(Particle.CLOUD);
                        c.setRadius(5);
                        c.setDuration(15 * 20);
                        c.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 5, 3), false);
                    });
                }
            }
        }
        spinYaw += 30;
        dragon.setRotation(spinYaw, 0);
    }

    /** Ataques cada 60 s (40 s enfurecido): lluvia de rayos, endermans invencibles o visión nocturna + daño. */
    private void attacks(World world, Location portal) {
        if (lightningRain > 0) {
            lightningRain--;
            for (Player p : world.getPlayers()) {
                world.strikeLightning(p.getLocation());
                if (enraged) {
                    p.damage(1.0);
                }
            }
        }
        if (nightVision > 0 && --nightVision == 0) {
            for (Player p : world.getPlayers()) {
                world.spawn(ground(p.getLocation()), AreaEffectCloud.class, c -> {
                    c.setParticle(Particle.DAMAGE_INDICATOR);
                    c.setRadius(3);
                    c.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 5 * 20, 1), false);
                });
            }
        }
        boolean busy = lightningRain > 0 || nightVision > 0 || spin > 0;
        if (--attackTimer > 0) {
            return;
        }
        attackTimer = enraged ? 40 : 60;
        if (busy) {
            return;
        }
        int r = random.nextInt(25);
        if (r <= 3) {
            lightningRain = 5;
        } else if (r <= 15) {
            buffEndermen(world, portal);
        } else {
            nightVision = 5;
            for (Player p : world.getPlayers()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 7 * 20, 0));
            }
        }
    }

    /** Nube verde de 15 s sobre hasta 4 endermans a 35 bloques del portal: dentro, al morir les salta un tótem. */
    private void buffEndermen(World world, Location portal) {
        int count = 0;
        for (Enderman man : world.getEntitiesByClass(Enderman.class)) {
            Location flat = man.getLocation();
            flat.setY(portal.getY());
            if (count == 4) {
                break;
            }
            if (flat.distance(portal) > 35) {
                continue;
            }
            count++;
            world.spawn(ground(man.getLocation()), AreaEffectCloud.class, c -> {
                c.setParticle(Particle.HAPPY_VILLAGER);
                c.setRadius(10);
                c.setDuration(15 * 20);
                c.getPersistentDataContainer().set(healCloudKey, PersistentDataType.BYTE, (byte) 1);
            });
            man.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 15 * 20, 0));
        }
    }

    /** Un enderman que muere dentro de una nube verde del demonio se salva como con un tótem. */
    @EventHandler
    public void onEndermanResurrect(EntityResurrectEvent e) {
        if (!e.isCancelled() || !(e.getEntity() instanceof Enderman man)) {
            return;
        }
        for (AreaEffectCloud cloud : man.getLocation().getNearbyEntitiesByType(AreaEffectCloud.class, 10)) {
            if (cloud.getPersistentDataContainer().has(healCloudKey, PersistentDataType.BYTE)
                    && cloud.getLocation().distance(man.getLocation()) <= cloud.getRadius()) {
                e.setCancelled(false); // Paper hace el resto: 1 de vida, efectos y animación del tótem
                return;
            }
        }
    }

    /** Encima del bloque más alto de esa columna (o donde está, si debajo es vacío). */
    private static Location ground(Location loc) {
        Block top = loc.getWorld().getHighestBlockAt(loc);
        return top.getType().isAir() ? loc : top.getLocation().add(0.5, 1, 0.5);
    }

    private void regenCrystals(World world) {
        crystalRegen.replaceAll((loc, seconds) -> seconds - 1);
        crystalRegen.entrySet().removeIf(entry -> {
            if (entry.getValue() > 0) {
                return false;
            }
            Block block = entry.getKey().getBlock();
            if (block.getType() == Material.FIRE) {
                block.setType(Material.AIR);
            }
            world.spawn(entry.getKey(), EnderCrystal.class);
            return true;
        });
    }

    /** Los cristales de los pilares vuelven a salir a los 1-8 min mientras viva el demonio. */
    @EventHandler(ignoreCancelled = true)
    public void onCrystalExplode(EntityExplodeEvent e) {
        if (e.getEntityType() != EntityType.END_CRYSTAL
                || e.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType() != Material.BEDROCK) {
            return;
        }
        DragonBattle battle = e.getLocation().getWorld().getEnderDragonBattle();
        EnderDragon dragon = battle == null ? null : battle.getEnderDragon();
        if (dragon != null && !dragon.isDead() && dragon.getUniqueId().equals(dragonId)) {
            crystalRegen.put(e.getLocation().clone(),
                    CRYSTAL_REGEN_SECONDS[random.nextInt(CRYSTAL_REGEN_SECONDS.length)]);
        }
    }

    /** Su TNT no rompe bloques: lanza algo de end stone por el aire (como el original). */
    @EventHandler(ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed)
                || !e.getEntity().getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) {
            return;
        }
        List<Block> blocks = new ArrayList<>(e.blockList());
        e.blockList().clear();
        Collections.shuffle(blocks, random);
        int flying = 0;
        for (Block b : blocks) {
            if (flying == MAX_FLYING_BLOCKS) {
                break;
            }
            if (b.getType() != Material.END_STONE && b.getType() != Material.END_STONE_BRICKS) {
                continue;
            }
            flying++;
            FallingBlock fb = b.getWorld().spawn(b.getLocation().add(0.5, 1, 0.5), FallingBlock.class,
                    f -> f.setBlockData(b.getBlockData()));
            fb.setDropItem(false);
            fb.setVelocity(new Vector((random.nextDouble() - 0.5) * 0.6, 0.3 + random.nextDouble() * 0.9,
                    (random.nextDouble() - 0.5) * 0.6));
        }
    }

    /** Su propia TNT no le hace daño. */
    @EventHandler(ignoreCancelled = true)
    public void onTntHitsDemon(EntityDamageByEntityEvent e) {
        if ((e.getEntity() instanceof EnderDragon || e.getEntity() instanceof EnderDragonPart)
                && e.getDamager().getPersistentDataContainer().has(tntKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
        }
    }

    /** Se cura la mitad (cristales incluidos). */
    @EventHandler(ignoreCancelled = true)
    public void onDemonRegen(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof LivingEntity living && isDemon(living)) {
            e.setAmount(e.getAmount() / 2);
        }
    }

    /**
     * Su bola de aliento convierte el end stone del impacto en bedrock. Enfurecido, la mitad de las veces
     * es una nube de humo con Daño II que además quita todos los efectos.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDemonFireball(EnderDragonFireballHitEvent e) {
        if (!(e.getEntity().getShooter() instanceof EnderDragon dragon) || !isDemon(dragon)) {
            return;
        }
        AreaEffectCloud cloud = e.getAreaEffectCloud();
        if (enraged && random.nextBoolean()) {
            cloud.setParticle(Particle.SMOKE);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20, 1), false);
            cloud.getPersistentDataContainer().set(smokeKey, PersistentDataType.BYTE, (byte) 1);
            return;
        }
        Location center = cloud.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && random.nextBoolean()) {
                    continue;
                }
                // Solo end stone: así no tapa el portal de salida ni el huevo
                Block top = center.getWorld().getHighestBlockAt(center.getBlockX() + dx, center.getBlockZ() + dz);
                if (top.getType() == Material.END_STONE) {
                    top.setType(Material.BEDROCK);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmokeCloud(AreaEffectCloudApplyEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(smokeKey, PersistentDataType.BYTE)) {
            return;
        }
        for (LivingEntity entity : e.getAffectedEntities()) {
            if (entity instanceof Player p && p.getLocation().distance(e.getEntity().getLocation()) <= 3) {
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }
    }

    @EventHandler
    public void onDemonDeath(EntityDeathEvent e) {
        if (!isDemon(e.getEntity())) {
            return;
        }
        crystalRegen.clear();
        spin = 0;
        Bukkit.broadcast(MessageUtils.format("<gold><bold>¡El Permadeath Demon ha sido derrotado!"));
        for (Player p : e.getEntity().getWorld().getPlayers()) {
            // 6 bloques arriba para que la explosión del cohete no le haga daño
            Firework fw = p.getWorld().spawn(p.getLocation().add(0, 6, 0), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().withColor(Color.LIME).flicker(true).build());
            fw.setFireworkMeta(meta);
            fw.detonate();
        }
    }
}
