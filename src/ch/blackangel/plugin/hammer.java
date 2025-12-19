package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class hammer implements Listener {

    private static final String HAMMER_NAME = "§bSudime Hammer";
    private final Plugin plugin;

    public hammer(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        createHammerRecipe();
    }

    private void createHammerRecipe() {
        ItemStack hammer = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = hammer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HAMMER_NAME);
            meta.setUnbreakable(false);
            hammer.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "sudime_hammer");
        ShapedRecipe recipe = new ShapedRecipe(key, hammer);
        recipe.shape("SSS", "SXS", " X ");
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem())); // Utilisation de Sudime
        recipe.setIngredient('X', Material.STICK);

        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().hasItemMeta() &&
                event.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(HAMMER_NAME)) {

            breakBlocks(event.getBlock(), event);
        }
    }

    private void breakBlocks(Block center, BlockBreakEvent event) {
        Set<Block> blocksToBreak = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block relative = center.getRelative(x, y, z);
                    if (!relative.equals(center) && relative.getType() != Material.BEDROCK) {
                        blocksToBreak.add(relative);
                    }
                }
            }
        }

        for (Block block : blocksToBreak) {
            block.breakNaturally(event.getPlayer().getInventory().getItemInMainHand());
        }
    }
}
