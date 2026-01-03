package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
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

    // Plus ce nombre est haut, plus la houe est résistante (ex: 2 = 2x plus de vie)
    private final int DURABILITY_MULTIPLIER = 2;

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
            // On peut ajouter un lore pour expliquer la durabilité
            hoe.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "sudime_hoe");
        ShapedRecipe recipe = new ShapedRecipe(key, hoe);
        recipe.shape("SS ", " X ", " X ");
        // Note: Assure-toi que CustomItemCreator existe bien dans ton projet
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem()));
        recipe.setIngredient('X', Material.STICK);

        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            ItemStack item = event.getItem();

            if (item != null && item.hasItemMeta() &&
                    item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(HOE_NAME)) {

                replantCrops(event.getClickedBlock());
                handleCustomDurability(item, event.getPlayer());
            }
        }
    }

    private void handleCustomDurability(ItemStack item, org.bukkit.entity.Player player) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof org.bukkit.inventory.meta.Damageable)) return;

        org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;

        // --- LOGIQUE DE DURABILITÉ ---

        // 1. Calcul du bonus "Unbreaking" (Solidité)
        // La formule officielle est 100/(level+1) % de chance de perdre de la durabilité
        int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);
        double chanceToDamage = 1.0 / (unbreakingLevel + 1);

        // 2. Application du multiplicateur custom (DURABILITY_MULTIPLIER)
        chanceToDamage = chanceToDamage / DURABILITY_MULTIPLIER;

        // 3. Test de chance : si le random est inférieur à notre chance, on applique le dégât
        if (random.nextDouble() <= chanceToDamage) {
            int newDamage = damageable.getDamage() + 1;
            int maxDurability = item.getType().getMaxDurability();

            if (newDamage >= maxDurability) {
                item.setAmount(0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                damageable.setDamage(newDamage);
                item.setItemMeta(meta);
            }
        }
    }

    // Le reste de tes méthodes (replantCrops, dropHarvest) reste inchangé
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