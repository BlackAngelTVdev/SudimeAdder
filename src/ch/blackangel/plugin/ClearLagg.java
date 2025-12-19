package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClearLagg implements CommandExecutor {

    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;
    private Set<Material> whitelist = new HashSet<>();
    private long nextClearTime = 0;
    private BukkitRunnable clearTask;

    public ClearLagg(Plugin plugin) {
        this.plugin = plugin;
        createConfig();
        loadWhitelist();
        startClearTask();
    }

    private void createConfig() {
        File pluginFolder = plugin.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        configFile = new File(pluginFolder, "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();

                // Valeurs par défaut
                config.set("clearlagg.interval", 60);
                config.set("clearlagg.whitelist", Arrays.asList("DIAMOND", "NETHERITE_INGOT", "EMERALD"));

                // Ajout des nouvelles valeurs
                config.set("event.enabled", false);
                config.set("event.respawn-location", "0 100 0");

                saveConfig();
                plugin.getLogger().info("config.yml créé !");
            } catch (IOException e) {
                plugin.getLogger().severe("Erreur lors de la création du config.yml : " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("config.yml chargé !");
        }
    }


    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder config.yml !");
        }
    }

    private void loadWhitelist() {
        List<String> itemList = config.getStringList("clearlagg.whitelist");
        for (String itemName : itemList) {
            try {
                Material material = Material.valueOf(itemName.toUpperCase());
                whitelist.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Matériel inconnu dans la whitelist : " + itemName);
            }
        }
    }

    private void startClearTask() {
        if (clearTask != null) {
            clearTask.cancel();
            plugin.getLogger().info("Tâche de nettoyage précédente annulée.");
        }

        int interval = config.getInt("clearlagg.interval", 60);

        clearTask = new BukkitRunnable() {
            @Override
            public void run() {
                nextClearTime = System.currentTimeMillis() + 30000; // 30 sec avant la suppression
                Bukkit.broadcastMessage("§c[ClearLagg] §eSuppression des items au sol dans §a30 secondes §e!");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        nextClearTime = System.currentTimeMillis() + 5000; // 5 sec avant la suppression
                        Bukkit.broadcastMessage("§c[ClearLagg] §eSuppression des items au sol dans §c5 secondes §e!");

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                int removed = 0;
                                for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
                                    if (entity instanceof Item) {
                                        Item item = (Item) entity;
                                        ItemStack itemStack = item.getItemStack();
                                        if (!whitelist.contains(itemStack.getType())) {
                                            item.remove();
                                            removed++;
                                        }
                                    }
                                }
                                Bukkit.broadcastMessage("§c[ClearLagg] §f" + removed + " items supprimés !");
                                nextClearTime = System.currentTimeMillis() + (interval * 1000L); // Planifie la prochaine suppression
                            }
                        }.runTaskLater(plugin, 100); // Suppression après 5 secondes (100 ticks)
                    }
                }.runTaskLater(plugin, 600); // Annonce 30 secondes avant la suppression
            }
        };

        clearTask.runTaskTimer(plugin, 0, interval * 20L);
        plugin.getLogger().info("Tâche de nettoyage démarrée avec un intervalle de " + interval + " secondes.");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("clearinfo")) {
            long timeLeft = nextClearTime - System.currentTimeMillis();
            if (timeLeft > 0) {
                long minutes = (timeLeft / 60000) % 60;
                long seconds = (timeLeft / 1000) % 60;
                sender.sendMessage("§a[ClearLagg] Prochain nettoyage dans §e" + String.format("%02d:%02d", minutes, seconds));
            } else {
                sender.sendMessage("§c[ClearLagg] Aucune suppression planifiée.");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("clearlagg")) {
            if (!sender.hasPermission("clearlagg.admin")) {
                sender.sendMessage("§cVous n'avez pas la permission d'exécuter cette commande !");
                return true;
            }

            Bukkit.broadcastMessage("§c[ClearLagg] §eNettoyage manuel des items au sol !");
            int removed = 0;
            for (Entity entity : Bukkit.getWorlds().get(0).getEntities()) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    ItemStack itemStack = item.getItemStack();
                    if (!whitelist.contains(itemStack.getType())) {
                        item.remove();
                        removed++;
                    }
                }
            }
            Bukkit.broadcastMessage("§c[ClearLagg] §f" + removed + " items supprimés !");

            return true;
        }
        return false;
    }


}

