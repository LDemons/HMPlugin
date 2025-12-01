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
import org.bukkit.entity.Player;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import HM.LuckyDemon.managers.ScoreboardManager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {

    // Set para rastrear jugadores que ya vieron el mensaje de PvP
    private final Set<UUID> pvpMessageShown = new HashSet<>();

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
            int specialDay = HMPlugin.getInstance().getDifficultyManager().getSpecialRulesDay();
            MessageUtils.send(e.getPlayer(),
                    "<red>☠ A partir del día " + specialDay + ", la noche no se puede saltar. El ciclo continúa...");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.8f);
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
    public void onPvP(EntityDamageByEntityEvent e) {
        // Cancelar PvP entre jugadores
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            e.setCancelled(true);
            Player attacker = (Player) e.getDamager();

            // Mostrar mensaje solo la primera vez en la sesión
            if (!pvpMessageShown.contains(attacker.getUniqueId())) {
                MessageUtils.send(attacker, "<red>⚔ El PvP está deshabilitado en este servidor.");
                pvpMessageShown.add(attacker.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        // Verificar si es un slime/magma que viene de una división (día 25+)
        if (HM.LuckyDemon.managers.GameManager.getInstance().getDay() >= 25) {
            if (e.getEntity() instanceof org.bukkit.entity.Slime) {
                org.bukkit.entity.Slime slime = (org.bukkit.entity.Slime) e.getEntity();

                // Si el spawn es por SLIME_SPLIT, heredar marcadores del padre
                if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT) {
                    // Buscar el slime padre más cercano
                    org.bukkit.entity.Slime parent = findParentSlime(slime);

                    if (parent != null) {
                        // Heredar marcadores del padre
                        if (slime instanceof org.bukkit.entity.MagmaCube) {
                            // Verificar si el padre era un Giga Magma
                            NamespacedKey gigaKey = new NamespacedKey(HMPlugin.getInstance(), "giga_magma");
                            if (parent.getPersistentDataContainer().has(gigaKey, PersistentDataType.BYTE)) {
                                // Heredar el marcador de giga
                                slime.getPersistentDataContainer().set(gigaKey, PersistentDataType.BYTE, (byte) 1);
                            }

                            // Marcar como división
                            slime.getPersistentDataContainer().set(
                                    new NamespacedKey(HMPlugin.getInstance(), "magma_division"),
                                    PersistentDataType.BYTE,
                                    (byte) 1);
                        } else {
                            // Verificar si el padre era un Giga Slime
                            NamespacedKey gigaKey = new NamespacedKey(HMPlugin.getInstance(), "giga_slime");
                            if (parent.getPersistentDataContainer().has(gigaKey, PersistentDataType.BYTE)) {
                                // Heredar el marcador de giga
                                slime.getPersistentDataContainer().set(gigaKey, PersistentDataType.BYTE, (byte) 1);
                            }

                            // Marcar como división
                            slime.getPersistentDataContainer().set(
                                    new NamespacedKey(HMPlugin.getInstance(), "slime_division"),
                                    PersistentDataType.BYTE,
                                    (byte) 1);
                        }
                    }
                }
            }
        }

        // Delay de 1 tick para asegurar que el mob esté completamente cargado
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

        Component deathMsg;
        if (customMessage != null && !customMessage.isEmpty()) {
            deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim
                    + " ha muerto. <yellow>Vidas restantes: " + remainingLives + "/" + livesManager.getMaxLives()
                    + "<br><gray><!bold><italic>\"" + customMessage + "\"");
        } else {
            deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim
                    + " ha muerto. <yellow>Vidas restantes: " + remainingLives + "/" + livesManager.getMaxLives());
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

        // Mostrar título en la pantalla del jugador
        player.showTitle(net.kyori.adventure.title.Title.title(
                MessageUtils.format("<red><bold>¡Permadeath!"),
                MessageUtils.format("<gray>" + victim + " ha muerto"),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(1000))));

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
                double maxHealth = attribute.getBaseValue();

                if (maxHealth < 40.0) {
                    attribute.setBaseValue(maxHealth + 4.0);
                    p.sendMessage(MessageUtils.format("<green>¡Has ganado contenedores de vida extra!"));
                } else {
                    p.sendMessage(MessageUtils.format("<red>Ya has alcanzado el límite de vida extra."));
                }
            }

            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 30, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 2));

        } else if ("super".equals(gapType)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 15, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 300, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 300, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 1));
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
     * DÍA 25+: Manejar drops de armadura de Netherite especial
     */
    private void handleDay25ArmorDrops(EntityDeathEvent e) {
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
}