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

    // DÍA 40: la columna del centro (4, 13, 22, 31) y la mano izquierda (40). Los libera la Reliquia del Fin.
    // DÍA 60 (como el original): la mano izquierda pasa a los de la Reliquia del Comienzo, que bloquea casi todo el
    // inventario (quedan libres 0-3, 5, 6 y 35). La Reliquia del Comienzo libera todos.
    private static final Set<Integer> END_RELIC_SLOTS = Set.of(4, 13, 22, 31);
    private static final Set<Integer> BEGINNING_RELIC_SLOTS = Set.of(40, 34, 33, 32, 30, 29, 28, 27, 26, 25, 24,
            23, 21, 20, 19, 18, 17, 16, 15, 14, 12, 11, 10, 9, 8, 7);
    private static final int OFFHAND_SLOT = 40;

    private static NamespacedKey LOCKED_SLOT_KEY;

    private static NamespacedKey getLockedSlotKey() {
        if (LOCKED_SLOT_KEY == null) {
            LOCKED_SLOT_KEY = new NamespacedKey(HMPlugin.getInstance(), "locked_slot");
        }
        return LOCKED_SLOT_KEY;
    }

    /** Slots que tiene bloqueados ahora el jugador según el día y las reliquias que lleva. */
    public static Set<Integer> lockedSlots(Player player) {
        int day = GameManager.getInstance().getDay();
        Set<Integer> locked = new HashSet<>();
        if (day < 40 || HM.LuckyDemon.items.HMItems.carries(player, "beginning_relic")) {
            return locked;
        }
        if (!hasEndRelic(player)) {
            locked.addAll(END_RELIC_SLOTS);
            locked.add(OFFHAND_SLOT);
        }
        if (day >= 60) {
            locked.addAll(BEGINNING_RELIC_SLOTS);
        }
        return locked;
    }

    public static boolean hasEndRelic(Player player) {
        return HM.LuckyDemon.items.HMItems.carries(player, "end_relic");
    }

    private static boolean isRelic(ItemStack item) {
        return HM.LuckyDemon.items.HMItems.isItem(item, "end_relic")
                || HM.LuckyDemon.items.HMItems.isItem(item, "beginning_relic");
    }

    /**
     * Al unirse el jugador, colocar los bloqueadores si es día 40+
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (GameManager.getInstance().getDay() >= 40) {
            // Delay para asegurar que el inventario esté cargado
            Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> updateLockedSlots(player), 20L);
        }
        // La vida maxima del dia 40 la calcula PlayerListener.updateHealth al entrar
    }

    /**
     * Si una Reliquia sale del inventario (a un cofre, etc.) vuelven los bloqueos;
     * si entra, se liberan.
     */
    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        // Al tick siguiente: el juego devuelve al inventario lo del cursor y la cuadrícula de crafteo DESPUÉS de
        // este evento. Antes se bloqueaba con la reliquia en el cursor y, sin huecos, el juego la tiraba al suelo
        if (e.getPlayer() instanceof Player player) {
            Bukkit.getScheduler().runTask(HMPlugin.getInstance(), () -> updateLockedSlots(player));
        }
    }

    @EventHandler
    public void onPickupRelic(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player && isRelic(e.getItem().getItemStack())) {
            Bukkit.getScheduler().runTaskLater(HMPlugin.getInstance(), () -> updateLockedSlots(player), 1L);
        }
    }

    /**
     * Pone los bloqueadores en los slots bloqueados y los quita de los que ya no lo están.
     */
    public static void updateLockedSlots(Player player) {
        if (GameManager.getInstance().getDay() < 40) {
            return;
        }
        Set<Integer> locked = lockedSlots(player);
        PlayerInventory inv = player.getInventory();
        for (int slot = 0; slot <= OFFHAND_SLOT; slot++) {
            if (locked.contains(slot)) {
                applyLockToSlot(player, inv, slot, locked);
            } else if (isLockedSlotItem(inv.getItem(slot))) {
                inv.setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    /** Quita todos los bloqueadores. */
    private static void removeLockedSlotsFromPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int slot = 0; slot <= OFFHAND_SLOT; slot++) {
            if (isLockedSlotItem(inv.getItem(slot))) {
                inv.setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    private static void applyLockToSlot(Player player, PlayerInventory inv, int slot, Set<Integer> locked) {
        ItemStack current = inv.getItem(slot);

        // Si ya tiene un bloqueador, skip
        if (current != null && isLockedSlotItem(current)) {
            return;
        }

        // Si hay un item en el slot, moverlo a otro slot disponible
        if (current != null && current.getType() != Material.AIR) {
            int freeSlot = findFreeSlot(player, locked);
            if (freeSlot != -1) {
                inv.setItem(freeSlot, current);
            } else {
                // No hay espacio, dropear el item
                player.getWorld().dropItemNaturally(player.getLocation(), current);
                MessageUtils.send(player, "<red>No había espacio para un item, se ha dropeado al suelo.");
            }
        }

        // Colocar el bloqueador (el 40 es la mano izquierda)
        inv.setItem(slot, createLockedSlotItem());
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
                    Component.text("Este slot está bloqueado.").color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("").decoration(TextDecoration.ITALIC, false),
                    Component.text(GameManager.getInstance().getDay() >= 60 ? "Obtén la Reliquia del Comienzo"
                            : "Obtén la Reliquia del Fin").color(NamedTextColor.GOLD)
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
     * Encontrar un slot libre que no esté bloqueado (primero el inventario, después la hotbar)
     */
    private static int findFreeSlot(Player player, Set<Integer> locked) {
        PlayerInventory inv = player.getInventory();
        for (int n = 0; n < 36; n++) {
            int i = (n + 9) % 36;
            ItemStack item = inv.getItem(i);
            if (!locked.contains(i) && (item == null || item.getType() == Material.AIR)) {
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

        // En creativo el objeto que se arrastra solo existe en el cliente: cancelar el clic lo borraba. No se frena;
        // al cerrar, updateLockedSlots mueve lo que quede en un slot bloqueado a otro libre o al suelo
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        Set<Integer> locked = lockedSlots(player);
        if (locked.isEmpty()) {
            return;
        }
        int slot = e.getSlot();

        // Tecla numérica de un slot bloqueado o F (mano izquierda) en cualquier inventario, también en cofres:
        // intercambiaban el bloqueador con el slot del cofre
        if ((e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                && locked.contains(e.getHotbarButton()))
                || (e.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND && locked.contains(OFFHAND_SLOT))) {
            e.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.2f);
            return;
        }

        // Verificar si el click es en el inventario del jugador
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(player.getInventory())) {

            // Si es un slot bloqueado o el offhand
            if (locked.contains(slot)) {
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
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return; // ver onInventoryClick
        }

        Set<Integer> locked = lockedSlots(player);

        // Verificar si alguno de los slots afectados del jugador está bloqueado (los del cofre no cuentan)
        for (int raw : e.getRawSlots()) {
            int slot = e.getView().convertSlot(raw);
            if (e.getView().getInventory(raw) == player.getInventory()
                    && locked.contains(slot)) {
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

        // Mano izquierda o slot de la mano bloqueados: el bloqueador cambiaría de sitio
        Set<Integer> locked = lockedSlots(e.getPlayer());
        if (!locked.contains(OFFHAND_SLOT) && !locked.contains(e.getPlayer().getInventory().getHeldItemSlot())) {
            return;
        }
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

        // Si suelta una reliquia, actualizar los slots
        if (isRelic(e.getItemDrop().getItemStack())) {
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

    /**
     * Restringir el crafteo de la Reliquia del Fin solo a día 40+
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftEndRelic(org.bukkit.event.inventory.PrepareItemCraftEvent e) {
        if (e.getRecipe() == null || e.getRecipe().getResult() == null) {
            return;
        }

        // Verificar si el resultado es la Reliquia del Fin
        ItemStack result = e.getRecipe().getResult();
        if (!HM.LuckyDemon.items.HMItems.isItem(result, "end_relic")) {
            return;
        }

        // Si no es día 40+, cancelar el crafteo
        if (GameManager.getInstance().getDay() < 40) {
            e.getInventory().setResult(null);
        }
    }
}