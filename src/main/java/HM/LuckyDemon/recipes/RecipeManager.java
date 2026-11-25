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
                registerChorusToDragonBreath(); // DÍA 40
                registerShulkerBoxToShells(); // DÍA 40 (Day40CraftListener la bloquea antes del 40 o con la caja llena)
        }

        public static final String SHULKER_SHELLS_KEY = "shulker_box_to_shells";

        /**
         * DÍA 40: Shulker Box (cualquier color) → 2 Shulker Shells
         */
        private static void registerShulkerBoxToShells() {
                NamespacedKey key = new NamespacedKey(HMPlugin.getInstance(), SHULKER_SHELLS_KEY);
                org.bukkit.inventory.ShapelessRecipe recipe = new org.bukkit.inventory.ShapelessRecipe(key,
                                new ItemStack(Material.SHULKER_SHELL, 2));
                recipe.addIngredient(new org.bukkit.inventory.RecipeChoice.MaterialChoice(
                                org.bukkit.Tag.SHULKER_BOXES));

                if (HMPlugin.getInstance().getServer().getRecipe(key) == null) {
                        HMPlugin.getInstance().getServer().addRecipe(recipe);
                }
        }

        private static void registerInfernalArmor() {
                // Netherite Infernal: la pieza de netherite (Armadura Infernal del dia 25) en el centro rodeada por
                // arriba, los lados y abajo de Bloques de Netherite Infernal. BeginningManager le pasa los encantamientos
                org.bukkit.inventory.RecipeChoice block = new org.bukkit.inventory.RecipeChoice.ExactChoice(
                                HMItems.createInfernalNetherite());
                registerShaped("infernal_helmet", HMItems.craftInfernalHelmet(),
                                " I ", "IAI", " I ", 'I', block, 'A', Material.NETHERITE_HELMET);
                registerShaped("infernal_chestplate", HMItems.craftInfernalChestplate(),
                                " I ", "IAI", " I ", 'I', block, 'A', Material.NETHERITE_CHESTPLATE);
                registerShaped("infernal_leggings", HMItems.craftInfernalLeggings(),
                                " I ", "IAI", " I ", 'I', block, 'A', Material.NETHERITE_LEGGINGS);
                registerShaped("infernal_boots", HMItems.craftInfernalBoots(),
                                " I ", "IAI", " I ", 'I', block, 'A', Material.NETHERITE_BOOTS);

                // Elytras de Netherite Infernal (día 60, Day40CraftListener): elytra rodeada de 8 bloques
                registerShaped("infernal_elytra", HMItems.craftInfernalElytra(),
                                "III",
                                "IEI",
                                "III",
                                'I', block,
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
                registerShaped("super_gap", HMItems.createSuperGoldenApple(),
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
                        if (ingredients[i + 1] instanceof org.bukkit.inventory.RecipeChoice choice) {
                                recipe.setIngredient(character, choice);
                        } else {
                                recipe.setIngredient(character, (Material) ingredients[i + 1]);
                        }
                }

                // Registrar solo si no existe (para evitar errores al recargar)
                if (HMPlugin.getInstance().getServer().getRecipe(key) == null) {
                        HMPlugin.getInstance().getServer().addRecipe(recipe);
                }
        }

        /**
         * DÍA 40: Chorus Flower → Dragon's Breath
         */
        private static void registerChorusToDragonBreath() {
                NamespacedKey key = new NamespacedKey(HMPlugin.getInstance(), "chorus_to_dragon_breath");
                ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.DRAGON_BREATH));

                recipe.shape(
                                "CCC",
                                "CCC",
                                "CCC");

                recipe.setIngredient('C', Material.CHORUS_FLOWER);

                if (HMPlugin.getInstance().getServer().getRecipe(key) == null) {
                        HMPlugin.getInstance().getServer().addRecipe(recipe);
                }
        }
}