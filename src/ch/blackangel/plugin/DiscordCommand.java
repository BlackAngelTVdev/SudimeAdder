package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DiscordCommand implements CommandExecutor {

    private final Plugin plugin;

    public DiscordCommand(Plugin plugin) {
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
        String discordLink = config.getString("discord_link", "https://discord.gg/tonserveur"); // Lien par défaut

        // DEBUG : Vérifie si le lien est bien récupéré
        System.out.println("[DEBUG] Lien Discord récupéré du config.yml: " + discordLink);

        // Construction du message JSON cliquable
        String tellrawCommand = "[\"\","
                + "{\"text\":\"----------------------\",\"bold\":true,\"color\":\"#8600FF\"},"
                + "{\"text\":\"\\n\",\"bold\":true},"
                + "{\"text\":\"         Discord\",\"bold\":true,\"color\":\"gold\"},"
                + "{\"text\":\"\\n\",\"bold\":true},"
                + "{\"text\":\"       ▶\",\"bold\":true,\"color\":\"red\"},"
                + "{\"text\":\" Clic ici\",\"bold\":true,\"color\":\"blue\","
                + " \"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + discordLink + "\"}},"
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
