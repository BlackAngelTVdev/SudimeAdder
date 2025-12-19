package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Chunk;
import org.bukkit.inventory.meta.ItemMeta;




public class ChestDetectorListener implements Listener {

    private final JavaPlugin plugin;
    private static final NamespacedKey DURABILITY_KEY;

    static {
        DURABILITY_KEY = new NamespacedKey("ch.blackangel.plugin", "durability");
    }

    public ChestDetectorListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void createCraft(JavaPlugin plugin) {
        ItemStack chestDetector = createChestDetector();
        NamespacedKey key = new NamespacedKey(plugin, "chest_detector");
        ShapedRecipe recipe = new ShapedRecipe(key, chestDetector);
        recipe.shape("FSF", "SCS", "FSF");
        recipe.setIngredient('F', Material.IRON_INGOT);
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem())); // Utilisation de Sudime
        recipe.setIngredient('C', Material.CHEST);
        Bukkit.addRecipe(recipe);
    }

    public static ItemStack createChestDetector() {
        ItemStack chestDetector = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = chestDetector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eDetecteur de Coffres");
            meta.setLore(java.util.Collections.singletonList("§7Détecte le nombre de coffres dans le chunk"));
            meta.getPersistentDataContainer().set(DURABILITY_KEY, PersistentDataType.INTEGER, 100);
            chestDetector.setItemMeta(meta);
        }
        return chestDetector;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();

        if (isChestDetector(itemInMainHand) && event.getTo() != null) {
            Chunk fromChunk = event.getFrom().getChunk();
            Chunk toChunk = event.getTo().getChunk();

            // Vérifie si le joueur a changé de chunk
            if (fromChunk.getX() != toChunk.getX() || fromChunk.getZ() != toChunk.getZ()) {
                int chestCount = countChestsInChunk(toChunk);
                player.sendMessage("§6Il y a " + chestCount + " coffres dans ce chunk.");
                reduceItemDurability(itemInMainHand, player);
            }
        }
    }


    private void reduceItemDurability(ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer data = meta.getPersistentDataContainer();
            int durability = data.getOrDefault(DURABILITY_KEY, PersistentDataType.INTEGER, 10);
            durability--;
            if (durability <= 0) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.sendMessage("§cTon détecteur de coffres est cassé !");
            } else {
                data.set(DURABILITY_KEY, PersistentDataType.INTEGER, durability);
                item.setItemMeta(meta);
            }
        }
    }

    private int countChestsInChunk(Chunk chunk) {
        int chestCount = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType() == Material.CHEST) {
                        chestCount++;
                    }
                }
            }
        }
        return chestCount;
    }

    private boolean isChestDetector(ItemStack item) {
        return item != null && item.getType() == Material.IRON_SWORD && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals("§eDetecteur de Coffres");
    }
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();

            if (isChestDetector(itemInMainHand)) {
                event.setCancelled(true);
                player.sendMessage("§cTu ne peux pas frapper avec le Détecteur de Coffres !");
            }
        }
    }

}

