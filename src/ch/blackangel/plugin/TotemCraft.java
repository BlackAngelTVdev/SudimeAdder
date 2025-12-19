package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.RecipeChoice;

import java.util.Arrays;

public class TotemCraft {

    public static void registerRecipe(JavaPlugin plugin) {
        ItemStack totemStand = getTotemArmorStand();

        NamespacedKey key = new NamespacedKey(plugin, "totem_armor_stand");
        ShapedRecipe recipe = new ShapedRecipe(key, totemStand);

        recipe.shape("OOO", "OSO", "OOO");
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem()));

        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getTotemArmorStand() {
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b§lTotem de Fertilité");
        meta.setLore(Arrays.asList(
                "§7Pose-le sur un bloc de diamant",
                "§7entouré d'obsidienne pour créer",
                "§7un §bTotem de Fertilité§7.",
                "",
                "§eDonne-lui un bloc de diamant",
                "§epour activer le boost de croissance !"
        ));

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTotemArmorStand(ItemStack item) {
        if (item == null || item.getType() != Material.ARMOR_STAND) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals("§b§lTotem de Fertilité");
    }
}