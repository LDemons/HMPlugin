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

    // ========== DRIED GHAST -> SPAWNER TRAMPA, HAPPY GHAST -> GHAST ==========

    /**
     * Spawner de ghast (los Happy Ghast pasan a Ghast igual): se activa con un jugador a 8 bloques y saca 1 cada
     * 10 min. El spawner muestra un ghast: uno de Happy Ghast reintentaría cada tick al cancelarle el spawn.
     */
    private static void makeGhastTrap(org.bukkit.block.Block block) {
        if (block.getType() != Material.DRIED_GHAST) return;
        block.setType(Material.SPAWNER);
        org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) block.getState();
        spawner.setSpawnedType(org.bukkit.entity.EntityType.GHAST);
        spawner.setRequiredPlayerRange(8);
        spawner.setSpawnCount(1);
        spawner.setMaxSpawnDelay(20 * 60 * 10); // el máximo antes que el mínimo: min no puede pasar a max
        spawner.setMinSpawnDelay(20 * 60 * 10);
        spawner.update();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDriedGhastPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (day50() && e.getBlockPlaced().getType() == Material.DRIED_GHAST) {
            org.bukkit.block.Block block = e.getBlockPlaced();
            Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> makeGhastTrap(block));
        }
    }

    /** Los Dried Ghast que ya están en el mundo pasan a spawner al cargar su chunk (se mira la paleta antes). */
    @EventHandler
    public void onDriedGhastChunk(org.bukkit.event.world.ChunkLoadEvent e) {
        if (!day50() || !hasDriedGhast(e.getChunk())) return;
        org.bukkit.Chunk chunk = e.getChunk();
        Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> {
            if (!chunk.isLoaded()) return;
            for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++)
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        makeGhastTrap(chunk.getBlock(x, y, z));
        });
    }

    private static boolean hasDriedGhast(org.bukkit.Chunk chunk) {
        org.bukkit.block.data.type.DriedGhast data = (org.bukkit.block.data.type.DriedGhast) Material.DRIED_GHAST.createBlockData();
        for (org.bukkit.block.BlockFace face : data.getFaces())
            for (int h = 0; h <= data.getMaximumHydration(); h++)
                for (boolean water : new boolean[] { false, true }) {
                    data.setFacing(face);
                    data.setHydration(h);
                    data.setWaterlogged(water);
                    if (chunk.contains(data)) return true;
                }
        return false;
    }

    /** Todo Happy Ghast nuevo sale como Ghast (con los efectos del día). */
    @EventHandler(ignoreCancelled = true)
    public void onHappyGhastSpawn(org.bukkit.event.entity.CreatureSpawnEvent e) {
        if (day50() && e.getEntityType() == org.bukkit.entity.EntityType.HAPPY_GHAST
                && e.getSpawnReason() != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM) {
            e.setCancelled(true);
            HMPlugin.getInstance().getDifficultyManager().applyMobEffects((org.bukkit.entity.LivingEntity)
                    e.getLocation().getWorld().spawnEntity(e.getLocation(), org.bukkit.entity.EntityType.GHAST));
        }
    }

    /** Los Happy Ghast que ya existían (también domados) pasan a Ghast al cargarse; el arnés se suelta. */
    @EventHandler
    public void onHappyGhastLoad(org.bukkit.event.world.EntitiesLoadEvent e) {
        if (!day50()) return;
        for (org.bukkit.entity.Entity entity : e.getEntities()) {
            if (entity.getType() != org.bukkit.entity.EntityType.HAPPY_GHAST) continue;
            org.bukkit.entity.LivingEntity happy = (org.bukkit.entity.LivingEntity) entity;
            org.bukkit.Location loc = happy.getLocation();
            ItemStack harness = happy.getEquipment() == null ? null
                    : happy.getEquipment().getItem(org.bukkit.inventory.EquipmentSlot.BODY);
            if (harness != null && !harness.isEmpty()) loc.getWorld().dropItemNaturally(loc, harness);
            happy.remove();
            HMPlugin.getInstance().getDifficultyManager().applyMobEffects((org.bukkit.entity.LivingEntity)
                    loc.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.GHAST));
        }
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
