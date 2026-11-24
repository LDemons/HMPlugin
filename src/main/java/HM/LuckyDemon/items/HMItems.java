package HM.LuckyDemon.items;

import HM.LuckyDemon.utils.ItemBuilder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

public class HMItems {

    // --- Reliquias y Orbes ---
    /** Super Golden Apple + (Health Boost I 5 min, ver PlayerListener). */
    public static ItemStack createSuperGoldenApple() {
        return new ItemBuilder(Material.GOLDEN_APPLE)
                .name("<gold>Super Golden Apple +")
                .pdc("gap_type", "super")
                .build();
    }

    public static ItemStack createEndRelic() {
        return new ItemBuilder(Material.LIGHT_BLUE_DYE)
                .name("<gold>Reliquia del Fin")
                .modelData(1)
                .unbreakable(true)
                .model("end_relic")
                .pdc("permadeath_item", "end_relic") // Identificador único
                .build();
    }

    public static ItemStack createBeginningRelic() {
        return new ItemBuilder(Material.CYAN_DYE)
                .name("<gold>Reliquia del Comienzo")
                .modelData(1)
                .unbreakable(true)
                .model("beginning_relic")
                .pdc("permadeath_item", "beginning_relic")
                .build();
    }

    public static ItemStack createLifeOrb() {
        return new ItemBuilder(Material.BROWN_DYE)
                .name("<gold>Orbe de Vida")
                .modelData(1)
                .unbreakable(true)
                .model("life_orb")
                .pdc("permadeath_item", "life_orb")
                .build();
    }

    /**
     * Bloque de Netherite Infernal (lo suelta el bloque de The Beginning al picarlo). Es papel con el modelo del
     * bloque: no se puede poner y BeginningManager no deja usarlo en otras recetas.
     */
    public static ItemStack createInfernalNetherite() {
        return new ItemBuilder(Material.PAPER)
                .name("<dark_red>Bloque de Netherite Infernal")
                .model("infernal_netherite")
                .pdc("permadeath_item", "infernal_netherite")
                .build();
    }

    /**
     * Items creados antes de que el resource pack tuviera sus texturas (no llevan item_model): se les pone al
     * entrar el jugador (inventario y ender chest).
     */
    public static void updateTextures(org.bukkit.inventory.Inventory inv) {
        org.bukkit.NamespacedKey idKey = new org.bukkit.NamespacedKey(HM.LuckyDemon.HMPlugin.getInstance(), "permadeath_item");
        org.bukkit.NamespacedKey tierKey = new org.bukkit.NamespacedKey(HM.LuckyDemon.HMPlugin.getInstance(), "tier");
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !item.hasItemMeta() || item.getItemMeta().hasItemModel()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();
            String id = pdc.get(idKey, org.bukkit.persistence.PersistentDataType.STRING);
            if (id != null) {
                inv.setItem(i, new ItemBuilder(item).model(id).build());
            } else if ("infernal".equals(pdc.get(tierKey, org.bukkit.persistence.PersistentDataType.STRING))
                    && item.getType().name().startsWith("LEATHER_")) {
                inv.setItem(i, new ItemBuilder(item)
                        .model("infernal_netherite_" + item.getType().name().substring(8).toLowerCase())
                        .equipment("infernal_netherite", item.getType().getEquipmentSlot()).build());
            }
        }
    }

    public static boolean isInfernalNetherite(ItemStack item) {
        return item != null && item.hasItemMeta() && "infernal_netherite".equals(item.getItemMeta()
                .getPersistentDataContainer().get(new org.bukkit.NamespacedKey(HM.LuckyDemon.HMPlugin.getInstance(),
                        "permadeath_item"), org.bukkit.persistence.PersistentDataType.STRING));
    }

    // --- Netherite Infernal (Armadura) ---
    // Como en el original: armadura de cuero tenida de rojo. El resource pack le da modelo y textura de netherite
    // (grises, se tinen con el color del cuero).

    public static ItemStack craftInfernalHelmet() {
        return new ItemBuilder(Material.LEATHER_HELMET)
                .name("<dark_purple>Infernal Netherite Helmet")
                .model("infernal_netherite_helmet")
                .equipment("infernal_netherite", EquipmentSlot.HEAD)
                .color(Color.fromRGB(16711680)) // Rojo puro
                .unbreakable(true)
                .attribute(Attribute.ARMOR, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                .attribute(Attribute.ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.HEAD)
                .pdc("tier", "infernal")
                .build();
    }

    public static ItemStack craftInfernalChestplate() {
        return new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .name("<dark_purple>Infernal Netherite Chestplate")
                .model("infernal_netherite_chestplate")
                .equipment("infernal_netherite", EquipmentSlot.CHEST)
                .color(Color.fromRGB(16711680))
                .unbreakable(true)
                .attribute(Attribute.ARMOR, 8, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .attribute(Attribute.ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.CHEST)
                .pdc("tier", "infernal")
                .build();
    }

    public static ItemStack craftInfernalLeggings() {
        return new ItemBuilder(Material.LEATHER_LEGGINGS)
                .name("<dark_purple>Infernal Netherite Leggings")
                .model("infernal_netherite_leggings")
                .equipment("infernal_netherite", EquipmentSlot.LEGS)
                .color(Color.fromRGB(16711680))
                .unbreakable(true)
                .attribute(Attribute.ARMOR, 6, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .attribute(Attribute.ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.LEGS)
                .pdc("tier", "infernal")
                .build();
    }

    public static ItemStack craftInfernalBoots() {
        return new ItemBuilder(Material.LEATHER_BOOTS)
                .name("<dark_purple>Infernal Netherite Boots")
                .model("infernal_netherite_boots")
                .equipment("infernal_netherite", EquipmentSlot.FEET)
                .color(Color.fromRGB(16711680))
                .unbreakable(true)
                .attribute(Attribute.ARMOR, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                .attribute(Attribute.ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.FEET)
                .pdc("tier", "infernal")
                .build();
    }

    // --- Elytras Infernales ---
    public static ItemStack craftInfernalElytra() {
        return new ItemBuilder(Material.ELYTRA)
                .name("<dark_purple>Elytras de Netherite Infernal")
                .modelData(1)
                .unbreakable(true)
                .attribute(Attribute.ARMOR, 8, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .attribute(Attribute.ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.CHEST)
                .pdc("tier", "infernal")
                .build();
    }
}