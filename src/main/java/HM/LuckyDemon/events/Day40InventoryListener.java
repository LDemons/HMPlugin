package HM.LuckyDemon.events;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.managers.GameManager;
import HM.LuckyDemon.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DÍA 40+: Bloquea 5 slots del inventario del jugador
 * Los slots bloqueados muestran un icono de barrier block
 * Si el jugador tiene la "Reliquia del Fin", los slots NO se bloquean
 */
public class Day40InventoryListener implements Listener {

    // Slots bloqueados (columna vertical como se ve en la imagen):
    // - Slot 40 = Offhand/Escudo
    // - Slot 13 = Quinta columna, primera fila del inventario
    // - Slot 22 = Quinta columna, segunda fila del inventario
    // - Slot 31 = Quinta columna, tercera fila del inventario
    // - Slot 4 = Quinta columna de la hotbar
    private static final Set<Integer> LOCKED_SLOTS = new HashSet<>(Arrays.asList(4, 13, 22, 31));
    private static final int OFFHAND_SLOT = 40;

    private static NamespacedKey LOCKED_SLOT_KEY;
    private static NamespacedKey END_RELIC_KEY;

    private static NamespacedKey getLockedSlotKey() {
        if (LOCKED_SLOT_KEY == null) {
            LOCKED_SLOT_KEY = new NamespacedKey(HMPlugin.getInstance(), "locked_slot");
        }
        return LOCKED_SLOT_KEY;
    }

    private static NamespacedKey getEndRelicKey() {
        if (END_RELIC_KEY == null) {
            END_RELIC_KEY = new NamespacedKey(HMPlugin.getInstance(), "permadeath_item");
        }
        return END_RELIC_KEY;
    }

    /**
     * Verificar si el jugador tiene la Reliquia del Fin en su inventario
     */
    public static boolean hasEndRelic(Player player) {
        PlayerInventory inv = player.getInventory();

        // Revisar todo el inventario incluyendo armadura y offhand
        for (ItemStack item : inv.getContents()) {
            if (isEndRelic(item)) {
                return true;
            }
        }

        // Revisar offhand específicamente
        if (isEndRelic(inv.getItemInOffHand())) {
            return true;
        }

        // Revisar armadura
        for (ItemStack item : inv.getArmorContents()) {
            if (isEndRelic(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verificar si un item es la Reliquia del Fin
     */
    private static boolean isEndRelic(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String value = meta.getPersistentDataContainer().get(getEndRelicKey(), PersistentDataType.STRING);
        return "end_relic".equals(value);
    }

    /**
     * Al unirse el jugador, colocar los bloqueadores si es día 40+ y NO tiene
     * reliquia
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        // Verificar si es día 40+
        if (GameManager.getInstance().getDay() >= 40) {
            // Delay para asegurar que el inventario esté cargado
            Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                updateLockedSlots(player);
            }, 20L);
        }
    }

    /**
     * Actualizar los slots bloqueados según si tiene o no la reliquia
     */
    public static void updateLockedSlots(Player player) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        if (hasEndRelic(player)) {
            // Tiene la reliquia - remover bloqueadores si los tiene
            removeLockedSlotsFromPlayer(player);
        } else {
            // No tiene la reliquia - aplicar bloqueadores
            applyLockedSlots(player);
        }
    }

    /**
     * Remover los slots bloqueados de un jugador específico
     */
    private static void removeLockedSlotsFromPlayer(Player player) {
        PlayerInventory inv = player.getInventory();

        // Remover de slots normales
        for (int slot : LOCKED_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (isLockedSlotItem(item)) {
                inv.setItem(slot, null);
            }
        }

        // Remover del offhand
        if (isLockedSlotItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }

        player.updateInventory();
    }

    /**
     * Aplicar los slots bloqueados al inventario del jugador
     */
    public static void applyLockedSlots(Player player) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        // Si tiene la reliquia, no bloquear
        if (hasEndRelic(player)) {
            return;
        }

        PlayerInventory inv = player.getInventory();

        // Bloquear slots normales del inventario
        for (int slot : LOCKED_SLOTS) {
            applyLockToSlot(player, inv, slot);
        }

        // Bloquear el offhand
        applyLockToOffhand(player, inv);

        player.updateInventory();
    }

    private static void applyLockToSlot(Player player, PlayerInventory inv, int slot) {
        ItemStack current = inv.getItem(slot);

        // Si ya tiene un bloqueador, skip
        if (current != null && isLockedSlotItem(current)) {
            return;
        }

        // Si hay un item en el slot, moverlo a otro slot disponible
        if (current != null && current.getType() != Material.AIR) {
            int freeSlot = findFreeSlot(player);
            if (freeSlot != -1) {
                inv.setItem(freeSlot, current);
            } else {
                // No hay espacio, dropear el item
                player.getWorld().dropItemNaturally(player.getLocation(), current);
                MessageUtils.send(player, "<red>⚠ No había espacio para un item, se ha dropeado al suelo.");
            }
        }

        // Colocar el bloqueador
        inv.setItem(slot, createLockedSlotItem());
    }

    private static void applyLockToOffhand(Player player, PlayerInventory inv) {
        ItemStack current = inv.getItemInOffHand();

        // Si ya tiene un bloqueador, skip
        if (current != null && isLockedSlotItem(current)) {
            return;
        }

        // Si hay un item en el offhand, moverlo a otro slot disponible
        if (current != null && current.getType() != Material.AIR) {
            int freeSlot = findFreeSlot(player);
            if (freeSlot != -1) {
                inv.setItem(freeSlot, current);
            } else {
                // No hay espacio, dropear el item
                player.getWorld().dropItemNaturally(player.getLocation(), current);
                MessageUtils.send(player, "<red>⚠ No había espacio para un item, se ha dropeado al suelo.");
            }
        }

        // Colocar el bloqueador en offhand
        inv.setItemInOffHand(createLockedSlotItem());
    }

    /**
     * Crear el item bloqueador (structure void con nombre especial)
     */
    private static ItemStack createLockedSlotItem() {
        ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Nombre rojo con formato
            meta.displayName(Component.text("✖ SLOT BLOQUEADO")
                    .color(NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            // Lore explicativo
            meta.lore(List.of(
                    Component.text("").decoration(TextDecoration.ITALIC, false),
                    Component.text("Este slot está bloqueado").color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("a partir del Día 40.").color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("").decoration(TextDecoration.ITALIC, false),
                    Component.text("-5 Slots de inventario").color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("").decoration(TextDecoration.ITALIC, false),
                    Component.text("Obtén la Reliquia del Fin").color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("para desbloquear estos slots.").color(NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)));

            // Marcar con PDC para identificarlo
            meta.getPersistentDataContainer().set(getLockedSlotKey(), PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Verificar si un item es un bloqueador de slot
     */
    private static boolean isLockedSlotItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(getLockedSlotKey(), PersistentDataType.BYTE);
    }

    /**
     * Verificar si el jugador tiene algún item bloqueador en su inventario
     */
    private static boolean hasAnyLockedSlotItem(Player player) {
        PlayerInventory inv = player.getInventory();

        // Revisar slots bloqueados
        for (int slot : LOCKED_SLOTS) {
            if (isLockedSlotItem(inv.getItem(slot))) {
                return true;
            }
        }

        // Revisar offhand
        if (isLockedSlotItem(inv.getItemInOffHand())) {
            return true;
        }

        return false;
    }

    /**
     * Encontrar un slot libre que no esté en la lista de bloqueados
     */
    private static int findFreeSlot(Player player) {
        PlayerInventory inv = player.getInventory();

        // Buscar en el inventario principal (excluyendo slots bloqueados)
        for (int i = 9; i < 36; i++) {
            if (LOCKED_SLOTS.contains(i))
                continue;
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return i;
            }
        }

        // Buscar en la hotbar (excluyendo slots bloqueados)
        for (int i = 0; i < 9; i++) {
            if (LOCKED_SLOTS.contains(i))
                continue;
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return i;
            }
        }

        return -1; // No hay espacio
    }

    /**
     * Prevenir clicks en slots bloqueados (solo si no tiene reliquia)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        // Si tiene la reliquia, permitir todo (solo remover bloqueadores si existen)
        if (hasEndRelic(player)) {
            // Solo remover si hay bloqueadores pendientes
            if (hasAnyLockedSlotItem(player)) {
                removeLockedSlotsFromPlayer(player);
            }
            return;
        }

        int slot = e.getSlot();

        // Verificar si el click es en el inventario del jugador
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(player.getInventory())) {

            // Si es un slot bloqueado o el offhand
            if (LOCKED_SLOTS.contains(slot) || slot == OFFHAND_SLOT) {
                // Cancelar CUALQUIER interacción con slot bloqueado
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
                return;
            }

            // Si el item que está clickeando es un bloqueador (por si está en otro slot)
            if (isLockedSlotItem(e.getCurrentItem())) {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
                return;
            }

            // Si está intentando shift-click un bloqueador
            if (e.isShiftClick() && isLockedSlotItem(e.getCurrentItem())) {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
                return;
            }
        }

        // Si está intentando poner un bloqueador en el cursor a otro lado
        if (isLockedSlotItem(e.getCursor())) {
            e.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
            return;
        }
    }

    /**
     * Prevenir drag en slots bloqueados
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        // Si tiene la reliquia, permitir todo
        if (hasEndRelic(player)) {
            return;
        }

        // Verificar si alguno de los slots afectados está bloqueado
        for (int slot : e.getInventorySlots()) {
            if (LOCKED_SLOTS.contains(slot) || slot == OFFHAND_SLOT) {
                e.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
                return;
            }
        }
    }

    /**
     * Prevenir swap de manos (F por defecto)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHandSwap(PlayerSwapHandItemsEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        // Si tiene la reliquia, permitir
        if (hasEndRelic(e.getPlayer())) {
            return;
        }

        // Cancelar el swap porque el offhand está bloqueado
        e.setCancelled(true);
        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
    }

    /**
     * Prevenir dropear el item bloqueador
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        if (isLockedSlotItem(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);

            // Re-aplicar los bloqueadores por si acaso
            Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                updateLockedSlots(e.getPlayer());
            }, 1L);
        }

        // Si suelta la reliquia, actualizar los slots
        if (isEndRelic(e.getItemDrop().getItemStack())) {
            Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> {
                updateLockedSlots(e.getPlayer());
            }, 1L);
        }

    }

    /**
     * Prevenir usar/colocar el barrier bloqueador
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }

        ItemStack item = e.getItem();
        if (isLockedSlotItem(item)) {
            e.setCancelled(true);
        }
    }

    /**
     * Método público para aplicar los slots bloqueados a todos los jugadores
     */
    public static void applyToAllPlayers() {
        if (GameManager.getInstance().getDay() >= 40) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateLockedSlots(player);
            }
        }
    }

    /**
     * Método público para remover los slots bloqueados de todos los jugadores
     */
    public static void removeFromAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeLockedSlotsFromPlayer(player);
        }
    }
}