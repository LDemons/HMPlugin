package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPluggin;
import HM.LuckyDemon.utils.MessageUtils;
import HM.LuckyDemon.utils.WebhookUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(MessageUtils.format("<gray>[<green>+<gray>] <yellow>" + e.getPlayer().getName()));
        updateHealth(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(MessageUtils.format("<gray>[<red>-<gray>] <yellow>" + e.getPlayer().getName()));
    }

    // Modificación: Resetea Phantoms pero protege la tormenta
    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent e) {
        if (e.getPlayer().getWorld().hasStorm()) {
            // 1. Evitamos que duerma (para no quitar la tormenta)
            e.setCancelled(true);

            // 2. ¡TRUCO! Reseteamos la estadística de descanso manualmente
            // Esto hace que el juego crea que durmió y reinicia el contador de Phantoms a 0
            e.getPlayer().setStatistic(org.bukkit.Statistic.TIME_SINCE_REST, 0);

            // 3. Mensaje de feedback
            MessageUtils.send(e.getPlayer(),
                    "<aqua>El estruendo de la tormenta no te deja dormir... <gray>(Pero te has relajado, <green>los Phantoms se han reiniciado<gray>)");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.8f);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        String victim = player.getName();

        // Hacer que el jugador respawnee inmediatamente (sin pantalla de muerte)
        Bukkit.getScheduler().runTaskLater(HMPluggin.getInstance(), () -> {
            player.spigot().respawn();
        }, 1L);

        // 1. Sistema de Vidas
        HM.LuckyDemon.managers.LivesManager livesManager = HM.LuckyDemon.managers.LivesManager.getInstance();
        int remainingLives = livesManager.removeLife(player);

        // 2. Mensaje y Efectos
        String customMessage = HMPluggin.getInstance().getConfig()
                .getString("death_messages." + player.getUniqueId().toString(), null);

        Component deathMsg;
        if (customMessage != null && !customMessage.isEmpty()) {
            deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim
                    + " ha muerto. <yellow>Vidas restantes: " + remainingLives + "/" + livesManager.getMaxLives()
                    + "<br><gray><!bold><italic>" + victim + ", " + customMessage);
        } else {
            deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim
                    + " ha muerto. <yellow>Vidas restantes: " + remainingLives + "/" + livesManager.getMaxLives());
        }
        e.deathMessage(deathMsg);

        // Obtener causa de muerte
        Component deathMessageComponent = e.deathMessage();
        String deathCause = deathMessageComponent != null ? 
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(deathMessageComponent) : 
            "Causa desconocida";

        // Enviar al webhook
        WebhookUtils.sendDeathNotification(
            player, 
            deathCause, 
            remainingLives, 
            livesManager.getMaxLives(),
            customMessage != null ? customMessage : ""
        );

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

        // 3. Lógica de Tormenta (Death Train)
        int currentDay = HM.LuckyDemon.managers.GameManager.getInstance().getDay();
        int addedSeconds = currentDay * 3600; // 1 hora por día
        int addedTicks = addedSeconds * 20;

        if (world.hasStorm()) {
            int currentDuration = world.getWeatherDuration();
            world.setWeatherDuration(currentDuration + addedTicks);
            Bukkit.broadcast(MessageUtils.format("<blue>⛈ ¡La tormenta se ha extendido <bold>"
                    + MessageUtils.formatTime(addedSeconds) + "<reset><blue> más!"));
        } else {
            world.setStorm(true);
            world.setWeatherDuration(addedTicks);
            Bukkit.broadcast(MessageUtils.format("<dark_aqua>⛈ ¡Ha comenzado una tormenta de <bold>"
                    + MessageUtils.formatTime(addedSeconds) + "<reset><dark_aqua>!"));
        }

        // 4. Modo Espectador temporal o permanente
        Bukkit.getScheduler().runTaskLater(HMPluggin.getInstance(), () -> {
            player.setGameMode(GameMode.SPECTATOR);

            if (remainingLives > 0) {
                // Tiene vidas: volver a survival después de 5 segundos
                MessageUtils.send(player, "<yellow>Volverás al juego en 5 segundos... (<green>" + remainingLives
                        + " vidas restantes<yellow>)");

                Bukkit.getScheduler().runTaskLater(HMPluggin.getInstance(), () -> {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);

                    // Mostrar animación de corazones con el que perdió
                    int maxLives = livesManager.getMaxLives();

                    StringBuilder heartsDisplay = new StringBuilder();
                    for (int i = 0; i < maxLives; i++) {
                        if (i < remainingLives) {
                            heartsDisplay.append("❤ ");
                        } else {
                            heartsDisplay.append("🖤 "); // Corazón negro para las vidas perdidas
                        }
                    }

                    // Sonido dramático
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

                    // Mostrar título con animación de corazones
                    player.showTitle(net.kyori.adventure.title.Title.title(
                            MessageUtils.format("<gradient:red:dark_red><bold>-1 VIDA</gradient>"),
                            MessageUtils.format("<red>" + heartsDisplay.toString() + "<br><yellow>" + remainingLives
                                    + "/" + maxLives + " vidas"),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500), // Fade in
                                    java.time.Duration.ofMillis(2500), // Stay
                                    java.time.Duration.ofMillis(1000) // Fade out
                    )));

                    MessageUtils.send(player, "<green>¡Has regresado al juego! <gray>Te quedan <yellow>"
                            + remainingLives + " vidas<gray>.");
                }, 100L); // 5 segundos
            } else {
                // Sin vidas: kickear
                MessageUtils.send(player, "<dark_red><bold>¡TE HAS QUEDADO SIN VIDAS!");

                if (HMPluggin.getInstance().getConfig().getBoolean("game.ban_enabled", true)) {
                    Bukkit.getScheduler().runTaskLater(HMPluggin.getInstance(), () -> {
                        player.kick(MessageUtils.format(
                                "<red><bold>TE QUEDASTE SIN VIDAS<br><br><yellow>Has perdido todas tus vidas.<br><gray>Contacta a un administrador para volver."));
                    }, 100L); // 5 segundos de retraso
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

        NamespacedKey key = new NamespacedKey(HMPluggin.getInstance(), "gap_type");

        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING))
            return;

        String gapType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if ("hyper".equals(gapType)) {
            AttributeInstance attribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);

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
        AttributeInstance attribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null && attribute.getBaseValue() < 20.0) {
            attribute.setBaseValue(20.0);
        }
    }
}