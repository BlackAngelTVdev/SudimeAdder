package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FurnaceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette commande !");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("furnace.sudimeadder")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cette commande !");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cTu dois avoir un item dans ta main !");
            return true;
        }

        Material smeltedMaterial = getSmeltedItem(itemInHand.getType());
        if (smeltedMaterial == null) {
            player.sendMessage("§cCet item ne peut pas être cuit !");
            return true;
        }

        itemInHand.setType(smeltedMaterial);
        player.sendMessage("§6Ton item a été cuit avec succès !");
        return true;
    }

    private Material getSmeltedItem(Material material) {
        switch (material) {
            case IRON_ORE: return Material.IRON_INGOT;
            case GOLD_ORE: return Material.GOLD_INGOT;
            case COBBLESTONE: return Material.STONE;
            case SAND: return Material.GLASS;
            case CLAY_BALL: return Material.BRICK;
            case BEEF: return Material.COOKED_BEEF;
            case CHICKEN: return Material.COOKED_CHICKEN;
            case PORKCHOP: return Material.COOKED_PORKCHOP;
            case POTATO: return Material.BAKED_POTATO;
            case MUTTON: return Material.COOKED_MUTTON;
            case RABBIT: return Material.COOKED_RABBIT;
            case SALMON: return Material.COOKED_SALMON;
            case COD: return Material.COOKED_COD;
            case NETHERRACK: return Material.NETHER_BRICK;
            case CLAY: return Material.TERRACOTTA;
            case KELP: return Material.DRIED_KELP;
            case CACTUS: return Material.GREEN_DYE;
            case WET_SPONGE: return Material.SPONGE;
            default: return null;
        }
    }
}