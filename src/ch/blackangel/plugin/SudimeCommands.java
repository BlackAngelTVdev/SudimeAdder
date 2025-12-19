package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SudimeCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("give.sudimeadder")) {
            sender.sendMessage("§cTu n'as pas la permission d'utiliser cette commande !");
            return true;
        }

        // Vérifier si c'est la commande givesudime ou givelaxarium
        boolean isSudimeCommand = command.getName().equalsIgnoreCase("givesudime");
        boolean isLaxariumCommand = command.getName().equalsIgnoreCase("givelaxarium");

        if (!isSudimeCommand && !isLaxariumCommand) {
            return false;
        }

        String itemName = isSudimeCommand ? "Sudime" : "Laxarium";

        if (args.length == 0) {
            // Donne 1 item à soi-même
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack item = isSudimeCommand ? CustomItemCreator.getSudimeItem() : CustomItemCreator.getLaxariumItem();
                player.getInventory().addItem(item);
                player.sendMessage("§6Tu as reçu un " + itemName + " !");
            } else {
                sender.sendMessage("§cLa console doit spécifier un joueur !");
            }
            return true;
        }

        if (args.length == 1) {
            try {
                int amount = Integer.parseInt(args[0]);
                if (amount < 1) throw new NumberFormatException();

                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack itemStack = isSudimeCommand ? CustomItemCreator.getSudimeItem() : CustomItemCreator.getLaxariumItem();
                    itemStack.setAmount(amount);
                    player.getInventory().addItem(itemStack);
                    player.sendMessage("§6Tu as reçu " + amount + " " + itemName + "s !");
                } else {
                    sender.sendMessage("§cLa console doit spécifier un joueur !");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cVeuillez entrer un nombre valide !");
            }
            return true;
        }

        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cCe joueur n'est pas connecté !");
                return true;
            }
            try {
                int amount = Integer.parseInt(args[1]);
                if (amount < 1) throw new NumberFormatException();

                ItemStack itemStack = isSudimeCommand ? CustomItemCreator.getSudimeItem() : CustomItemCreator.getLaxariumItem();
                itemStack.setAmount(amount);
                target.getInventory().addItem(itemStack);
                sender.sendMessage("§6Tu as donné " + amount + " " + itemName + "s à " + target.getName() + " !");
                target.sendMessage("§6Tu as reçu " + amount + " " + itemName + "s !");
            } catch (NumberFormatException e) {
                sender.sendMessage("§cVeuillez entrer un nombre valide !");
            }
            return true;
        }

        sender.sendMessage("§cUtilisation : /" + command.getName() + " [joueur] [nombre]");
        return true;
    }
}