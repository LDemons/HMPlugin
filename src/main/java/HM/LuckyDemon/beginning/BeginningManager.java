package HM.LuckyDemon.beginning;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.events.AchievementListener;
import HM.LuckyDemon.managers.DifficultyManager;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.PortalType;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.structure.Structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Beginning: mundo propio que se crea el dia 40 con un portal flotante escondido en el Overworld.
 * Se entra desde el dia 50 (antes el portal te mata) y se cierra durante el Death Train.
 */
public class BeginningManager implements Listener {

    public static final String WORLD_NAME = "hm_the_beginning";
    public static final int CREATE_DAY = 40;
    public static final int ENTER_DAY = 50;
    private static final int PORTAL_RANGE = 3000;
    private static final int MOB_LIMIT = 70;
    /** Tope por zona: con un solo jugador los 70 terminaban todos a su alrededor (pasillos de la Ytic llenos). */
    private static final int LOCAL_MOB_LIMIT = 8;
    private static final double LOCAL_MOB_RADIUS = 32;
    /** Del punto de llegada (donde estaba parado el que hizo el schematic) a la esquina minima del portal. */
    private static final int[] PORTAL_OFFSET = { -11, -4, -15 };

    private final HMPlugin plugin;
    private final Random random = new Random();
    private final Set<UUID> inTransit = new HashSet<>();
    private static final double QUANTUM_DAMAGE_MULTIPLIER = 5;
    private static final int MIN_LOOT = 3;
    /** Ghasts naturales solo desde esta altura; por debajo solo de spawners. */
    private static final int GHAST_MIN_Y = 95;
    private final NamespacedKey portalKey, lootedKey, firstEntryKey, skipKey, quantumKey, deathTrainKey;
    private World world;

    public BeginningManager(HMPlugin plugin) {
        this.plugin = plugin;
        portalKey = new NamespacedKey(plugin, "beginning_portal");
        lootedKey = new NamespacedKey(plugin, "beginning_looted");
        firstEntryKey = new NamespacedKey(plugin, "beginning_first_entry");
        skipKey = new NamespacedKey(plugin, "skip_effects");
        quantumKey = new NamespacedKey(plugin, "quantum_creeper");
        deathTrainKey = new NamespacedKey(plugin, "death_train");
    }

    public World getWorld() {
        return world;
    }

    private boolean isBeginning(World w) {
        return world != null && w.equals(world);
    }

    private static World overworld() {
        return Bukkit.getWorlds().get(0);
    }

    /** Cerrado durante el Death Train (desde el dia 50, como en el original). La lluvia normal no lo cierra. */
    public boolean isClosed() {
        return GameManager.getInstance().getDay() >= ENTER_DAY && overworld().hasStorm()
                && overworld().getPersistentDataContainer().has(deathTrainKey);
    }

    /** Lo llama la muerte de un jugador al empezar o alargar el Death Train: cierra y saca a todos. */
    public void startDeathTrain() {
        // DÍA 50: modo UHC (como el original): sin regeneración natural en ningún mundo mientras dure. Solo se
        // anuncia al empezar, no cuando otra muerte alarga la tormenta
        if (GameManager.getInstance().getDay() >= 50
                && !overworld().getPersistentDataContainer().has(deathTrainKey)) {
            for (World w : Bukkit.getWorlds()) {
                w.setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, false);
            }
            Bukkit.broadcast(MessageUtils.format("<red>¡Ha comenzado el modo UHC! <gray>(sin regeneración natural hasta que acabe el Death Train)"));
        }
        overworld().getPersistentDataContainer().set(deathTrainKey, PersistentDataType.BYTE, (byte) 1);
        if (world == null || GameManager.getInstance().getDay() < ENTER_DAY || world.getPlayers().isEmpty()) return;
        Bukkit.broadcast(MessageUtils.format("<yellow>The Beginning ha cerrado durante el Death Train."));
        for (Player p : world.getPlayers()) {
            p.teleport(overworld().getSpawnLocation());
            p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
        }
    }

    /** Crea o carga el mundo y los dos portales. Se puede llamar de nuevo sin problema. */
    public void setup() {
        if (world != null || GameManager.getInstance().getDay() < CREATE_DAY) return;

        Structure portal = load("beginning_portal");
        List<Structure> islands = new ArrayList<>();
        for (int i = 1; i <= 5; i++) islands.add(load("island" + i));

        world = new WorldCreator(WORLD_NAME)
                .environment(World.Environment.THE_END)
                .generator(new BeginningGenerator(islands, load("ytic")))
                .generateStructures(false)
                .createWorld();
        world.setGameRule(GameRule.MOB_GRIEFING, false);

        // Portal de llegada en The Beginning (50, 140, 50), una sola vez
        if (!world.getPersistentDataContainer().has(portalKey)) {
            Location arrival = new Location(world, 50, 140, 50);
            place(portal, arrival);
            world.setSpawnLocation(arrival);
            world.getPersistentDataContainer().set(portalKey, PersistentDataType.BYTE, (byte) 1);
        }

        // Portal en el Overworld: X/Z al azar, flotando 15 bloques sobre el suelo. Nadie sabe donde esta
        World ow = overworld();
        if (!ow.getPersistentDataContainer().has(portalKey)) {
            int x = random.nextInt(PORTAL_RANGE * 2) - PORTAL_RANGE;
            int z = random.nextInt(PORTAL_RANGE * 2) - PORTAL_RANGE;
            Location loc = new Location(ow, x, ow.getHighestBlockYAt(x, z) + 15, z);
            place(portal, loc);
            ow.getPersistentDataContainer().set(portalKey, PersistentDataType.INTEGER_ARRAY,
                    new int[] { loc.getBlockX(), loc.getBlockY(), loc.getBlockZ() });
            plugin.getLogger().info("Portal a The Beginning creado en " + x + " " + loc.getBlockY() + " " + z);
        }
    }

    /** Punto de llegada del portal del Overworld (al lado del arco), o null si todavia no existe. */
    public Location getOverworldPortal() {
        int[] c = overworld().getPersistentDataContainer().get(portalKey, PersistentDataType.INTEGER_ARRAY);
        return c == null ? null : new Location(overworld(), c[0] + 0.5, c[1], c[2] + 0.5);
    }

    private Structure load(String name) {
        try (InputStream in = plugin.getResource("beginning/" + name + ".nbt")) {
            return Bukkit.getStructureManager().loadStructure(in);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la estructura " + name, e);
        }
    }

    private void place(Structure portal, Location arrival) {
        portal.place(arrival.clone().add(PORTAL_OFFSET[0], PORTAL_OFFSET[1], PORTAL_OFFSET[2]), false,
                StructureRotation.NONE, Mirror.NONE, 0, 1f, random);
    }

    // ========== PORTALES ==========

    /** Los end gateways del Overworld y de The Beginning son los portales: se cancela el viaje vanilla. */
    @EventHandler(ignoreCancelled = true)
    public void onGateway(EntityPortalEnterEvent e) {
        World from = e.getEntity().getWorld();
        if (e.getPortalType() != PortalType.END_GATEWAY || world == null
                || (!isBeginning(from) && !from.equals(overworld()))) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getEntity() instanceof Player p) || p.isDead() || !inTransit.add(p.getUniqueId())) return;

        // El evento salta en cada tick dentro del portal: se viaja al tick siguiente y una sola vez
        Bukkit.getScheduler().runTask(plugin, () -> {
            inTransit.remove(p.getUniqueId());
            if (!p.isOnline() || p.isDead()) return;
            int day = GameManager.getInstance().getDay();
            if (day < ENTER_DAY) {
                p.setHealth(0);
                Bukkit.broadcast(MessageUtils.format("<red><bold>" + p.getName()
                        + " intentó entrar a The Beginning antes de tiempo."));
            } else if (isClosed()) {
                p.sendActionBar(MessageUtils.format("<yellow>The Beginning está cerrado durante el Death Train."));
            } else if (isBeginning(p.getWorld())) {
                p.teleport(overworld().getSpawnLocation());
            } else {
                p.teleport(world.getSpawnLocation().add(0.5, 0, 0.5));
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                p.sendMessage(MessageUtils.format("<light_purple>Bienvenido a The Beginning."));
                if (!world.getPersistentDataContainer().has(firstEntryKey)) {
                    world.getPersistentDataContainer().set(firstEntryKey, PersistentDataType.STRING, p.getName());
                    AchievementListener.grant(p, "yokoso");
                }
            }
        });
    }

    /** Al acabar la tormenta del Overworld se acaba el Death Train y The Beginning vuelve a abrir. */
    @EventHandler(ignoreCancelled = true)
    public void onStormEnd(WeatherChangeEvent e) {
        if (!e.toWeatherState() && e.getWorld().equals(overworld())
                && e.getWorld().getPersistentDataContainer().has(deathTrainKey)) {
            e.getWorld().getPersistentDataContainer().remove(deathTrainKey);
            // Fin del modo UHC (se devuelve aunque el día haya bajado de 50 entre medio)
            for (World w : Bukkit.getWorlds()) {
                if (Boolean.FALSE.equals(w.getGameRuleValue(org.bukkit.GameRule.NATURAL_REGENERATION))) {
                    w.setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, true);
                }
            }
            if (GameManager.getInstance().getDay() >= 50) {
                Bukkit.broadcast(MessageUtils.format("<green>Ha terminado el modo UHC: vuelve la regeneración natural."));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (isBeginning(e.getPlayer().getWorld()) && isClosed()) {
            e.getPlayer().teleport(overworld().getSpawnLocation());
        }
    }

    /** Sin portales nuevos (ni del Nether ni el del dragon) dentro de The Beginning. */
    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent e) {
        if (isBeginning(e.getWorld())) e.setCancelled(true);
    }

    // ========== REGLAS DEL MUNDO ==========

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (isBeginning(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispenseBucket(BlockDispenseEvent e) {
        if (isBeginning(e.getBlock().getWorld()) && e.getItem().getType().name().endsWith("_BUCKET")) {
            e.setCancelled(true);
        }
    }

    /** Nadie es invisible en The Beginning. */
    @EventHandler(ignoreCancelled = true)
    public void onInvisibility(EntityPotionEffectEvent e) {
        if (e.getEntity() instanceof Player && isBeginning(e.getEntity().getWorld()) && e.getNewEffect() != null
                && e.getNewEffect().getType().equals(PotionEffectType.INVISIBILITY)) {
            e.setCancelled(true);
        }
    }

    // ========== COFRES ==========

    @EventHandler(ignoreCancelled = true)
    public void onChestOpen(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) fillChest(e.getClickedBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent e) {
        fillChest(e.getBlock());
    }

    // ========== NETHERITE INFERNAL ==========

    /** Modelo del resource pack y marca del display. */
    public static final NamespacedKey INFERNAL_NETHERITE = new NamespacedKey("hmplugin", "infernal_netherite");

    /**
     * Como en el original, el bloque es un spawner que no saca nada. Para que se vea como bloque entero lo tapa un
     * item display un pelo mas grande que el bloque (asi no se ve la jaula) con la textura del pack.
     */
    public static void infernalDisplay(org.bukkit.entity.ItemDisplay d) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(m -> m.setItemModel(INFERNAL_NETHERITE));
        d.setItemStack(item);
        d.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.NONE);
        d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(), new org.joml.AxisAngle4f(),
                new org.joml.Vector3f(1.002f), new org.joml.AxisAngle4f()));
        d.getPersistentDataContainer().set(INFERNAL_NETHERITE, PersistentDataType.BYTE, (byte) 1);
    }

    /** Quita el display del bloque; true si ese spawner era un bloque de Netherite Infernal. */
    private static boolean removeInfernalDisplay(Block block) {
        if (block.getType() != Material.SPAWNER) return false;
        boolean found = false;
        for (org.bukkit.entity.Entity e : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5),
                0.3, 0.3, 0.3, e -> e.getPersistentDataContainer().has(INFERNAL_NETHERITE))) {
            e.remove();
            found = true;
        }
        return found;
    }

    /** Picado con pico suelta el Bloque de Netherite Infernal (sin la experiencia del spawner). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInfernalBreak(BlockBreakEvent e) {
        if (!removeInfernalDisplay(e.getBlock())) return;
        e.setExpToDrop(0);
        if (org.bukkit.Tag.ITEMS_PICKAXES.isTagged(e.getPlayer().getInventory().getItemInMainHand().getType())) {
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.5, 0.5),
                    HM.LuckyDemon.items.HMItems.createInfernalNetherite());
        }
    }

    /**
     * Armadura de Netherite Infernal: conserva los encantamientos de la pieza del centro. El Bloque de Netherite
     * Infernal es papel, asi que en cualquier otra receta (libros, mapas, cohetes) no da resultado.
     */
    @EventHandler
    public void onInfernalCraft(org.bukkit.event.inventory.PrepareItemCraftEvent e) {
        ItemStack[] matrix = e.getInventory().getMatrix();
        if (java.util.Arrays.stream(matrix).noneMatch(HM.LuckyDemon.items.HMItems::isInfernalNetherite)) return;
        ItemStack result = e.getInventory().getResult();
        if (!(e.getRecipe() instanceof org.bukkit.Keyed k) || !k.getKey().getKey().startsWith("infernal_")
                || result == null || matrix.length != 9 || matrix[4] == null) {
            e.getInventory().setResult(null);
            return;
        }
        result.addUnsafeEnchantments(matrix[4].getEnchantments());
        e.getInventory().setResult(result);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInfernalExplode(org.bukkit.event.entity.EntityExplodeEvent e) {
        e.blockList().forEach(BeginningManager::removeInfernalDisplay);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInfernalBlockExplode(org.bukkit.event.block.BlockExplodeEvent e) {
        e.blockList().forEach(BeginningManager::removeInfernalDisplay);
    }

    /** Los cofres que pone un jugador no traen loot. */
    @EventHandler(ignoreCancelled = true)
    public void onChestPlace(BlockPlaceEvent e) {
        if (isBeginning(e.getBlock().getWorld()) && e.getBlock().getState() instanceof Chest chest) {
            chest.getPersistentDataContainer().set(lootedKey, PersistentDataType.BYTE, (byte) 1);
            chest.update();
        }
    }

    /** Objeto del loot: probabilidad en %, cantidad minima y maxima. */
    private record Loot(ItemStack item, int chance, int min, int max) {
        Loot(Material mat, int chance, int min, int max) {
            this(new ItemStack(mat), chance, min, max);
        }
    }

    /**
     * Los objetos del original (sin flechas ni el structure void) mas botellas de experiencia, con mas cantidad y
     * probabilidad (Nacho lo pidio).
     */
    private static final List<Loot> LOOT = List.of(
            new Loot(Material.DIAMOND, 70, 24, 40),
            new Loot(Material.GOLDEN_APPLE, 70, 4, 10),
            new Loot(Material.GOLD_INGOT, 60, 40, 64),
            new Loot(Material.EXPERIENCE_BOTTLE, 40, 8, 24),
            new Loot(Material.FIREWORK_ROCKET, 35, 32, 64),
            new Loot(Material.TOTEM_OF_UNDYING, 35, 1, 1));

    /**
     * 2 o 3 tiradas, cada objeto sale una vez como mucho y su cantidad se reparte en 1 a 3 montones en huecos al
     * azar del cofre. Siempre trae al menos MIN_LOOT objetos distintos.
     */
    private void fillChest(Block block) {
        if (!isBeginning(block.getWorld()) || !(block.getState() instanceof Chest chest)
                || chest.getPersistentDataContainer().has(lootedKey)) {
            return;
        }
        Inventory inv = chest.getSnapshotInventory();
        // Cofres que ya traen cosas de la estructura (el de herramientas de la Ytic) no reciben loot. Sus
        // herramientas de diamante irrompibles eran de netherite en el original (por textura): pasan a netherite
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType().name().startsWith("DIAMOND_")) {
                Material netherite = Material.matchMaterial("NETHERITE_" + it.getType().name().substring(8));
                if (netherite != null) inv.setItem(i, it.withType(netherite));
            }
        }
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < inv.getSize() && inv.isEmpty(); i++) free.add(i);
        java.util.Collections.shuffle(free, random);
        Set<Loot> rolled = new HashSet<>();
        // Se sigue tirando hasta tener al menos MIN_LOOT objetos distintos (salian cofres con solo manzanas)
        for (int roll = random.nextInt(2) + 2; !free.isEmpty() && (roll > 0 || rolled.size() < MIN_LOOT); roll--) {
            for (Loot loot : LOOT) {
                if (random.nextInt(100) >= loot.chance() || !rolled.add(loot)) continue;
                int amount = loot.min() + random.nextInt(loot.max() - loot.min() + 1);
                for (int piles = Math.min(amount, random.nextInt(3) + 1); piles > 0 && !free.isEmpty(); piles--) {
                    int part = piles == 1 ? amount : 1 + random.nextInt(amount - piles + 1);
                    inv.setItem(free.removeLast(), loot.item().asQuantity(part));
                    amount -= part;
                }
            }
        }
        chest.getPersistentDataContainer().set(lootedKey, PersistentDataType.BYTE, (byte) 1);
        chest.update();
    }

    // ========== MOBS ==========

    /**
     * Los spawns naturales se cambian por los mobs de The Beginning (tope de 70 en el mundo). Va antes que
     * PlayerListener, que ignora los spawns cancelados y los CUSTOM.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!isBeginning(e.getLocation().getWorld())) return;
        LivingEntity entity = e.getEntity();

        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            // Spawners de las islas y la Ytic
            switch (entity.getType()) {
                case CREEPER -> quantumCreeper((Creeper) entity);
                case GHAST -> definitiveGhast((Ghast) entity);
                case VEX -> definitiveVex((Vex) entity);
                case WITHER -> {
                    entity.setRemoveWhenFarAway(true);
                    if (entity.getLocation().getY() >= MINI_WITHER_MIN_Y) miniWither((org.bukkit.entity.Wither) entity);
                }
                default -> { return; }
            }
            entity.getPersistentDataContainer().set(skipKey, PersistentDataType.BYTE, (byte) 1);
            return;
        }
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        e.setCancelled(true);
        if (entity.getType() == EntityType.ENDER_DRAGON || world.getLivingEntities().size() > MOB_LIMIT) return;
        Location loc = e.getLocation();
        // Minecraft saca grupos de hasta 4 en el mismo tick: 1 por chunk cada CHUNK_SPAWN_COOLDOWN_MS
        long chunkKey = org.bukkit.Chunk.getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        long now = System.currentTimeMillis();
        Long last = chunkSpawns.get(chunkKey);
        if ((last != null && now - last < CHUNK_SPAWN_COOLDOWN_MS) || crowded(loc)) return;
        if (chunkSpawns.size() > 2000) chunkSpawns.values().removeIf(t -> now - t >= CHUNK_SPAWN_COOLDOWN_MS);
        chunkSpawns.put(chunkKey, now);
        // Se vuelve a mirar al crearlo: los que estan en camino (tick siguiente) no se contaban
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!crowded(loc)) spawnBeginningMob(loc);
        });
    }

    private static final long CHUNK_SPAWN_COOLDOWN_MS = 10_000;
    private final java.util.Map<Long, Long> chunkSpawns = new java.util.HashMap<>();

    private static boolean crowded(Location loc) {
        return loc.getNearbyLivingEntities(LOCAL_MOB_RADIUS, ent -> !(ent instanceof Player)).size() >= LOCAL_MOB_LIMIT;
    }

    private static final long WITHER_COOLDOWN_MS = 5 * 60 * 1000;
    /** Withers normales de la Ytic: 1 cada 15 min (primero 5, salian demasiados). */
    private static final long NORMAL_WITHER_COOLDOWN_MS = 15 * 60 * 1000;
    private static final double WITHER_NEARBY_RADIUS = 48;
    /** Spawners de wither de las islas chicas (Y 120-123): tandas de 3 Mini Withers. Los de la Ytic (Y 37-61), 1 normal. */
    private static final int MINI_WITHER_MIN_Y = 100;
    private static final int MINI_WITHERS_PER_WAVE = 3;
    /** Por spawner: inicio de la tanda y withers sacados en ella (en memoria: se reinicia con el server). */
    private final java.util.Map<Location, long[]> witherSpawns = new java.util.HashMap<>();

    /**
     * Spawners de wither: los de las islas sacan 3 Mini Withers cada 5 minutos y los de la Ytic 1 wither normal cada
     * 15, y ninguno saca otra tanda si queda un wither a 48 bloques (vale tambien para los ya generados).
     */
    @EventHandler(ignoreCancelled = true)
    public void onBeginningSpawner(org.bukkit.event.entity.SpawnerSpawnEvent e) {
        if (!isBeginning(e.getLocation().getWorld()) || e.getSpawner() == null) return;
        org.bukkit.block.CreatureSpawner spawner = e.getSpawner();
        if (e.getEntityType() == EntityType.WITHER) {
            long now = System.currentTimeMillis();
            boolean mini = spawner.getY() >= MINI_WITHER_MIN_Y;
            long[] wave = witherSpawns.computeIfAbsent(spawner.getLocation(), k -> new long[] { 0, 0 });
            if (now - wave[0] >= (mini ? WITHER_COOLDOWN_MS : NORMAL_WITHER_COOLDOWN_MS)) {
                // Los withers nunca despawnean: no se empieza otra tanda mientras quede alguno cerca
                if (!spawner.getWorld().getNearbyEntitiesByType(org.bukkit.entity.Wither.class,
                        spawner.getLocation().add(0.5, 0.5, 0.5), WITHER_NEARBY_RADIUS).isEmpty()) {
                    e.setCancelled(true);
                    return;
                }
                wave[0] = now;
                wave[1] = 0;
            }
            if (wave[1] >= (mini ? MINI_WITHERS_PER_WAVE : 1)) e.setCancelled(true);
            else wave[1]++;
        }
    }

    /**
     * La explosion del Quantum Creeper hace x5 de dano: en el centro pasa de ~200 a ~1000 (mata a un wither, 300 de
     * vida, y a casi cualquier cosa cerca). No rompe bloques (mob griefing desactivado en The Beginning).
     */
    @EventHandler(ignoreCancelled = true)
    public void onQuantumExplosion(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Creeper c && c.getPersistentDataContainer().has(quantumKey)) {
            e.setDamage(e.getDamage() * QUANTUM_DAMAGE_MULTIPLIER);
        }
    }

    /**
     * Los spawners de monstruos (wither, vex, creeper) no sacan nada con luz mayor a ~12 cerca, y las islas tienen
     * lava y sea lanterns: se les pone una regla de spawn con cualquier luz. Al cargar el chunk, asi vale tambien
     * para los ya generados (se compara por nombre porque los chunks del spawn cargan antes de tener el mundo).
     */
    @EventHandler
    public void onBeginningChunkLoad(org.bukkit.event.world.ChunkLoadEvent e) {
        if (!e.getWorld().getName().equals(WORLD_NAME)) return;
        org.bukkit.block.spawner.SpawnRule anyLight = new org.bukkit.block.spawner.SpawnRule(0, 15, 0, 15);
        for (org.bukkit.block.BlockState st : e.getChunk().getTileEntities()) {
            if (!(st instanceof org.bukkit.block.CreatureSpawner s) || s.getSpawnedType() == null
                    || s.getSpawnedType() == EntityType.ARMOR_STAND) {
                continue;
            }
            if (s.getSpawnedEntity() == null || (!s.getPotentialSpawns().isEmpty()
                    && s.getPotentialSpawns().stream().allMatch(p -> p.getSpawnRule() != null))) {
                continue;
            }
            org.bukkit.block.spawner.SpawnerEntry entry =
                    new org.bukkit.block.spawner.SpawnerEntry(s.getSpawnedEntity(), 1, anyLight);
            s.setPotentialSpawns(List.of(entry));
            s.setSpawnedEntity(entry);
            s.update(true, false);
        }
    }

    private void spawnBeginningMob(Location loc) {
        // Sobre 85: los vex (antes 15) solo salen de los spawners de las estructuras
        int p = random.nextInt(85);
        if (p < 60) {
            world.spawn(loc, WitherSkeleton.class, this::rosySkeleton);
        } else if (p < 64) {
            // Por debajo de GHAST_MIN_Y los ghasts solo salen de spawners (el del zepelin de la Ytic): se tira de nuevo
            if (loc.getY() < GHAST_MIN_Y) spawnBeginningMob(loc);
            else world.spawn(loc.clone().add(0, 5, 0), Ghast.class, this::definitiveGhast);
        } else {
            world.spawn(loc, Creeper.class, this::quantumCreeper);
        }
    }

    /** Wither de los spawners de las islas: 30% del tamano (primero 50%) y 100 de vida (un tercio de uno normal). */
    private void miniWither(org.bukkit.entity.Wither w) {
        w.getAttribute(Attribute.SCALE).setBaseValue(0.3);
        setHealth(w, 100);
        DifficultyManager.setSpecialName(w, "Mini Wither", NamedTextColor.GOLD);
    }

    private static void setHealth(LivingEntity entity, double health) {
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        entity.setHealth(health);
    }

    private void rosySkeleton(WitherSkeleton s) {
        EntityEquipment eq = s.getEquipment();
        eq.setChestplate(pinkLeather(Material.LEATHER_CHESTPLATE));
        eq.setBoots(pinkLeather(Material.LEATHER_BOOTS));
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.addUnsafeEnchantment(Enchantment.SHARPNESS, random.nextInt(5) + 1);
        eq.setItemInMainHand(sword);
        eq.setChestplateDropChance(0);
        eq.setBootsDropChance(0);
        eq.setItemInMainHandDropChance(0);
        s.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 2));
        setHealth(s, 100);
        DifficultyManager.setSpecialName(s, "Wither Skeleton Rosáceo", NamedTextColor.GOLD);
    }

    private static ItemStack pinkLeather(Material mat) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(LeatherArmorMeta.class, m -> m.setColor(Color.fromRGB(255, 182, 193)));
        return item;
    }

    private void definitiveVex(Vex v) {
        EntityEquipment eq = v.getEquipment();
        ItemStack helmet = new ItemStack(Material.CARVED_PUMPKIN);
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        ItemStack crystal = new ItemStack(Material.END_CRYSTAL);
        crystal.addUnsafeEnchantment(Enchantment.SHARPNESS, 15);
        crystal.addUnsafeEnchantment(Enchantment.KNOCKBACK, 20);
        eq.setHelmet(helmet);
        eq.setItemInMainHand(crystal);
        eq.setHelmetDropChance(0);
        eq.setItemInMainHandDropChance(0);
        DifficultyManager.setSpecialName(v, "Vex Definitivo", NamedTextColor.GOLD);
    }

    /**
     * Ender Ghast con 150 de vida: usa la marca y la explosion de los Ender Ghasts del dia 30, con potencia 10 (el
     * doble que el mas fuerte de esos; primero era 5). El impacto directo hace x6.6 (formula de PlayerListener).
     */
    private void definitiveGhast(Ghast g) {
        g.getPersistentDataContainer().set(new NamespacedKey(plugin, "ender_ghast"), PersistentDataType.BYTE, (byte) 1);
        g.getPersistentDataContainer().set(new NamespacedKey(plugin, "explosion_power"), PersistentDataType.INTEGER, 10);
        setHealth(g, 150);
        DifficultyManager.setSpecialName(g, "Ender Ghast Definitivo", NamedTextColor.GOLD);
    }

    /** Ender Creeper (invisible, electrico, se teletransporta) con 100 de vida y explosion 7 que mata todo. */
    private void quantumCreeper(Creeper c) {
        c.getPersistentDataContainer().set(new NamespacedKey(plugin, "ender_creeper"), PersistentDataType.BYTE, (byte) 1);
        c.getPersistentDataContainer().set(quantumKey, PersistentDataType.BYTE, (byte) 1);
        c.setPowered(true);
        c.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0));
        c.setExplosionRadius(7);
        setHealth(c, 100);
        DifficultyManager.setSpecialName(c, "Quantum Creeper", NamedTextColor.GOLD);
    }
}
