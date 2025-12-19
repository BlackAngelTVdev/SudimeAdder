package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;

public class CarteGUI implements Listener {
    private static Plugin plugin;
    private static YamlConfiguration carteConfig;

    public CarteGUI(Plugin plugin) {
        CarteGUI.plugin = plugin;
        loadConfig();
    }

    public static void openCarteGUI(Player player, Plugin plugin) {
        loadConfig();

        int size = carteConfig.getInt("carte.taille", 27);
        String title = ChatColor.translateAlternateColorCodes('&', carteConfig.getString("carte.titre", "Carte"));

        Inventory gui = Bukkit.createInventory(null, size, title);

        // Chargement des items
        if (carteConfig.contains("carte.items")) {
            for (String slotStr : carteConfig.getConfigurationSection("carte.items").getKeys(false)) {
                int slot = Integer.parseInt(slotStr);
                String type = carteConfig.getString("carte.items." + slot + ".type", "BARRIER");
                String name = carteConfig.getString("carte.items." + slot + ".nom", "&cItem inconnu");
                List<String> lore = carteConfig.getStringList("carte.items." + slot + ".lore");

                ItemStack item = new ItemStack(Material.valueOf(type.toUpperCase()));
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                for (int i = 0; i < lore.size(); i++) {
                    lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);

                gui.setItem(slot, item);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.translateAlternateColorCodes('&', carteConfig.getString("carte.titre", "Carte"));

        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot >= event.getInventory().getSize()) return; // Ignorer clic hors GUI

        if (carteConfig.contains("carte.items." + slot + ".commande")) {
            String command = carteConfig.getString("carte.items." + slot + ".commande");
            player.closeInventory();
            Bukkit.dispatchCommand(player, command); // sans slash au début
        }
    }


    private static void loadConfig() {
        File file = new File(plugin.getDataFolder(), "carte.yml");
        if (!file.exists()) {
            plugin.saveResource("carte.yml", false);
        }
        carteConfig = YamlConfiguration.loadConfiguration(file);
    }
}
