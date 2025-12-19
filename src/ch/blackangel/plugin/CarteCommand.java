package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CarteCommand implements CommandExecutor {
    private final Plugin plugin;

    public CarteCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();

        // Vérifier si carte.yml existe
        if (!plugin.getDataFolder().toPath().resolve("carte.yml").toFile().exists()) {
            player.sendMessage(ChatColor.RED + "La configuration 'carte.yml' est introuvable !");
            return true;
        }

        // Ouvrir le GUI
        CarteGUI.openCarteGUI(player, plugin);
        return true;
    }
}
