package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class wiki implements CommandExecutor {

    private final Plugin plugin;

    public wiki(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();
        String wikiLink = config.getString("wiki_link", "https://laxacube.softr.app/"); // Lien par défaut

        // DEBUG : Vérifie si le lien est bien récupéré
        System.out.println("[DEBUG] Lien récupéré du config.yml: " + wikiLink);

        // Construction du message JSON cliquable
        String tellrawCommand = "[\"\","
                + "{\"text\":\"----------------------\",\"bold\":true,\"color\":\"#8600FF\"},"
                + "{\"text\":\"\\n\",\"bold\":true},"
                + "{\"text\":\"               Wiki\",\"bold\":true,\"color\":\"aqua\"},"
                + "{\"text\":\"\\n\",\"bold\":true},"
                + "{\"text\":\"          ▶\",\"bold\":true,\"color\":\"red\"},"
                + "{\"text\":\" Clic ici\",\"bold\":true,\"color\":\"blue\","
                + " \"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + wikiLink + "\"}},"
                + "{\"text\":\" ◀\",\"bold\":true,\"color\":\"red\"},"
                + "{\"text\":\"\\n\",\"bold\":true},"
                + "{\"text\":\"----------------------\",\"bold\":true,\"color\":\"#8600FF\"}"
                + "]";

        // DEBUG : Vérifie si la commande Tellraw est bien générée
        System.out.println("[DEBUG] Commande Tellraw envoyée: " + tellrawCommand);

        // Envoie la commande Tellraw
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + tellrawCommand);

        return true;
    }
}
