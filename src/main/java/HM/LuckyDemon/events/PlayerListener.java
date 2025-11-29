package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPluggin;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        String victim = player.getName();

        // Mensaje de muerte
        Component deathMsg = MessageUtils.format("<red><bold>PERMADEATH <gray>» <red>" + victim + " ha muerto. <dark_red><bold>¡HA SIDO PERMABANEADO!");
        e.deathMessage(deathMsg);

        // Efectos
        World world = player.getWorld();
        world.strikeLightningEffect(player.getLocation());
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        // Lógica de BAN
        if (HMPluggin.getInstance().getConfig().getBoolean("game.ban_enabled", true)) {
            Bukkit.getScheduler().runTask(HMPluggin.getInstance(), () -> {
                // Usamos BanList.Type (Estándar de Bukkit) para máxima compatibilidad y evitar errores de import
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                        player.getName(),
                        "<red><bold>HAS MUERTO EN PERMADEATH<br><br><gray>Fuiste eliminado del evento.",
                        null, // Null = Permanente
                        "Permadeath System"
                );

                player.kick(MessageUtils.format("<red><bold>¡PERMABANEADO!<br><br><gray>Has muerto en Permadeath."));
            });
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (item.getType() != Material.GOLDEN_APPLE) return;
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        Player p = e.getPlayer();

        NamespacedKey key = new NamespacedKey(HMPluggin.getInstance(), "gap_type");

        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        String gapType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if ("hyper".equals(gapType)) {
            AttributeInstance attribute = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);

            // Verificación de seguridad para evitar NullPointerException (Warning amarillo)
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
        // Verificación de seguridad para evitar NullPointerException (Warning amarillo)
        if (attribute != null && attribute.getBaseValue() < 20.0) {
            attribute.setBaseValue(20.0);
        }
    }
}