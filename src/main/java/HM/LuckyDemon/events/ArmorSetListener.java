package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.utils.MessageUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Listener para manejar el bonus del set completo de armadura infernal
 */
public class ArmorSetListener implements Listener {

    // UUID FIJO para el modificador (nunca cambiar este UUID)
    private static final UUID MODIFIER_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private static final String MODIFIER_NAME = "infernal_armor_bonus";
    private static final NamespacedKey ARMOR_KEY = new NamespacedKey(HMPlugin.getInstance(), "infernal_armor");

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Verificar set completo al unirse
        new BukkitRunnable() {
            @Override
            public void run() {
                checkArmorSet(e.getPlayer());
            }
        }.runTaskLater(HMPlugin.getInstance(), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Limpiar bonus al salir
        removeHealthBonus(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) e.getWhoClicked();

        // Verificar después de que se complete el click
        new BukkitRunnable() {
            @Override
            public void run() {
                checkArmorSet(player);
            }
        }.runTaskLater(HMPlugin.getInstance(), 2L); // 2 ticks de delay
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        // Detectar cuando se equipa armadura con click derecho
        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        if (item != null && isArmorPiece(item)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkArmorSet(player);
                }
            }.runTaskLater(HMPlugin.getInstance(), 2L);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        // Detectar cuando se tira armadura
        ItemStack item = e.getItemDrop().getItemStack();

        if (isArmorPiece(item)) {
            Player player = e.getPlayer();
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkArmorSet(player);
                }
            }.runTaskLater(HMPlugin.getInstance(), 2L);
        }
    }

    /**
     * Verificar si un item es una pieza de armadura
     */
    private boolean isArmorPiece(ItemStack item) {
        if (item == null)
            return false;
        String type = item.getType().name();
        return type.contains("HELMET") || type.contains("CHESTPLATE") ||
                type.contains("LEGGINGS") || type.contains("BOOTS");
    }

    /**
     * Verificar si el jugador tiene el set completo y aplicar/remover bonus
     */
    private void checkArmorSet(Player player) {
        if (!player.isOnline())
            return;

        boolean hasFullSet = hasFullInfernalSet(player);
        boolean hasBonus = hasHealthBonus(player);

        if (hasFullSet && !hasBonus) {
            // Aplicar bonus
            applyHealthBonus(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        } else if (!hasFullSet && hasBonus) {
            // Remover bonus
            removeHealthBonus(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);
        }
    }

    /**
     * Verificar si el jugador tiene el set completo
     */
    private boolean hasFullInfernalSet(Player player) {
        EntityEquipment equipment = player.getEquipment();
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
        return meta.getPersistentDataContainer().has(ARMOR_KEY, PersistentDataType.BYTE);
    }

    /**
     * Verificar si el jugador tiene el bonus de vida
     */
    private boolean hasHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return false;

        // Buscar por UUID fijo
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            if (modifier.getUniqueId().equals(MODIFIER_UUID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aplicar bonus de +4 corazones (8 puntos de vida)
     */
    private void applyHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return;

        // Remover bonus anterior si existe
        removeHealthBonus(player);

        // Crear y aplicar nuevo modificador con UUID FIJO
        AttributeModifier modifier = new AttributeModifier(
                MODIFIER_UUID, // UUID FIJO
                MODIFIER_NAME,
                8.0, // +4 corazones = +8 puntos de vida
                AttributeModifier.Operation.ADD_NUMBER);

        maxHealth.addModifier(modifier);

        // Curar al jugador para que vea el efecto inmediatamente
        double newHealth = Math.min(player.getHealth() + 8.0, maxHealth.getValue());
        player.setHealth(newHealth);
    }

    /**
     * Remover bonus de vida
     */
    private void removeHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return;

        // Buscar y remover el modificador por UUID
        AttributeModifier toRemove = null;
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            if (modifier.getUniqueId().equals(MODIFIER_UUID)) {
                toRemove = modifier;
                break;
            }
        }

        if (toRemove != null) {
            maxHealth.removeModifier(toRemove);
        }

        // Ajustar la vida actual si excede el nuevo máximo
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }
}