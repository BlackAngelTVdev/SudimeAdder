package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import java.util.Random;

public class Hoe implements Listener {

    private static final String HOE_NAME = "§bSudime Hoe";
    private final Plugin plugin;
    private final Random random = new Random();

    public Hoe(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        createHoeRecipe();
    }

    private void createHoeRecipe() {
        ItemStack hoe = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HOE_NAME);
            meta.setUnbreakable(false);
            hoe.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "sudime_hoe");
        ShapedRecipe recipe = new ShapedRecipe(key, hoe);
        recipe.shape("SS ", " X ", " X ");
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem()));
        recipe.setIngredient('X', Material.STICK);

        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getPlayer().getInventory().getItemInMainHand().hasItemMeta() &&
                    event.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(HOE_NAME)) {
                replantCrops(event.getClickedBlock());
                event.getItem().setDurability((short) (event.getItem().getDurability() + 1));
            }
        }
    }

    private void replantCrops(Block center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block relative = center.getRelative(x, 0, z);
                Material type = relative.getType();

                if (relative.getBlockData() instanceof Ageable) {
                    Ageable crop = (Ageable) relative.getBlockData();
                    if (crop.getAge() == crop.getMaximumAge()) {
                        dropHarvest(relative, type);
                        crop.setAge(0);
                        relative.setBlockData(crop);
                    }
                } else if (type == Material.COCOA) {
                    relative.setType(Material.AIR);
                    relative.setType(Material.COCOA);
                } else if (type == Material.SUGAR_CANE || type == Material.BAMBOO) {
                    while (relative.getRelative(0, 1, 0).getType() == type) {
                        relative.getRelative(0, 1, 0).setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void dropHarvest(Block block, Material type) {
        switch (type) {
            case WHEAT:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.WHEAT, 1));
                break;
            case CARROTS:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CARROT, 1));
                break;
            case POTATOES:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.POTATO, 1));
                break;
            case BEETROOTS:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.BEETROOT, 1));
                break;
            case COCOA:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.COCOA_BEANS, random.nextInt(3) + 1));
                break;
            case MELON:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.MELON_SLICE, random.nextInt(3) + 3));
                break;
            case PUMPKIN:
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.PUMPKIN, 1));
                break;
            default:
                break;
        }
    }
}