package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;

public class CraftingManager implements Listener {

    private final JavaPlugin plugin;
    private final MagicSticks magicSticks;

    // Constructeur pour passer le plugin
    public CraftingManager(JavaPlugin plugin, MagicSticks magicSticks) {
        this.plugin = plugin;
        this.magicSticks = magicSticks;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();

        // Vérifier si le craft utilise des sticks magiques
        if (containsMagicSticks(inv)) {
            event.getInventory().setResult(null); // Annuler le craft
            return;
        }

        // Vérifier si le craft utilise du Laxarium dans des crafts non-autorisés
        if (containsLaxariumInForbiddenCraft(inv)) {
            event.getInventory().setResult(null); // Annuler le craft
            return;
        }

        // Vérifier si le résultat est un bloc ou une torche de redstone
        if (result != null && (result.getType() == Material.REDSTONE_BLOCK || result.getType() == Material.REDSTONE_TORCH)) {
            // Vérifier les ingrédients
            for (ItemStack item : inv.getMatrix()) {
                if (item != null && item.getType() == Material.REDSTONE) {
                    ItemMeta meta = item.getItemMeta();
                    // Vérifier si la redstone n'est PAS un item Sudime
                    if (meta == null || !meta.getDisplayName().equals("§cSudime")) {
                        // Annuler le craft
                        inv.setResult(null);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Vérifie si l'inventaire de craft contient des sticks magiques
     */
    private boolean containsMagicSticks(CraftingInventory inv) {
        for (ItemStack item : inv.getMatrix()) {
            if (item != null && isMagicStick(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si un item est un stick magique
     */
    private boolean isMagicStick(ItemStack item) {
        return magicSticks.isStickOfGood(item) ||
                magicSticks.isStickOfView(item) ||
                magicSticks.isStickOfTNT(item);
    }

    /**
     * Vérifie si le Laxarium est utilisé dans des crafts non-autorisés
     */
    private boolean containsLaxariumInForbiddenCraft(CraftingInventory inv) {
        ItemStack result = inv.getResult();

        // Si pas de résultat, on ne fait rien
        if (result == null) return false;

        // Vérifier si le craft contient du Laxarium
        boolean hasLaxarium = false;
        for (ItemStack item : inv.getMatrix()) {
            if (item != null && isLaxarium(item)) {
                hasLaxarium = true;
                break;
            }
        }

        // Si pas de Laxarium, on ne fait rien
        if (!hasLaxarium) return false;

        // Liste des crafts INTERDITS avec Laxarium
        Material[] forbiddenLaxariumCrafts = {
                // Blocs de lapis
                Material.LAPIS_BLOCK,

                // Colorants (dyes)
                Material.BLUE_DYE,
                Material.LIGHT_BLUE_DYE,
                Material.CYAN_DYE,
                Material.LIME_DYE,
                Material.MAGENTA_DYE,
                Material.ORANGE_DYE,
                Material.PINK_DYE,
                Material.PURPLE_DYE,
                Material.RED_DYE,
                Material.YELLOW_DYE,
                Material.GREEN_DYE,
                Material.BROWN_DYE,
                Material.BLACK_DYE,
                Material.WHITE_DYE,
                Material.LIGHT_GRAY_DYE,
                Material.GRAY_DYE,

                // Autres crafts vanilla qui utilisent du lapis
                Material.BLUE_BED,

                Material.BLUE_CARPET,
                Material.BLUE_CONCRETE,
                Material.BLUE_CONCRETE_POWDER,
                Material.BLUE_GLAZED_TERRACOTTA,
                Material.BLUE_SHULKER_BOX,
                Material.BLUE_STAINED_GLASS,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.BLUE_TERRACOTTA,
                Material.BLUE_WOOL,

                // Autres couleurs similaires
                Material.LIGHT_BLUE_BED,

                Material.LIGHT_BLUE_CARPET,
                Material.LIGHT_BLUE_CONCRETE,
                Material.LIGHT_BLUE_CONCRETE_POWDER,
                Material.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Material.LIGHT_BLUE_SHULKER_BOX,
                Material.LIGHT_BLUE_STAINED_GLASS,
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                Material.LIGHT_BLUE_TERRACOTTA,
                Material.LIGHT_BLUE_WOOL,

                Material.CYAN_BED,

                Material.CYAN_CARPET,
                Material.CYAN_CONCRETE,
                Material.CYAN_CONCRETE_POWDER,
                Material.CYAN_GLAZED_TERRACOTTA,
                Material.CYAN_SHULKER_BOX,
                Material.CYAN_STAINED_GLASS,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.CYAN_TERRACOTTA,
                Material.CYAN_WOOL
        };

        // Vérifier si le résultat est un craft interdit
        for (Material forbiddenMaterial : forbiddenLaxariumCrafts) {
            if (result.getType() == forbiddenMaterial) {
                return true; // Craft interdit
            }
        }

        return false;
    }
    private boolean isLaxarium(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null &&
                meta.hasDisplayName() &&
                meta.getDisplayName().equals("§9Laxarium");
    }

    public static void createCustomRecipes(JavaPlugin plugin) {
        // Créer la pioche ultime
        addCustomTool(plugin, "Pioche en Sudime", Material.NETHERITE_PICKAXE, "op_pickaxe",
                new Enchantment[]{Enchantment.DIG_SPEED, Enchantment.DURABILITY, Enchantment.LOOT_BONUS_BLOCKS},
                new int[]{10, 5, 3}, // Moins cheaté
                "SSS", " X ", " X ");

        // Créer la hache ultime
        addCustomTool(plugin, "Hache en Sudime", Material.NETHERITE_AXE, "op_axe",
                new Enchantment[]{Enchantment.DIG_SPEED, Enchantment.DURABILITY, Enchantment.DAMAGE_ALL},
                new int[]{10, 5, 4},
                "SS ", "SX ", " X ");

        // Créer la pelle ultime
        addCustomTool(plugin, "Pelle en Sudime", Material.NETHERITE_SHOVEL, "op_shovel",
                new Enchantment[]{Enchantment.DIG_SPEED, Enchantment.DURABILITY},
                new int[]{7, 5},
                " S ", " X ", " X ");

        // Créer l'épée ultime (enchantements différents)
        addCustomTool(plugin, "Epee en Sudime", Material.NETHERITE_SWORD, "op_sword",
                new Enchantment[]{Enchantment.DAMAGE_ALL, Enchantment.LOOT_BONUS_MOBS, Enchantment.DURABILITY},
                new int[]{7, 5, 5},
                " S ", " S ", " X ");

        // Créer l'armure ultime (mêmes enchantements pour chaque pièce)
        addCustomArmor(plugin, "Casque en Sudime", Material.NETHERITE_HELMET, "op_helmet",
                "SSS", "S S");
        addCustomArmor(plugin, "Plastron en Sudime", Material.NETHERITE_CHESTPLATE, "op_chestplate",
                "S S", "SSS", "SSS");
        addCustomArmor(plugin, "Jambieres en Sudime", Material.NETHERITE_LEGGINGS, "op_leggings",
                "SSS", "S S", "S S");
        addCustomArmor(plugin, "Bottes en Sudime", Material.NETHERITE_BOOTS, "op_boots",
                "S S", "S S");
    }

    private static void addCustomTool(JavaPlugin plugin, String displayName, Material material, String keyName, Enchantment[] enchants, int[] levels, String... shape) {
        ItemStack tool = new ItemStack(material);
        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c" + displayName);

            // Appliquer les enchantements
            for (int i = 0; i < enchants.length; i++) {
                meta.addEnchant(enchants[i], levels[i], true);
            }

            // Ajouter une étiquette NBT pour empêcher l'amélioration en netherite
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "no_smithing"), PersistentDataType.BYTE, (byte) 1);

            tool.setItemMeta(meta);
        }

        // Ajouter la recette
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        ShapedRecipe recipe = new ShapedRecipe(key, tool);
        recipe.shape(shape);
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem())); // Sudime
        recipe.setIngredient('X', Material.STICK);

        plugin.getServer().addRecipe(recipe);
    }

    private static void addCustomArmor(JavaPlugin plugin, String displayName, Material material, String keyName, String... shape) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b" + displayName);
            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 6, true); // Protection 7
            meta.addEnchant(Enchantment.DURABILITY, 5, true); // Solidité 5


            // Empêcher l'amélioration en netherite
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "no_smithing"), PersistentDataType.BYTE, (byte) 1);

            armor.setItemMeta(meta);
        }

        // Ajouter la recette
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        ShapedRecipe recipe = new ShapedRecipe(key, armor);
        recipe.shape(shape);
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem())); // Sudime

        plugin.getServer().addRecipe(recipe);
    }

    // Écouter l'événement PrepareSmithingEvent pour interdire la transformation en netherite
    @EventHandler
    public void onItemSmith(PrepareSmithingEvent event) {
        // Vérifie les items dans la table de forge
        ItemStack inputItem = event.getInventory().getItem(0); // Premier slot (l'item à améliorer)
        ItemStack upgradeItem = event.getInventory().getItem(1); // Second slot (l'item de l'amélioration, comme la barre de Netherite)

        // Si l'inputItem contient le tag "no_smithing" ou si le second item est une barre de Netherite
        if (inputItem != null && inputItem.hasItemMeta() &&
                inputItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "no_smithing"), PersistentDataType.BYTE)) {
            // Comportement similaire à un objet en fer (impossible de transformer en Netherite)
            if (upgradeItem != null && upgradeItem.getType() == Material.NETHERITE_INGOT) {
                event.setResult(new ItemStack(Material.AIR)); // Empêche l'amélioration
            }
        }
    }
}
