package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AntiCraft implements Listener {

    private final Plugin plugin;
    private final Set<Material> blockedCrafts = new HashSet<>();
    private File anticraftFile;
    private FileConfiguration anticraftConfig;

    public AntiCraft(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadAntiCraftConfig();
    }

    private void loadAntiCraftConfig() {
        // Création du dossier du plugin si inexistant
        File pluginFolder = plugin.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        // Création du fichier anticraft.yml s'il n'existe pas
        anticraftFile = new File(pluginFolder, "anticraft.yml");
        if (!anticraftFile.exists()) {
            try {
                anticraftFile.createNewFile();
                anticraftConfig = YamlConfiguration.loadConfiguration(anticraftFile);
                anticraftConfig.set("anticraft", java.util.Arrays.asList("BED"));
                saveAntiCraftConfig();
                plugin.getLogger().info("Fichier anticraft.yml créé avec les valeurs par défaut.");
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer anticraft.yml !");
                e.printStackTrace();
            }
        } else {
            anticraftConfig = YamlConfiguration.loadConfiguration(anticraftFile);
        }

        // Chargement des items interdits
        List<String> blockedItems = anticraftConfig.getStringList("anticraft");
        for (String itemName : blockedItems) {
            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                blockedCrafts.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Matériau inconnu dans anticraft.yml : " + itemName);
            }
        }

        plugin.getLogger().info("Configuration anticraft.yml chargée !");
    }

    private void saveAntiCraftConfig() {
        try {
            anticraftConfig.save(anticraftFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder anticraft.yml !");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result != null && blockedCrafts.contains(result.getType())) {
            event.getInventory().setResult(null);
            event.getView().getPlayer().sendMessage("§cCe craft est interdit !");
        }
    }
}
