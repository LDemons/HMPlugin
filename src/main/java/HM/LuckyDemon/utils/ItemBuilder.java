package HM.LuckyDemon.utils;

import HM.LuckyDemon.HMPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.displayName(MessageUtils.format(name));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(MessageUtils.format(line));
            }
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchant, int level) {
        if (meta != null) {
            meta.addEnchant(enchant, level, true);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder unbreakable(boolean value) {
        if (meta != null) {
            meta.setUnbreakable(value);
        }
        return this;
    }

    public ItemBuilder modelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder color(Color color) {
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        }
        return this;
    }

    /**
     * Añade atributos como Daño, Vida o Armadura.
     * Reemplaza el código NMS antiguo.
     */
    public ItemBuilder attribute(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        if (meta != null) {
            // En 1.21 se usa NamespacedKey y EquipmentSlotGroup
            NamespacedKey key = new NamespacedKey(HMPlugin.getInstance(), UUID.randomUUID().toString());
            AttributeModifier modifier = new AttributeModifier(key, amount, operation, slot);
            meta.addAttributeModifier(attribute, modifier);
        }
        return this;
    }

    /**
     * Guarda datos invisibles en el ítem (PDC).
     * Reemplaza los NBT tags manuales y HiddenStringUtils.
     */
    public ItemBuilder pdc(String key, String value) {
        if (meta != null) {
            NamespacedKey nsKey = new NamespacedKey(HMPlugin.getInstance(), key);
            meta.getPersistentDataContainer().set(nsKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemStack build() {
        if (item != null && meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}