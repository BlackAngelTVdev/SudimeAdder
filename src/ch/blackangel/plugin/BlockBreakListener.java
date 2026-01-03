package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class BlockBreakListener implements Listener {

    private final Map<UUID, Integer> blockCount = new HashMap<>();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (block.getType() == Material.STONE) {
            if (isGoodPickaxe(mainHand)) {

                UUID uuid = player.getUniqueId();
                int currentCount = blockCount.getOrDefault(uuid, 0) + 1;

                if (currentCount >= 1000) {

                    ItemStack sudime = CustomItemCreator.getSudimeItem();
                    block.getWorld().dropItemNaturally(block.getLocation(), sudime);
                    player.sendMessage("§aVous avez trouvé un §6Sudime§a !");


                    blockCount.put(uuid, 0);
                } else {

                    blockCount.put(uuid, currentCount);
                }
            }
        }
    }

    // Petite méthode pour rendre le code plus propre
    private boolean isGoodPickaxe(ItemStack item) {
        Material type = item.getType();
        return type == Material.GOLDEN_PICKAXE || type == Material.DIAMOND_PICKAXE || type == Material.NETHERITE_PICKAXE;
    }
}
