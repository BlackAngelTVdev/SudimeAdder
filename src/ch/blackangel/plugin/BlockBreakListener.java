package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BlockBreakListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Vérifie si le bloc cassé est de la stone
        if (block.getType() == Material.STONE) {
            // Vérifie si le joueur tient au moins une pioche en or
            if (mainHand.getType() == Material.GOLDEN_PICKAXE
                    || mainHand.getType() == Material.DIAMOND_PICKAXE
                    || mainHand.getType() == Material.NETHERITE_PICKAXE) {

                int chance = random.nextInt(1000); // 1 chance sur 1000
                if (chance == 0) { // Drop uniquement si le random tombe sur 0
                    ItemStack sudime = CustomItemCreator.getSudimeItem();
                    block.getWorld().dropItemNaturally(block.getLocation(), sudime);
                    player.sendMessage("§aVous avez trouvé un §6Sudime§a !");
                }
            }
        }
    }
}
