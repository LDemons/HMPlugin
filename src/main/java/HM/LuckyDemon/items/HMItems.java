package HM.LuckyDemon.items;

import HM.LuckyDemon.utils.ItemBuilder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;

public class HMItems {

    // --- Reliquias y Orbes ---
    public static ItemStack createEndRelic() {
        return new ItemBuilder(Material.LIGHT_BLUE_DYE)
                .name("<gold>Reliquia del Fin")
                .modelData(1)
                .unbreakable(true)
                .pdc("permadeath_item", "end_relic") // Identificador único
                .build();
    }

    public static ItemStack createBeginningRelic() {
        return new ItemBuilder(Material.CYAN_DYE)
                .name("<gold>Reliquia del Comienzo")
                .modelData(1)
                .unbreakable(true)
                .pdc("permadeath_item", "beginning_relic")
                .build();
    }

    public static ItemStack createLifeOrb() {
        return new ItemBuilder(Material.BROWN_DYE)
                .name("<gold>Orbe de Vida")
                .modelData(1)
                .unbreakable(true)
                .pdc("permadeath_item", "life_orb")
                .build();
    }

    // --- Netherite Infernal (Armadura) ---
    // En el código original era armadura de cuero tintada de rojo con atributos bestiales.

    public static ItemStack craftInfernalHelmet() {
        return new ItemBuilder(Material.LEATHER_HELMET)
                .name("<dark_purple>Infernal Netherite Helmet")
                .color(Color.fromRGB(16711680)) // Rojo puro
                .unbreakable(true)
                .attribute(Attribute.GENERIC_ARMOR, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                .attribute(Attribute.GENERIC_ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD)
                .pdc("tier", "infernal")
                .build();
    }

    public static ItemStack craftInfernalChestplate() {
        return new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .name("<dark_purple>Infernal Netherite Chestplate")
                .color(Color.fromRGB(16711680))
                .unbreakable(true)
                .attribute(Attribute.GENERIC_ARMOR, 8, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .attribute(Attribute.GENERIC_ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .pdc("tier", "infernal")
                .build();
    }

    public static ItemStack craftInfernalLeggings() {
        return new ItemBuilder(Material.LEATHER_LEGGINGS)
                .name("<dark_purple>Infernal Netherite Leggings")
                .color(Color.fromRGB(16711680))
                .unbreakable(true)
                .attribute(Attribute.GENERIC_ARMOR, 6, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .attribute(Attribute.GENERIC_ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .pdc("tier", "infernal")
                .build();
    }

    public static ItemStack craftInfernalBoots() {
        return new ItemBuilder(Material.LEATHER_BOOTS)
                .name("<dark_purple>Infernal Netherite Boots")
                .color(Color.fromRGB(16711680))
                .unbreakable(true)
                .attribute(Attribute.GENERIC_ARMOR, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                .attribute(Attribute.GENERIC_ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET)
                .pdc("tier", "infernal")
                .build();
    }

    // --- Elytras Infernales ---
    public static ItemStack craftInfernalElytra() {
        return new ItemBuilder(Material.ELYTRA)
                .name("<dark_purple>Elytras de Netherite Infernal")
                .modelData(1)
                .unbreakable(true)
                .attribute(Attribute.GENERIC_ARMOR, 8, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .attribute(Attribute.GENERIC_ARMOR_TOUGHNESS, 3, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .pdc("tier", "infernal")
                .build();
    }
}