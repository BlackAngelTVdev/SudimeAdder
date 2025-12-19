package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomItemCreator {

    private static JavaPlugin plugin;

    private static final NamespacedKey SUDIME_KEY = new NamespacedKey("ch.blackangel.plugin", "sudime");
    private static final NamespacedKey LAXARIUM_KEY = new NamespacedKey("ch.blackangel.plugin", "laxarium");

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    // =============================
    // === Création des items ===
    // =============================

    public static ItemStack getSudimeItem() {
        ItemStack sudime = new ItemStack(Material.REDSTONE);
        ItemMeta meta = sudime.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Sudime");
            meta.getPersistentDataContainer().set(SUDIME_KEY, PersistentDataType.BYTE, (byte) 1);
            sudime.setItemMeta(meta);
        }
        return sudime;
    }

    public static ItemStack getLaxariumItem() {
        ItemStack laxarium = new ItemStack(Material.LAPIS_LAZULI);
        ItemMeta meta = laxarium.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§9Laxarium");
            meta.getPersistentDataContainer().set(LAXARIUM_KEY, PersistentDataType.BYTE, (byte) 1);
            laxarium.setItemMeta(meta);
        }
        return laxarium;
    }

    // =============================
    // === Vérification des items ===
    // =============================

    public static boolean isSudime(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        return data.has(SUDIME_KEY, PersistentDataType.BYTE);
    }

    public static boolean isLaxarium(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        return data.has(LAXARIUM_KEY, PersistentDataType.BYTE);
    }
}
