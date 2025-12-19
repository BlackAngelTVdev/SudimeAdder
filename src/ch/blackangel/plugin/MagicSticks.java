package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class MagicSticks {

    private final JavaPlugin plugin;
    private static final NamespacedKey STICK_GOOD_KEY = new NamespacedKey("ch.blackangel.plugin", "stick_good");
    private static final NamespacedKey STICK_VIEW_KEY = new NamespacedKey("ch.blackangel.plugin", "stick_view");
    private static final NamespacedKey STICK_TNT_KEY = new NamespacedKey("ch.blackangel.plugin", "stick_tnt");
    private static final NamespacedKey USES_KEY = new NamespacedKey("ch.blackangel.plugin", "uses");

    public MagicSticks(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // =============================
    // === CRÉATION DES STICKS ===
    // =============================

    public ItemStack getStickOfGood() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a✦ Stick of Good");
            meta.setLore(Arrays.asList(
                    "§7Soigne le porteur de façon spectaculaire",
                    "§7Durabilité: §e25 utilisations",
                    "§7Cooldown: §c30 secondes",
                    "",
                    "§a» Clic droit pour activer"
            ));
            meta.getPersistentDataContainer().set(STICK_GOOD_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(USES_KEY, PersistentDataType.INTEGER, 25);
            meta.addEnchant(Enchantment.DURABILITY, 3, true);
            stick.setItemMeta(meta);
        }
        return stick;
    }

    public ItemStack getStickOfView() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b✦ Stick of View");
            meta.setLore(Arrays.asList(
                    "§7Transforme en spectateur pendant 10 secondes",
                    "§7Durabilité: §e25 utilisations",
                    "§7Cooldown: §c1 minute",
                    "",
                    "§b» Clic droit pour activer"
            ));
            meta.getPersistentDataContainer().set(STICK_VIEW_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(USES_KEY, PersistentDataType.INTEGER, 25);
            meta.addEnchant(Enchantment.DURABILITY, 3, true);
            stick.setItemMeta(meta);
        }
        return stick;
    }

    public ItemStack getStickOfTNT() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c✦ Stick of TNT");
            meta.setLore(Arrays.asList(
                    "§7Lance une TNT allumée vers la cible",
                    "§7Durabilité: §e200 utilisations",
                    "§7Cooldown: §c3 secondes",
                    "",
                    "§c» Clic droit pour activer"
            ));
            meta.getPersistentDataContainer().set(STICK_TNT_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(USES_KEY, PersistentDataType.INTEGER, 200);
            meta.addEnchant(Enchantment.DURABILITY, 3, true);
            stick.setItemMeta(meta);
        }
        return stick;
    }

    // =============================
    // === RECETTES AVEC ITEMS EXACTS ===
    // =============================

    public void registerRecipes() {
        registerStickOfGoodRecipe();
        registerStickOfViewRecipe();
        registerStickOfTNTRecipe();
    }

    private void registerStickOfGoodRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "stick_of_good"),
                getStickOfGood()
        );
        recipe.shape("OLO", "OLO", " S ");

        // Vérification exacte du Laxarium
        recipe.setIngredient('L', new RecipeChoice.ExactChoice(CustomItemCreator.getLaxariumItem()));
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('O', Material.GOLD_INGOT);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerStickOfViewRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "stick_of_view"),
                getStickOfView()
        );
        recipe.shape("OLO", "OLO", " S ");

        // Vérification exacte du Laxarium
        recipe.setIngredient('L', new RecipeChoice.ExactChoice(CustomItemCreator.getLaxariumItem()));
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('O', Material.ENDER_EYE);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerStickOfTNTRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(plugin, "stick_of_tnt"),
                getStickOfTNT()
        );
        recipe.shape("TLT", "TLT", " S ");

        // Vérification exacte du Laxarium
        recipe.setIngredient('L', new RecipeChoice.ExactChoice(CustomItemCreator.getLaxariumItem()));
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('T', Material.TNT);

        plugin.getServer().addRecipe(recipe);
    }

    // =============================
    // === VÉRIFICATIONS ===
    // =============================

    public boolean isStickOfGood(ItemStack item) {
        return hasKey(item, STICK_GOOD_KEY);
    }

    public boolean isStickOfView(ItemStack item) {
        return hasKey(item, STICK_VIEW_KEY);
    }

    public boolean isStickOfTNT(ItemStack item) {
        return hasKey(item, STICK_TNT_KEY);
    }

    public int getUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);
    }

    // === SUPPRIMEZ CETTE MÉTHODE EN DOUBLE ===
    /*
    public void setUses(ItemStack item, int uses) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(USES_KEY, PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);

        // Mettre à jour le lore si besoin
        updateUsesLore(item, uses);
    }
    */

    // GARDEZ SEULEMENT CETTE VERSION DE setUses :
    public void setUses(ItemStack item, int uses) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(USES_KEY, PersistentDataType.INTEGER, uses);

        // Mettre à jour le lore
        updateUsesLore(item, meta, uses);

        item.setItemMeta(meta);
    }

    private void updateUsesLore(ItemStack item, ItemMeta meta, int uses) {
        if (meta == null) return;

        java.util.List<String> lore = meta.getLore();
        if (lore == null) return;

        // Trouver et mettre à jour la ligne de durabilité
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("Durabilité:") || line.contains("utilisations")) {
                // Déterminer le type de stick pour le format correct
                String color = "§e"; // couleur par défaut
                if (isStickOfGood(item)) {
                    lore.set(i, "§7Durabilité: " + color + uses + " utilisations");
                } else if (isStickOfView(item)) {
                    lore.set(i, "§7Durabilité: " + color + uses + " utilisations");
                } else if (isStickOfTNT(item)) {
                    lore.set(i, "§7Durabilité: " + color + uses + " utilisations");
                }
                break;
            }
        }

        meta.setLore(lore);
    }

    private boolean hasKey(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}