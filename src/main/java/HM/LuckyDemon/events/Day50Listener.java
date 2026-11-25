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

import java.util.List;

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
            // DÍA 60: 10 veces más rápido
            e.setAmount(Math.max(0, current - lost * (GameManager.getInstance().getDay() >= 60 ? 10 : 5)));
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

    /**
     * Mining Fatigue: la leche no la quita y dura el doble. El evento no deja cambiar el efecto: se cancela y se pone
     * el doble al tick siguiente (con causa PLUGIN, que aquí no se toca).
     */
    @EventHandler(ignoreCancelled = true)
    public void onMiningFatigue(org.bukkit.event.entity.EntityPotionEffectEvent e) {
        if (!day50() || !(e.getEntity() instanceof Player p)
                || e.getModifiedType() != PotionEffectType.MINING_FATIGUE) return;
        org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause = e.getCause();
        if (cause == org.bukkit.event.entity.EntityPotionEffectEvent.Cause.MILK) {
            e.setCancelled(true);
            return;
        }
        PotionEffect effect = e.getNewEffect();
        if (cause == org.bukkit.event.entity.EntityPotionEffectEvent.Cause.PLUGIN || effect == null
                || effect.isInfinite()) return;
        e.setCancelled(true);
        Bukkit.getScheduler().runTask(HMPlugin.getInstance(),
                () -> p.addPotionEffect(effect.withDuration(effect.getDuration() * 2)));
    }

    /**
     * Comidas: la tarta de calabaza da 5 s de Saturación (la mejor comida) y el ojo de araña, la carne podrida, la
     * patata venenosa y el pez globo dejan su efecto para siempre (como el original, que lo pone 5 ticks después).
     */
    @EventHandler(ignoreCancelled = true)
    public void onEat(org.bukkit.event.player.PlayerItemConsumeEvent e) {
        if (!day50()) return;
        Player p = e.getPlayer();
        int forever = PotionEffect.INFINITE_DURATION;
        List<PotionEffect> effects = switch (e.getItem().getType()) {
            // DÍA 60: pasa a ser la peor comida (Daño instantáneo IV)
            case PUMPKIN_PIE -> List.of(GameManager.getInstance().getDay() >= 60
                    ? new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 3)
                    : new PotionEffect(PotionEffectType.SATURATION, 20 * 5, 0));
            case SPIDER_EYE, POISONOUS_POTATO -> List.of(new PotionEffect(PotionEffectType.POISON, forever, 0));
            case ROTTEN_FLESH -> List.of(new PotionEffect(PotionEffectType.HUNGER, forever, 1));
            case PUFFERFISH -> List.of(new PotionEffect(PotionEffectType.POISON, forever, 3),
                    new PotionEffect(PotionEffectType.HUNGER, forever, 2),
                    new PotionEffect(PotionEffectType.NAUSEA, forever, 1));
            default -> List.of();
        };
        if (effects.isEmpty()) return;
        Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
            for (PotionEffect effect : effects) {
                p.removePotionEffect(effect.getType()); // si no, el de la comida (más fuerte o más largo) gana
                p.addPotionEffect(effect);
            }
        }, 5L);
    }

    /**
     * Escupitajo de llama: Veneno III 30 s, Náusea 10 s y Empuje III (se lanza al tick siguiente: el empuje normal
     * del golpe se aplica después del evento y pisaría la velocidad). Arañas de cueva: Veneno III y Náusea.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDay50Hit(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (!day50() || !(e.getEntity() instanceof Player p)) return;
        if (e.getDamager() instanceof org.bukkit.entity.LlamaSpit spit) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 30, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0));
            org.bukkit.util.Vector push = spit.getVelocity().setY(0).normalize().multiply(1.6).setY(0.45);
            if (Double.isFinite(push.getX())) {
                Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> p.setVelocity(push));
            }
        } else if (e.getDamager() instanceof org.bukkit.entity.CaveSpider) {
            // El Veneno I del juego llega después y no pisa uno más fuerte
            p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 15, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 0));
        } else if (e.getDamager() instanceof org.bukkit.entity.PolarBear bear
                && e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            // Solo su golpe: el daño de su propia explosión también llega con el oso como atacante
            // Como el original: en vez de pegar se queda quieto, suena a creeper y explota medio segundo después
            e.setCancelled(true);
            if (!bear.hasAI()) return; // ya está por explotar
            bear.setAI(false);
            bear.getWorld().playSound(bear.getLocation(), org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                if (!bear.isValid()) return;
                bear.getWorld().createExplosion(bear.getLocation(), 3f, false, true, bear); // como un creeper normal
                bear.remove();
            }, 10L);
        } else if (e.getDamager().getPersistentDataContainer().has(key("death_cod"))) {
            // Bacalao de la Muerte: Empuje C (el golpe del bacalao no empuja; se lanza al tick siguiente)
            org.bukkit.util.Vector push = p.getLocation().toVector().subtract(e.getDamager().getLocation().toVector())
                    .setY(0).normalize().multiply(5).setY(1);
            if (Double.isFinite(push.getX())) {
                Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> p.setVelocity(push));
            }
        }
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(HMPlugin.getInstance(), name);
    }

    /** Wither Skeleton Emperador: las flechas no le hacen daño. */
    @EventHandler(ignoreCancelled = true)
    public void onEmperorArrow(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof org.bukkit.entity.AbstractArrow
                && e.getEntity().getPersistentDataContainer().has(key("wither_emperor"))) {
            e.setCancelled(true);
        }
    }

    /** Wither Skeleton Emperador: 50% de soltar una espada de netherite (HIGHEST: desde el 20 se borran sus drops). */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onEmperorDeath(EntityDeathEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(key("wither_emperor"))
                && GameManager.getInstance().getDay() < 60 // DÍA 60: sin drops
                && java.util.concurrent.ThreadLocalRandom.current().nextBoolean()) {
            // Irrompible, como las herramientas de netherite de The Beginning y la Espada de Netherite del original
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.editMeta(meta -> meta.setUnbreakable(true));
            e.getDrops().add(sword);
        }
    }

    /** Gigante: suelta el Arco de Gigante (Poder X). */
    @EventHandler
    public void onGiantDeath(EntityDeathEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(key("hm_giant"))
                && GameManager.getInstance().getDay() < 60) { // DÍA 60: sin drops
            ItemStack bow = new ItemStack(Material.BOW);
            bow.editMeta(meta -> meta.displayName(net.kyori.adventure.text.Component.text("Arco de Gigante",
                    net.kyori.adventure.text.format.NamedTextColor.AQUA)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
            bow.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 10);
            e.getDrops().add(bow);
        }
    }

    /**
     * Un Ultra Ravager o Strider Jinete que despawnea se lleva a sus jinetes: los jinetes no despawnean nunca y
     * quedaban sueltos (Jess es aldeana, el Piglin Brute no despawnea). Se mira al tick siguiente para no borrar
     * entidades dentro del evento.
     */
    @EventHandler
    public void onUltraRavagerDespawn(org.bukkit.event.entity.EntityRemoveEvent e) {
        org.bukkit.persistence.PersistentDataContainer pdc = e.getEntity().getPersistentDataContainer();
        if (e.getCause() != org.bukkit.event.entity.EntityRemoveEvent.Cause.DESPAWN
                || !("ultra_ravager".equals(pdc.get(key("day40_boss"), org.bukkit.persistence.PersistentDataType.STRING))
                        || pdc.has(key("rider_mount")))) return;
        List<org.bukkit.entity.Entity> riders = new java.util.ArrayList<>();
        for (org.bukkit.entity.Entity carlos : e.getEntity().getPassengers()) {
            riders.add(carlos);
            riders.addAll(carlos.getPassengers());
        }
        Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> riders.forEach(org.bukkit.entity.Entity::remove));
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
