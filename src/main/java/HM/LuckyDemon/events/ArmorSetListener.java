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

    /** Cualquier cambio de armadura (también dispensers, romperse o morir): se mira al tick siguiente. */
    @EventHandler
    public void onArmorChange(com.destroystokyo.paper.event.player.PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();
        HMPlugin.getInstance().getServer().getScheduler().runTask(HMPlugin.getInstance(), () -> checkArmorSet(player));
    }

    /** Al morir se pierden los efectos (la Resistencia del set) y con KeepInventory la armadura no cambia. */
    @EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        HMPlugin.getInstance().getServer().getScheduler().runTask(HMPlugin.getInstance(), () -> checkArmorSet(player));
    }

    /**
     * Bonus del set completo (como el original): Armadura Infernal del día 25 +4 corazones; Netherite Infernal (la
     * roja de cuero) +5 corazones y Resistencia I mientras la lleves puesta.
     */
    private void checkArmorSet(Player player) {
        if (!player.isOnline())
            return;

        boolean infernalNetherite = hasFullSet(player, ArmorSetListener::isInfernalNetherite);
        double bonus = infernalNetherite ? 10.0 : hasFullSet(player, this::isInfernalArmor) ? 8.0 : 0.0;
        double current = currentHealthBonus(player);
        if (bonus != current) {
            removeHealthBonus(player);
            if (bonus > 0) {
                applyHealthBonus(player, bonus);
            }
            player.playSound(player.getLocation(), bonus > current ? org.bukkit.Sound.BLOCK_BEACON_ACTIVATE
                    : org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, bonus > current ? 1.5f : 0.8f);
        }

        org.bukkit.potion.PotionEffect resistance = player.getPotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
        boolean ours = resistance != null && resistance.isInfinite() && resistance.getAmplifier() == 0;
        if (infernalNetherite && resistance == null) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE,
                    org.bukkit.potion.PotionEffect.INFINITE_DURATION, 0, true, false));
        } else if (!infernalNetherite && ours) {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
        }
    }

    private boolean hasFullSet(Player player, java.util.function.Predicate<ItemStack> piece) {
        EntityEquipment equipment = player.getEquipment();
        return piece.test(equipment.getHelmet()) && piece.test(equipment.getChestplate())
                && piece.test(equipment.getLeggings()) && piece.test(equipment.getBoots());
    }

    /** Armadura Infernal del día 25 (netherite real). */
    private boolean isInfernalArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(ARMOR_KEY, PersistentDataType.BYTE);
    }

    /** Netherite Infernal: la de cuero rojo (HMItems.craftInfernal*, marca tier=infernal); las Elytras de Netherite
     * Infernal cuentan como pechera (como el original). */
    private static boolean isInfernalNetherite(ItemStack item) {
        return item != null && item.hasItemMeta()
                && (item.getType().name().startsWith("LEATHER_") || item.getType() == org.bukkit.Material.ELYTRA)
                && "infernal".equals(item.getItemMeta().getPersistentDataContainer()
                        .get(TIER_KEY, PersistentDataType.STRING));
    }

    private static final NamespacedKey TIER_KEY = new NamespacedKey(HMPlugin.getInstance(), "tier");

    /** Vida que da ahora el bonus del set (0 si no lo tiene). */
    private double currentHealthBonus(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return 0;
        for (AttributeModifier modifier : maxHealth.getModifiers()) {
            if (modifier.getUniqueId().equals(MODIFIER_UUID)) {
                return modifier.getAmount();
            }
        }
        return 0;
    }

    private void applyHealthBonus(Player player, double amount) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return;

        maxHealth.addModifier(new AttributeModifier(MODIFIER_UUID, MODIFIER_NAME, amount,
                AttributeModifier.Operation.ADD_NUMBER));

        // Curar al jugador para que vea el efecto inmediatamente
        player.setHealth(Math.min(player.getHealth() + amount, maxHealth.getValue()));
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
