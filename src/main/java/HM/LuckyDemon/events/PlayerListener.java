package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.utils.MessageUtils;
import HM.LuckyDemon.utils.WebhookUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.managers.ScoreboardManager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {

    // Set para rastrear jugadores que ya vieron el mensaje de PvP
    private final Set<UUID> pvpMessageShown = new HashSet<>();
    private final java.util.Map<org.bukkit.Location, Long> transformCooldowns = new java.util.HashMap<>();
    private final java.util.Random random = new java.util.Random();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(MessageUtils.format("<gray>[<green>+<gray>] <yellow>" + e.getPlayer().getName()));
        updateHealth(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(MessageUtils.format("<gray>[<red>-<gray>] <yellow>" + e.getPlayer().getName()));
        // Limpiar el registro cuando el jugador sale
        pvpMessageShown.remove(e.getPlayer().getUniqueId());
    }

    // Aplicar scoreboard de vida
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        ScoreboardManager.getInstance().applyScoreboard(player);
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent e) {
        // Verificar si se puede saltar la noche según el día
        if (!HMPlugin.getInstance().getDifficultyManager().canSkipNight()) {
            e.setCancelled(true);
            // Resetear el contador de phantoms aunque no se pueda dormir
            e.getPlayer().setStatistic(org.bukkit.Statistic.TIME_SINCE_REST, 0);
            MessageUtils.send(e.getPlayer(),
                    "<red>☠ La noche no se puede saltar... <gray>(Pero te has relajado, <green>los Phantoms se han reiniciado<gray>)");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.8f);
            return;
        }

        if (e.getPlayer().getWorld().hasStorm()) {
            e.setCancelled(true);
            e.getPlayer().setStatistic(org.bukkit.Statistic.TIME_SINCE_REST, 0);
            MessageUtils.send(e.getPlayer(),
                    "<aqua>El estruendo de la tormenta no te deja dormir... <gray>(Pero te has relajado, <green>los Phantoms se han reiniciado<gray>)");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.8f);
            return;
        }

        // Verificar jugadores mínimos requeridos según dificultad
        int minPlayers = HMPlugin.getInstance().getDifficultyManager().getMinimumPlayersRequired();
        long onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                .count();

        if (onlinePlayers < minPlayers) {
            e.setCancelled(true);
            MessageUtils.send(e.getPlayer(),
                    "<red>⚠ Se requieren al menos <bold>" + minPlayers
                            + " jugadores</bold> en línea para pasar la noche.");
            MessageUtils.send(e.getPlayer(),
                    "<gray>Actualmente hay <yellow>" + onlinePlayers + "<gray> jugador(es) conectado(s).");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onEnderEyePlace(PlayerInteractEvent e) {
        // Verificar si el jugador está haciendo clic derecho
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Verificar si el bloque clickeado es un End Portal Frame
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        // Verificar si el jugador tiene un Ender Eye en la mano
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) {
            return;
        }

        // Verificar el día actual
        int currentDay = GameManager.getInstance().getDay();

        // Bloquear hasta el día 29 (solo permitir desde el día 30)
        if (currentDay < 30) {
            e.setCancelled(true);
            MessageUtils.send(e.getPlayer(),
                    "<red>⚠ El portal al End está bloqueado hasta el <bold>Día 30</bold>.");
            MessageUtils.send(e.getPlayer(),
                    "<gray>Día actual: <yellow>" + currentDay + "<gray>/30");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        // DÍA 40+: Fuego amigo habilitado (PvP permitido)
        int currentDay = HM.LuckyDemon.managers.GameManager.getInstance().getDay();

        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            if (currentDay >= 40) {
                // PvP habilitado en día 40+
                Player attacker = (Player) e.getDamager();
                Player victim = (Player) e.getEntity();

                // Mostrar mensaje la primera vez que atacan
                if (!pvpMessageShown.contains(attacker.getUniqueId())) {
                    MessageUtils.send(attacker,
                            "<red>⚔ <gold>¡FUEGO AMIGO HABILITADO! <gray>Ten cuidado con tus ataques...");
                    pvpMessageShown.add(attacker.getUniqueId());
                }

                // NO cancelar - permitir el daño
                return;
            }

            // Antes del día 40: Cancelar PvP
            e.setCancelled(true);
            Player attacker = (Player) e.getDamager();

            // Mostrar mensaje solo la primera vez en la sesión
            if (!pvpMessageShown.contains(attacker.getUniqueId())) {
                MessageUtils.send(attacker, "<red>⚔ El PvP está deshabilitado en este servidor.");
                pvpMessageShown.add(attacker.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        // SKIP mobs marcados
        NamespacedKey skipKey = new NamespacedKey(HMPlugin.getInstance(), "skip_effects");
        NamespacedKey transformedKey = new NamespacedKey(HMPlugin.getInstance(), "transformed_mob");

        if (e.getEntity().getPersistentDataContainer().has(skipKey, PersistentDataType.BYTE)) {
            return;
        }

        if (e.getEntity().getPersistentDataContainer().has(transformedKey, PersistentDataType.BYTE)) {
            return;
        }

        // IMPORTANTE: Ignorar spawns personalizados (CUSTOM)
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }

        int currentDay = GameManager.getInstance().getDay();

        // DÍA 30+: Sistema de transformación con cooldown
        if (currentDay >= 30) {
            Location loc = e.getLocation();

            // Limpiar cooldowns antiguos (más de 5 segundos)
            transformCooldowns.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 5000);

            // Verificar cooldown en esta ubicación
            String locKey = String.format("%d_%d_%d",
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            // Bat -> 30% Blaze, 70% cancelar
            if (e.getEntity() instanceof Bat) {
                // Verificar si ya hubo una transformación reciente aquí
                boolean onCooldown = transformCooldowns.values().stream()
                        .anyMatch(time -> System.currentTimeMillis() - time < 1000);

                if (onCooldown) {
                    e.setCancelled(true);
                    return;
                }

                if (random.nextInt(100) < 30) {
                    // 30% transformar
                    e.setCancelled(true);
                    transformCooldowns.put(loc.clone(), System.currentTimeMillis());

                    Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                        Blaze blaze = (Blaze) loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
                        blaze.getPersistentDataContainer().set(transformedKey, PersistentDataType.BYTE, (byte) 1);
                        blaze.getPersistentDataContainer().set(skipKey, PersistentDataType.BYTE, (byte) 1);
                        blaze.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
                        blaze.customName(net.kyori.adventure.text.Component.text("Blaze Infernal")
                                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                        blaze.setCustomNameVisible(false);
                    }, 5L);
                } else {
                    // 70% cancelar
                    e.setCancelled(true);
                }
                return;
            }

            // Squid -> 30% Guardian, 70% cancelar
            if (e.getEntity() instanceof Squid) {
                boolean onCooldown = transformCooldowns.values().stream()
                        .anyMatch(time -> System.currentTimeMillis() - time < 1000);

                if (onCooldown) {
                    e.setCancelled(true);
                    return;
                }

                if (random.nextInt(100) < 30) {
                    // 30% transformar
                    e.setCancelled(true);
                    transformCooldowns.put(loc.clone(), System.currentTimeMillis());

                    Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                        Guardian guardian = (Guardian) loc.getWorld().spawnEntity(loc, EntityType.GUARDIAN);
                        guardian.getPersistentDataContainer().set(transformedKey, PersistentDataType.BYTE, (byte) 1);
                        guardian.getPersistentDataContainer().set(skipKey, PersistentDataType.BYTE, (byte) 1);
                        guardian.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                        guardian.customName(net.kyori.adventure.text.Component.text("Guardián Acuático")
                                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                        guardian.setCustomNameVisible(false);
                    }, 5L);
                } else {
                    // 70% cancelar
                    e.setCancelled(true);
                }
                return;
            }
        }

        // DÍA 25+: Marcar slimes de división
        if (currentDay >= 25) {
            if (e.getEntity() instanceof Slime) {
                Slime slime = (Slime) e.getEntity();

                if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) {
                    Slime parent = findParentSlime(slime);

                    if (parent != null) {
                        if (slime instanceof MagmaCube) {
                            NamespacedKey gigaKey = new NamespacedKey(HMPlugin.getInstance(), "giga_magma");
                            if (parent.getPersistentDataContainer().has(gigaKey, PersistentDataType.BYTE)) {
                                slime.getPersistentDataContainer().set(gigaKey, PersistentDataType.BYTE, (byte) 1);
                            }
                            slime.getPersistentDataContainer().set(
                                    new NamespacedKey(HMPlugin.getInstance(), "magma_division"),
                                    PersistentDataType.BYTE, (byte) 1);
                        } else {
                            NamespacedKey gigaKey = new NamespacedKey(HMPlugin.getInstance(), "giga_slime");
                            if (parent.getPersistentDataContainer().has(gigaKey, PersistentDataType.BYTE)) {
                                slime.getPersistentDataContainer().set(gigaKey, PersistentDataType.BYTE, (byte) 1);
                            }
                            slime.getPersistentDataContainer().set(
                                    new NamespacedKey(HMPlugin.getInstance(), "slime_division"),
                                    PersistentDataType.BYTE, (byte) 1);
                        }
                    }
                }
            }
        }

        // SPAWNS ESPECIALES EN EL END (DÍA 30+)
        if (currentDay >= 30 && e.getLocation().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            // Spawn Ender Ghast (2% probabilidad)
            if (random.nextInt(100) < 2 && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                Location loc = e.getLocation();
                e.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                    Ghast enderGhast = (Ghast) loc.getWorld().spawnEntity(loc, EntityType.GHAST);
                    HMPlugin.getInstance().getDifficultyManager().applyMobEffects(enderGhast);
                }, 2L);
                return;
            }

            // Spawn Ender Creeper (10% probabilidad cuando spawneaCREEPER o realizado
            // manualmente)
            if (random.nextInt(100) < 10 && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                Location loc = e.getLocation();
                e.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                    Creeper enderCreeper = (Creeper) loc.getWorld().spawnEntity(loc, EntityType.CREEPER);
                    HMPlugin.getInstance().getDifficultyManager().applyMobEffects(enderCreeper);
                }, 2L);
                return;
            }
        }

        // Aplicar efectos normales con delay
        Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
            if (e.getEntity().isValid() && !e.getEntity().isDead()) {
                HMPlugin.getInstance().getDifficultyManager().applyMobEffects(e.getEntity());
            }
        }, 1L);
    }

    /**
     * Encontrar el slime padre más cercano (para heredar marcadores)
     */
    private org.bukkit.entity.Slime findParentSlime(org.bukkit.entity.Slime child) {
        // Buscar slimes cercanos (radio de 5 bloques)
        for (org.bukkit.entity.Entity nearby : child.getNearbyEntities(5, 5, 5)) {
            if (nearby instanceof org.bukkit.entity.Slime) {
                org.bukkit.entity.Slime potentialParent = (org.bukkit.entity.Slime) nearby;

                // El padre debe ser del mismo tipo y más grande
                if (potentialParent.getClass().equals(child.getClass()) &&
                        potentialParent.getSize() > child.getSize()) {
                    return potentialParent;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        // Verificar si la entidad debe soltar drops según el día actual
        if (!HMPlugin.getInstance().getDifficultyManager().shouldDropItems(e.getEntity())) {
            e.getDrops().clear();
            e.setDroppedExp(0);
        }

        // Ravager especial con Tótem de Inmortalidad
        if (e.getEntity() instanceof org.bukkit.entity.Ravager) {
            org.bukkit.entity.Ravager ravager = (org.bukkit.entity.Ravager) e.getEntity();

            // Verificar si tiene chance de drop configurado (Día 25+)
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(HMPlugin.getInstance(), "totem_drop_chance");
            if (ravager.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                int chance = ravager.getPersistentDataContainer().get(key,
                        org.bukkit.persistence.PersistentDataType.INTEGER);

                if (new java.util.Random().nextInt(100) < chance) {
                    e.getDrops().add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING));
                }
            }
            // Compatibilidad con día 20 (1% chance si tiene nombre especial)
            else if (ravager.customName() != null && ravager.customName().toString().contains("Ravager Especial")) {
                if (new java.util.Random().nextInt(100) == 0) {
                    e.getDrops().add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING));
                }
            }
        }

        // DÍA 25+: Drops de armadura de Netherite especial
        if (HMPlugin.getInstance().getDifficultyManager().isDay25OrLater()) {
            handleDay25ArmorDrops(e);
        }
    }

    @EventHandler
    public void onShulkerDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.Shulker)) {
            return;
        }

        org.bukkit.entity.Shulker shulker = (org.bukkit.entity.Shulker) e.getEntity();
        org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                "explosive_shulker");

        // Verificar si es un Shulker Explosivo
        if (shulker.getPersistentDataContainer().has(explosiveKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
            // Limpiar drops normales (para evitar duplicados)
            e.getDrops().clear();

            // Spawnear TNT activa en la ubicación del shulker
            org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) shulker.getWorld()
                    .spawnEntity(shulker.getLocation(), org.bukkit.entity.EntityType.TNT);
            tnt.setFuseTicks(40); // 2 segundos de fusión

            // 20% drop de Shulker Shell NORMAL pero protegida de explosiones
            if (new Random().nextInt(100) < 20) {
                // Crear item normal
                org.bukkit.inventory.ItemStack shulkerShell = new org.bukkit.inventory.ItemStack(
                        org.bukkit.Material.SHULKER_SHELL, 1);

                // Marcar el item como protegido de explosiones
                org.bukkit.inventory.meta.ItemMeta meta = shulkerShell.getItemMeta();
                if (meta != null) {
                    org.bukkit.NamespacedKey protectedKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                            "explosion_proof");
                    meta.getPersistentDataContainer().set(protectedKey, org.bukkit.persistence.PersistentDataType.BYTE,
                            (byte) 1);
                    shulkerShell.setItemMeta(meta);
                }

                // Dropear el item
                shulker.getWorld().dropItemNaturally(shulker.getLocation(), shulkerShell);
            }
        }
    }

    @EventHandler
    public void onAllayAttackPlayer(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        // Verificar si el atacante es un Allay
        if (!(e.getDamager() instanceof org.bukkit.entity.Allay)) {
            return;
        }

        org.bukkit.entity.Allay allay = (org.bukkit.entity.Allay) e.getDamager();
        org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                "explosive_allay");

        // Verificar si es un Allay Explosivo
        if (allay.getPersistentDataContainer().has(explosiveKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
            // Cancelar el daño normal
            e.setCancelled(true);

            // Spawnear TNT activa en la ubicación del allay
            org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) allay.getWorld()
                    .spawnEntity(allay.getLocation(), org.bukkit.entity.EntityType.TNT);
            tnt.setFuseTicks(1); // Explota casi instantáneamente

            // Remover el allay (se sacrifica)
            allay.remove();
        }
    }

    @EventHandler
    public void onItemDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        // Solo procesar items
        if (!(e.getEntity() instanceof org.bukkit.entity.Item)) {
            return;
        }

        // Solo proteger de explosiones
        if (e.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && e.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        org.bukkit.entity.Item item = (org.bukkit.entity.Item) e.getEntity();
        org.bukkit.inventory.ItemStack itemStack = item.getItemStack();

        // Verificar si el item tiene la marca de protección
        if (itemStack.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = itemStack.getItemMeta();
            org.bukkit.NamespacedKey protectedKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "explosion_proof");

            if (meta.getPersistentDataContainer().has(protectedKey,
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                // Cancelar el daño (el item no desaparece)
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onShulkerBulletHit(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        // Verificar si el daño es causado por un proyectil de Shulker
        if (!(e.getDamager() instanceof org.bukkit.entity.ShulkerBullet)) {
            return;
        }

        org.bukkit.entity.ShulkerBullet bullet = (org.bukkit.entity.ShulkerBullet) e.getDamager();

        // Verificar si el proyectil fue lanzado por un Shulker Explosivo
        if (bullet.getShooter() instanceof org.bukkit.entity.Shulker) {
            org.bukkit.entity.Shulker shulker = (org.bukkit.entity.Shulker) bullet.getShooter();
            org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "explosive_shulker");

            if (shulker.getPersistentDataContainer().has(explosiveKey,
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                // Solo spawnear TNT si golpea a un jugador
                if (e.getEntity() instanceof Player) {
                    org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) bullet.getWorld()
                            .spawnEntity(bullet.getLocation(), org.bukkit.entity.EntityType.TNT);
                    tnt.setFuseTicks(40); // 2 segundos de fusión

                    // Remover el proyectil
                    bullet.remove();
                }
            }
        }
    }

    @EventHandler
    public void onShulkerBulletDestroyed(org.bukkit.event.entity.ProjectileHitEvent e) {
        // Verificar si es un proyectil de Shulker
        if (!(e.getEntity() instanceof org.bukkit.entity.ShulkerBullet)) {
            return;
        }

        org.bukkit.entity.ShulkerBullet bullet = (org.bukkit.entity.ShulkerBullet) e.getEntity();

        // Verificar si el proyectil fue lanzado por un Shulker Explosivo
        if (bullet.getShooter() instanceof org.bukkit.entity.Shulker) {
            org.bukkit.entity.Shulker shulker = (org.bukkit.entity.Shulker) bullet.getShooter();
            org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "explosive_shulker");

            if (shulker.getPersistentDataContainer().has(explosiveKey,
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                // Siempre spawnear TNT cuando el proyectil impacta (sin importar qué golpee)
                org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) bullet.getWorld()
                        .spawnEntity(bullet.getLocation(), org.bukkit.entity.EntityType.TNT);
                tnt.setFuseTicks(40); // 2 segundos de fusión
            }
        }
    }

    @EventHandler
    public void onShulkerBulletDamaged(org.bukkit.event.entity.EntityDamageEvent e) {
        // Verificar si es un proyectil de Shulker siendo destruido
        if (!(e.getEntity() instanceof org.bukkit.entity.ShulkerBullet)) {
            return;
        }

        org.bukkit.entity.ShulkerBullet bullet = (org.bukkit.entity.ShulkerBullet) e.getEntity();

        // Verificar si el proyectil fue lanzado por un Shulker Explosivo
        if (bullet.getShooter() instanceof org.bukkit.entity.Shulker) {
            org.bukkit.entity.Shulker shulker = (org.bukkit.entity.Shulker) bullet.getShooter();
            org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "explosive_shulker");

            if (shulker.getPersistentDataContainer().has(explosiveKey,
                    org.bukkit.persistence.PersistentDataType.BYTE)) {
                // Spawnear TNT antes de que el proyectil desaparezca
                org.bukkit.entity.TNTPrimed tnt = (org.bukkit.entity.TNTPrimed) bullet.getWorld()
                        .spawnEntity(bullet.getLocation(), org.bukkit.entity.EntityType.TNT);
                tnt.setFuseTicks(40); // 2 segundos de fusión
            }
        }
    }

    @EventHandler
    public void onEnderGhastArrowHit(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        // Solo procesar daño por flechas
        if (!(e.getDamager() instanceof org.bukkit.entity.Arrow)) {
            return;
        }

        // Solo procesar si es un Ghast
        if (!(e.getEntity() instanceof org.bukkit.entity.Ghast)) {
            return;
        }

        org.bukkit.entity.Ghast ghast = (org.bukkit.entity.Ghast) e.getEntity();
        org.bukkit.NamespacedKey enderGhastKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                "ender_ghast");

        // Verificar si es un Ender Ghast
        if (ghast.getPersistentDataContainer().has(enderGhastKey,
                org.bukkit.persistence.PersistentDataType.BYTE)) {
            // Teletransportar 20% de las veces
            Random random = new Random();
            if (random.nextInt(100) < 20) {
                Location teleportLoc = ghast.getLocation().add(
                        (random.nextDouble() - 0.5) * 30,
                        random.nextDouble() * 15,
                        (random.nextDouble() - 0.5) * 30);
                ghast.teleport(teleportLoc);
                ghast.getWorld().playSound(ghast.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onGhastFireballDamage(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        // Solo procesar daño por Fireball
        if (!(e.getDamager() instanceof org.bukkit.entity.Fireball)) {
            return;
        }

        org.bukkit.entity.Fireball fireball = (org.bukkit.entity.Fireball) e.getDamager();

        // Verificar si fue lanzada por un Ghast especial
        if (fireball.getShooter() instanceof org.bukkit.entity.Ghast) {
            org.bukkit.entity.Ghast ghast = (org.bukkit.entity.Ghast) fireball.getShooter();
            org.bukkit.NamespacedKey explosionKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "explosion_power");

            // Si tiene ExplosionPower configurado, multiplicar el daño
            if (ghast.getPersistentDataContainer().has(explosionKey,
                    org.bukkit.persistence.PersistentDataType.INTEGER)) {
                int explosionPower = ghast.getPersistentDataContainer().get(explosionKey,
                        org.bukkit.persistence.PersistentDataType.INTEGER);

                // Multiplicar el daño base según el ExplosionPower
                // ExplosionPower 8-10 = multiplicador de 1.6-2x
                double multiplier = 1.6 + (explosionPower / 2.0);
                e.setDamage(e.getDamage() * multiplier);
            }
        }
    }

    @EventHandler
    public void onEnderGhastFireball(org.bukkit.event.entity.ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.Fireball)) {
            return;
        }

        org.bukkit.entity.Fireball fireball = (org.bukkit.entity.Fireball) e.getEntity();

        // Verificar si fue lanzada por un Ghast
        if (fireball.getShooter() instanceof org.bukkit.entity.Ghast) {
            org.bukkit.entity.Ghast ghast = (org.bukkit.entity.Ghast) fireball.getShooter();
            org.bukkit.NamespacedKey explosionKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                    "explosion_power");

            // Verificar si el Ghast tiene ExplosionPower configurado
            if (ghast.getPersistentDataContainer().has(explosionKey,
                    org.bukkit.persistence.PersistentDataType.INTEGER)) {
                int explosionPower = ghast.getPersistentDataContainer().get(explosionKey,
                        org.bukkit.persistence.PersistentDataType.INTEGER);

                // Usar NMS/Reflexión para establecer el ExplosionPower
                try {
                    // Obtener el handle de la fireball (NMS)
                    Object nmsFireball = fireball.getClass().getMethod("getHandle").invoke(fireball);

                    // Establecer el explosionPower usando reflexión
                    nmsFireball.getClass().getField("explosionPower").setInt(nmsFireball, explosionPower);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        String victim = player.getName();
        Location deathLocation = player.getLocation();

        Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
            player.spigot().respawn();
        }, 1L);

        HM.LuckyDemon.managers.LivesManager livesManager = HM.LuckyDemon.managers.LivesManager.getInstance();
        int remainingLives = livesManager.removeLife(player);

        // Crear monumento de muerte solo cuando el jugador pierde todas sus vidas
        if (remainingLives == 0) {
            createDeathMonument(player, deathLocation);
        }

        // Determinar qué vida se perdió (3 - remaining = vida perdida)
        int lostLifeNumber = livesManager.getMaxLives() - remainingLives;

        // Obtener el mensaje correspondiente
        String customMessage = null;
        if (lostLifeNumber == 1) {
            customMessage = HMPlugin.getInstance().getConfig()
                    .getString("death_messages." + player.getUniqueId().toString() + ".life1", null);
        } else if (lostLifeNumber == 2) {
            customMessage = HMPlugin.getInstance().getConfig()
                    .getString("death_messages." + player.getUniqueId().toString() + ".life2", null);
        } else if (lostLifeNumber == 3) {
            // Mensaje predeterminado para la tercera vida (final)
            customMessage = "Este es tu final... no hay vuelta atrás.";
        }

        // Obtener la causa de muerte ANTES de modificar el mensaje
        Component originalDeathMessage = e.deathMessage();
        String deathCauseText = "";
        if (originalDeathMessage != null) {
            String plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(originalDeathMessage);
            // Extraer solo la parte después del nombre del jugador (la causa)
            if (plainText.contains(victim)) {
                deathCauseText = plainText.substring(plainText.indexOf(victim) + victim.length()).trim();
            }
        }

        Component deathMsg;
        if (customMessage != null && !customMessage.isEmpty()) {
            deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim
                    + " " + deathCauseText + ". <yellow>Vidas restantes: " + remainingLives + "/"
                    + livesManager.getMaxLives()
                    + "<br><gray><!bold><italic>\"" + customMessage + "\"");
        } else {
            deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim
                    + " " + deathCauseText + ". <yellow>Vidas restantes: " + remainingLives + "/"
                    + livesManager.getMaxLives());
        }
        e.deathMessage(deathMsg);

        // Obtener causa de muerte
        Component deathMessageComponent = e.deathMessage();
        String deathCause = deathMessageComponent != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(deathMessageComponent)
                : "Causa desconocida";

        // Enviar al webhook
        WebhookUtils.sendDeathNotification(
                player,
                deathCause,
                remainingLives,
                livesManager.getMaxLives(),
                customMessage != null ? customMessage : "");

        // Mostrar título en la pantalla de TODOS los jugadores
        net.kyori.adventure.title.Title deathTitle = net.kyori.adventure.title.Title.title(
                MessageUtils.format("<red><bold>¡Permadeath!"),
                MessageUtils.format("<gray>" + victim + " ha muerto"),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(1000)));
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showTitle(deathTitle);
        }

        World world = player.getWorld();
        world.strikeLightningEffect(player.getLocation());
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        // Usar el nuevo sistema de horas que resetea cada 25 días
        int stormHours = HMPlugin.getInstance().getDifficultyManager().getStormHoursForCurrentDay();
        int addedSeconds = stormHours * 3600;
        int addedTicks = addedSeconds * 20;

        if (world.hasStorm()) {
            int currentDuration = world.getWeatherDuration();
            world.setWeatherDuration(currentDuration + addedTicks);
            world.setThunderDuration(currentDuration + addedTicks); // Extender también los rayos
            Bukkit.broadcast(MessageUtils.format("<blue>⛈ ¡La tormenta se ha extendido <bold>"
                    + MessageUtils.formatTime(addedSeconds) + "<reset><blue> más!"));
        } else {
            world.setStorm(true);
            world.setThundering(true); // ← ACTIVAR RAYOS
            world.setWeatherDuration(addedTicks);
            world.setThunderDuration(addedTicks); // ← DURACIÓN DE LOS RAYOS
            Bukkit.broadcast(MessageUtils.format("<dark_aqua>⛈ ¡Ha comenzado una tormenta de <bold>"
                    + MessageUtils.formatTime(addedSeconds) + "<reset><dark_aqua>!"));
        }

        Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
            player.setGameMode(GameMode.SPECTATOR);

            if (remainingLives > 0) {
                MessageUtils.send(player, "<yellow>Volverás al juego en 5 segundos... (<green>" + remainingLives
                        + " vidas restantes<yellow>)");

                Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);

                    int maxLives = livesManager.getMaxLives();
                    StringBuilder heartsDisplay = new StringBuilder();
                    for (int i = 0; i < maxLives; i++) {
                        if (i < remainingLives) {
                            heartsDisplay.append("<red>❤ ");
                        } else {
                            heartsDisplay.append("<gray>❤ ");
                        }
                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

                    player.showTitle(net.kyori.adventure.title.Title.title(
                            MessageUtils.format("<gradient:red:dark_red><bold>-1 VIDA</gradient>"),
                            MessageUtils.format(heartsDisplay.toString() + " <yellow>" + remainingLives + "/" + maxLives
                                    + " vidas"),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(2500),
                                    java.time.Duration.ofMillis(1000))));

                    MessageUtils.send(player, "<green>¡Has regresado al juego! <gray>Te quedan <yellow>"
                            + remainingLives + " vidas<gray>.");
                }, 100L);
            } else {
                MessageUtils.send(player, "<dark_red><bold>¡TE HAS QUEDADO SIN VIDAS!");

                if (HMPlugin.getInstance().getConfig().getBoolean("game.ban_enabled", true)) {
                    Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                        player.kick(MessageUtils.format(
                                "<red><bold>TE QUEDASTE SIN VIDAS<br><br><yellow>Has perdido todas tus vidas.<br><gray>Contacta a un administrador para volver."));
                    }, 100L);
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (item.getType() != Material.GOLDEN_APPLE)
            return;
        if (!item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        Player p = e.getPlayer();

        NamespacedKey key = new NamespacedKey(HMPlugin.getInstance(), "gap_type");

        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING))
            return;

        String gapType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if ("hyper".equals(gapType)) {
            AttributeInstance attribute = p.getAttribute(Attribute.MAX_HEALTH);

            if (attribute != null) {
                double currentMax = attribute.getValue(); // getValue incluye modificadores

                if (currentMax < 40.0) {
                    // Crear modificador único para este jugador
                    java.util.UUID modifierUUID = java.util.UUID.nameUUIDFromBytes(
                            ("hyper_apple_" + p.getUniqueId().toString() + "_" + System.currentTimeMillis())
                                    .getBytes());

                    org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                            modifierUUID,
                            "hmplugin.hyper_apple",
                            4.0, // Agregar 4 puntos (2 corazones)
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
                    attribute.addModifier(modifier);

                    p.sendMessage(MessageUtils.format("<green>¡Has ganado contenedores de vida extra!"));
                } else {
                    p.sendMessage(MessageUtils.format("<red>Ya has alcanzado el límite de vida extra."));
                }
            }

            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 30, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 2));

        }
    }

    private void updateHealth(Player p) {
        AttributeInstance attribute = p.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null && attribute.getBaseValue() < 20.0) {
            attribute.setBaseValue(20.0);
        }
    }

    /**
     * Crea un monumento de muerte en la ubicación donde murió el jugador.
     * Solo se crea cuando el jugador pierde todas sus vidas.
     * Estructura: Bedrock reemplaza el bloque de abajo, valla del nether en la
     * posición de muerte, cabeza arriba.
     */
    private void createDeathMonument(Player player, Location location) {
        Location loc = location.getBlock().getLocation();

        // Bedrock reemplaza el bloque de abajo (-1)
        Block bedrockBlock = loc.clone().add(0, -1, 0).getBlock();
        bedrockBlock.setType(Material.BEDROCK);

        // Valla del nether en la posición donde murió
        Block fenceBlock = loc.getBlock();
        fenceBlock.setType(Material.NETHER_BRICK_FENCE);

        // Cabeza del jugador encima de la valla (+1)
        Block skullBlock = loc.clone().add(0, 1, 0).getBlock();
        skullBlock.setType(Material.PLAYER_HEAD);

        // Configurar la cabeza con el skin del jugador
        if (skullBlock.getState() instanceof Skull) {
            Skull skull = (Skull) skullBlock.getState();
            skull.setOwningPlayer((OfflinePlayer) player);
            skull.update();
        }
    }

    /**
     * DÍA 25-29: Manejar drops de armadura de Netherite especial
     * A partir del día 30, estos mobs ya no dropean armadura
     */
    private void handleDay25ArmorDrops(EntityDeathEvent e) {
        // Solo permitir drops entre día 25 y 29
        int currentDay = GameManager.getInstance().getDay();
        if (currentDay >= 30) {
            return; // No dropear armadura a partir del día 30
        }

        java.util.Random random = new java.util.Random();
        ItemStack armorPiece = null;
        int dropChance = 30; // Probabilidad por defecto: 30%

        // Ghast Demoníaco → Botas de Netherite (30%)
        if (e.getEntity() instanceof org.bukkit.entity.Ghast) {
            org.bukkit.entity.Ghast ghast = (org.bukkit.entity.Ghast) e.getEntity();

            // Verificar si es un Ghast Demoníaco
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(HMPlugin.getInstance(), "demonic_ghast");
            if (ghast.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                dropChance = 30;
                if (random.nextInt(100) < dropChance) {
                    armorPiece = createSpecialNetheriteArmor(Material.NETHERITE_BOOTS, "Botas Infernales");
                }
            }
        }

        // Giga Slime → Pechera de Netherite (20%, solo en tamaño 1)
        else if (e.getEntity() instanceof org.bukkit.entity.Slime
                && !(e.getEntity() instanceof org.bukkit.entity.MagmaCube)) {
            org.bukkit.entity.Slime slime = (org.bukkit.entity.Slime) e.getEntity();

            // Verificar si es un Giga Slime Y está en tamaño 1 (más pequeño)
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(HMPlugin.getInstance(), "giga_slime");
            if (slime.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)
                    && slime.getSize() == 1) {
                dropChance = 20; // 20% para Giga Slimes
                if (random.nextInt(100) < dropChance) {
                    armorPiece = createSpecialNetheriteArmor(Material.NETHERITE_CHESTPLATE, "Pechera Infernal");
                }
            }
        }

        // Giga Magma Cube → Pantalones de Netherite (20%, solo en tamaño 1)
        else if (e.getEntity() instanceof org.bukkit.entity.MagmaCube) {
            org.bukkit.entity.MagmaCube magmaCube = (org.bukkit.entity.MagmaCube) e.getEntity();

            // Verificar si es un Giga Magma Cube Y está en tamaño 1 (más pequeño)
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(HMPlugin.getInstance(), "giga_magma");
            if (magmaCube.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE)
                    && magmaCube.getSize() == 1) {
                dropChance = 20; // 20% para Giga Magma Cubes
                if (random.nextInt(100) < dropChance) {
                    armorPiece = createSpecialNetheriteArmor(Material.NETHERITE_LEGGINGS, "Pantalones Infernales");
                }
            }
        }

        // Araña de Cueva → Casco de Netherite (30%)
        else if (e.getEntity() instanceof org.bukkit.entity.CaveSpider) {
            dropChance = 30;
            if (random.nextInt(100) < dropChance) {
                armorPiece = createSpecialNetheriteArmor(Material.NETHERITE_HELMET, "Casco Infernal");
            }
        }

        // Agregar el drop si se creó
        if (armorPiece != null) {
            e.getDrops().add(armorPiece);
        }
    }

    /**
     * Crear pieza de armadura de Netherite especial (irrompible)
     */
    private ItemStack createSpecialNetheriteArmor(Material material, String displayName) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();

        if (meta != null) {
            // Nombre personalizado
            meta.displayName(
                    MessageUtils.format("<gradient:dark_purple:light_purple><bold>" + displayName + "</gradient>"));

            // Hacer irrompible
            meta.setUnbreakable(true);

            // Lore explicativo
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(MessageUtils.format("<gray>Armadura especial del Día 25+"));
            lore.add(MessageUtils.format("<dark_purple>Irrompible"));
            lore.add(MessageUtils.format(""));
            lore.add(MessageUtils.format("<gold>Set completo: <yellow>+4 ❤"));
            meta.lore(lore);

            // Marcar como armadura especial
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(HMPlugin.getInstance(), "infernal_armor");
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

            armor.setItemMeta(meta);
        }

        return armor;
    }

    /**
     * Verificar si el jugador tiene el set completo de armadura infernal
     */
    private boolean hasFullInfernalSet(Player player) {
        org.bukkit.inventory.EntityEquipment equipment = player.getEquipment();
        if (equipment == null)
            return false;

        ItemStack helmet = equipment.getHelmet();
        ItemStack chestplate = equipment.getChestplate();
        ItemStack leggings = equipment.getLeggings();
        ItemStack boots = equipment.getBoots();

        return isInfernalArmor(helmet) &&
                isInfernalArmor(chestplate) &&
                isInfernalArmor(leggings) &&
                isInfernalArmor(boots);
    }

    /**
     * Verificar si un item es armadura infernal
     */
    private boolean isInfernalArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;

        ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(HMPlugin.getInstance(), "infernal_armor");

        return meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    @EventHandler
    public void onShulkerNearby(org.bukkit.event.player.PlayerMoveEvent e) {
        // Solo ejecutar cada cierto tiempo para no sobrecargar
        if (e.getPlayer().getTicksLived() % 20 != 0) { // Cada segundo
            return;
        }

        Player player = e.getPlayer();
        int currentDay = HMPlugin.getInstance().getDifficultyManager().getCurrentDay();

        // Solo en día 30+
        if (currentDay < 30) {
            return;
        }

        // Buscar Shulkers cercanos (10 bloques)
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof org.bukkit.entity.Shulker) {
                org.bukkit.entity.Shulker shulker = (org.bukkit.entity.Shulker) entity;
                org.bukkit.NamespacedKey explosiveKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                        "explosive_shulker");
                org.bukkit.NamespacedKey processedKey = new org.bukkit.NamespacedKey(HMPlugin.getInstance(),
                        "processed_shulker");

                // Si no ha sido marcado como explosivo ni procesado
                if (!shulker.getPersistentDataContainer().has(explosiveKey,
                        org.bukkit.persistence.PersistentDataType.BYTE)
                        && !shulker.getPersistentDataContainer().has(processedKey,
                                org.bukkit.persistence.PersistentDataType.BYTE)) {

                    // Marcar como procesado para no repetir
                    shulker.getPersistentDataContainer().set(processedKey,
                            org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

                    // Aplicar efectos
                    HMPlugin.getInstance().getDifficultyManager().applyMobEffects(shulker);
                }
            }
        }
    }

    @EventHandler
    public void onEnderCreeperProjectileNear(org.bukkit.event.entity.ProjectileLaunchEvent e) {
        // Solo procesar flechas
        if (!(e.getEntity() instanceof org.bukkit.entity.Arrow)) {
            return;
        }

        org.bukkit.entity.Arrow arrow = (org.bukkit.entity.Arrow) e.getEntity();

        // Programar verificación cada tick
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // Si la flecha ya no existe o está en el suelo, cancelar
                if (!arrow.isValid() || arrow.isInBlock() || arrow.isDead()) {
                    this.cancel();
                    return;
                }

                // Buscar Ender Creepers cercanos (3 bloques)
                for (org.bukkit.entity.Entity nearby : arrow.getNearbyEntities(3, 3, 3)) {
                    if (nearby instanceof org.bukkit.entity.Creeper) {
                        org.bukkit.entity.Creeper creeper = (org.bukkit.entity.Creeper) nearby;
                        org.bukkit.NamespacedKey enderCreeperKey = new org.bukkit.NamespacedKey(
                                HMPlugin.getInstance(), "ender_creeper");

                        if (creeper.getPersistentDataContainer().has(enderCreeperKey,
                                org.bukkit.persistence.PersistentDataType.BYTE)) {
                            // Teletransportar a un lugar seguro
                            Random random = new Random();
                            Location currentLoc = creeper.getLocation();
                            Location teleportLoc = null;

                            // Intentar hasta 5 veces encontrar un lugar seguro
                            for (int i = 0; i < 5; i++) {
                                double offsetX = (random.nextDouble() - 0.5) * 16;
                                double offsetZ = (random.nextDouble() - 0.5) * 16;

                                Location testLoc = currentLoc.clone().add(offsetX, 0, offsetZ);

                                // Buscar el bloque sólido más cercano abajo
                                testLoc.setY(currentLoc.getY());
                                while (testLoc.getY() > currentLoc.getWorld().getMinHeight()
                                        && testLoc.getBlock().getType().isAir()) {
                                    testLoc.subtract(0, 1, 0);
                                }

                                // Verificar que hay un bloque sólido y espacio arriba
                                if (!testLoc.getBlock().getType().isAir()
                                        && testLoc.clone().add(0, 1, 0).getBlock().getType().isAir()
                                        && testLoc.clone().add(0, 2, 0).getBlock().getType().isAir()) {
                                    teleportLoc = testLoc.add(0, 1, 0);
                                    break;
                                }
                            }

                            // Si se encontró un lugar seguro, teletransportar
                            if (teleportLoc != null) {
                                creeper.teleport(teleportLoc);
                                creeper.getWorld().playSound(creeper.getLocation(),
                                        org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            }

                            this.cancel();
                        }
                    }
                }
            }
        }.runTaskTimer(HMPlugin.getInstance(), 0L, 1L);
    }
}