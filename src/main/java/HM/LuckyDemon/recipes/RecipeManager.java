package HM.LuckyDemon.recipes;

import HM.LuckyDemon.HMPlugin;
import HM.LuckyDemon.items.HMItems;
import HM.LuckyDemon.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {

        public static void registerRecipes() {
                registerInfernalArmor();
                registerRelics();
                registerLifeOrb();
                registerGaps();
                registerSkullToNotch(); // DÍA 20
        }

        private static void registerInfernalArmor() {
                // Casco Infernal
                registerShaped("infernal_helmet", HMItems.craftInfernalHelmet(),
                                " D ",
                                "DAD",
                                " D ",
                                'D', Material.DIAMOND,
                                'A', Material.LEATHER_HELMET);

                // Pechera Infernal
                registerShaped("infernal_chestplate", HMItems.craftInfernalChestplate(),
                                " D ",
                                "DAD",
                                " D ",
                                'D', Material.DIAMOND,
                                'A', Material.LEATHER_CHESTPLATE);

                // Pantalones Infernales
                registerShaped("infernal_leggings", HMItems.craftInfernalLeggings(),
                                " D ",
                                "DAD",
                                " D ",
                                'D', Material.DIAMOND,
                                'A', Material.LEATHER_LEGGINGS);

                // Botas Infernales
                registerShaped("infernal_boots", HMItems.craftInfernalBoots(),
                                " D ",
                                "DAD",
                                " D ",
                                'D', Material.DIAMOND,
                                'A', Material.LEATHER_BOOTS);

                // Elytras Infernales (Receta original: Diamantes rodeando Elytras)
                registerShaped("infernal_elytra", HMItems.craftInfernalElytra(),
                                "DDD",
                                "DED",
                                "DDD",
                                'D', Material.DIAMOND,
                                'E', Material.ELYTRA);
        }

        private static void registerRelics() {
                // Reliquia del Fin (Shulker Shells y Bloque de Diamante)
                // Receta original: S sobre D sobre S
                registerShaped("end_relic", HMItems.createEndRelic(),
                                " S ",
                                " D ",
                                " S ",
                                'S', Material.SHULKER_SHELL,
                                'D', Material.DIAMOND_BLOCK);

                // Reliquia del Comienzo (Original: Bloques Diamante, Tinte Celeste, Shells)
                registerShaped("beginning_relic", HMItems.createBeginningRelic(),
                                "SBS",
                                "BDB",
                                "SBS",
                                'S', Material.SHULKER_SHELL,
                                'B', Material.DIAMOND_BLOCK,
                                'D', Material.LIGHT_BLUE_DYE);
        }

        private static void registerLifeOrb() {
                // Orbe de Vida (Receta compleja original)
                // DGB (Diamond, Gold, Bone Block)
                // RSE (Rod, Sea Heart, End Stone)
                // NOL (Nether Brick, Obsidian, Lapis Block)
                registerShaped("life_orb", HMItems.createLifeOrb(),
                                "DGB",
                                "RSE",
                                "NOL",
                                'D', Material.DIAMOND, 'G', Material.GOLD_INGOT, 'B', Material.BONE_BLOCK,
                                'R', Material.BLAZE_ROD, 'S', Material.HEART_OF_THE_SEA, 'E', Material.END_STONE,
                                'N', Material.NETHER_BRICKS, 'O', Material.OBSIDIAN, 'L', Material.LAPIS_BLOCK);
        }

        private static void registerGaps() {
                // Hyper Golden Apple (Bloques de oro + Manzana dorada)
                ItemStack hyperGap = new ItemBuilder(Material.GOLDEN_APPLE)
                                .name("<gold>Hyper Golden Apple +")
                                .unbreakable(true) // Marca visual
                                .pdc("gap_type", "hyper")
                                .build();

                registerShaped("hyper_gap", hyperGap,
                                "GGG",
                                "GAG",
                                "GGG",
                                'G', Material.GOLD_BLOCK,
                                'A', Material.GOLDEN_APPLE);

                // Super Golden Apple (Lingotes de oro + Manzana dorada)
                ItemStack superGap = new ItemBuilder(Material.GOLDEN_APPLE)
                                .name("<gold>Super Golden Apple +")
                                .pdc("gap_type", "super")
                                .build();

                registerShaped("super_gap", superGap,
                                "GGG",
                                "GAG",
                                "GGG",
                                'G', Material.GOLD_INGOT,
                                'A', Material.GOLDEN_APPLE);
        }

        /**
         * DÍA 20: Skeleton Skull + 8 Gold Blocks → Notch Apple
         */
        private static void registerSkullToNotch() {
                NamespacedKey key = new NamespacedKey(HMPlugin.getInstance(), "skull_to_notch");
                ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));

                recipe.shape(
                                "GGG",
                                "GSG",
                                "GGG");

                recipe.setIngredient('G', Material.GOLD_BLOCK);
                recipe.setIngredient('S', Material.SKELETON_SKULL);

                if (HMPlugin.getInstance().getServer().getRecipe(key) == null) {
                        HMPlugin.getInstance().getServer().addRecipe(recipe);
                }
        }

        // --- Métodos de ayuda para registrar fácil ---

        // Método para recetas con forma (Crafting Table normal)
        private static void registerShaped(String keyName, ItemStack result, String line1, String line2, String line3,
                        Object... ingredients) {
                NamespacedKey key = new NamespacedKey(HMPlugin.getInstance(), keyName);
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape(line1, line2, line3);

                for (int i = 0; i < ingredients.length; i += 2) {
                        Character character = (Character) ingredients[i];
                        Material material = (Material) ingredients[i + 1];
                        recipe.setIngredient(character, material);
                }

                // Registrar solo si no existe (para evitar errores al recargar)
                if (HMPlugin.getInstance().getServer().getRecipe(key) == null) {
                        HMPlugin.getInstance().getServer().addRecipe(recipe);
                }
        }
}