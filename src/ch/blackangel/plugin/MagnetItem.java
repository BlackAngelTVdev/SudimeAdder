package ch.blackangel.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.event.entity.EntityDamageByEntityEvent;


public class MagnetItem implements Listener {
    private final JavaPlugin plugin;
    private final Map<Player, Long> cooldowns = new HashMap<>(); // Cooldown pour éviter le spam

    // Clés persistantes
    private static final NamespacedKey MAGNET_KEY = new NamespacedKey("blackangel", "magnet_item");
    private static final NamespacedKey DURABILITY_KEY = new NamespacedKey("blackangel", "magnet_durability");
    private static final NamespacedKey ACTIVE_KEY = new NamespacedKey("blackangel", "magnet_active");

    // Constantes
    private static final int MAX_DURABILITY = 10000;
    private static final int MAGNET_RADIUS = 25;
    private static final double ATTRACTION_STRENGTH = 0.5;
    private static final long COOLDOWN_TIME = 500; // Temps en millisecondes

    public MagnetItem(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createMagnetItem() {
        ItemStack magnet = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = magnet.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§bMagnette");
            meta.setLore(Arrays.asList(
                    "§7Attire les items dans un rayon de " + MAGNET_RADIUS + " blocs",
                    "§eDurabilité : " + MAX_DURABILITY,
                    "§a[Activé]"
            ));

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(MAGNET_KEY, PersistentDataType.INTEGER, 1);
            pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, MAX_DURABILITY);
            pdc.set(ACTIVE_KEY, PersistentDataType.INTEGER, 1);

            magnet.setItemMeta(meta);
        }
        return magnet;
    }

    public void registerMagnetRecipe() {
        ItemStack magnet = createMagnetItem();
        ShapedRecipe recipe = new ShapedRecipe(NamespacedKey.minecraft("magnet_recipe"), magnet);

        recipe.shape(
                "I I",
                "S S",
                "SSS"
        );

        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(CustomItemCreator.getSudimeItem())); // Utilisation de Sudime

        plugin.getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMagnetItem(item)) return;

        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            long currentTime = System.currentTimeMillis();
            long lastUse = cooldowns.getOrDefault(player, 0L);

            if (currentTime - lastUse < COOLDOWN_TIME) {
                return; // Empêche le spam
            }

            cooldowns.put(player, currentTime);
            toggleMagnetState(player, item);
        }
    }

    private void toggleMagnetState(Player player, ItemStack magnetItem) {
        ItemMeta meta = magnetItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean isActive = pdc.getOrDefault(ACTIVE_KEY, PersistentDataType.INTEGER, 1) == 1;

        isActive = !isActive;
        pdc.set(ACTIVE_KEY, PersistentDataType.INTEGER, isActive ? 1 : 0);

        List<String> lore = meta.getLore();
        if (lore != null && lore.size() > 2) {
            lore.set(2, isActive ? "§a[Activé]" : "§c[Désactivé]");
            meta.setLore(lore);
        }

        magnetItem.setItemMeta(meta);
        player.sendMessage(isActive ? "§aMagnette activée !" : "§cMagnette désactivée !");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack magnetItem = findMagnetInInventory(player);

        if (magnetItem != null && isMagnetEnabled(magnetItem)) {
            attractNearbyItems(player, magnetItem);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            if (isMagnetItem(itemInMainHand)) {
                event.setCancelled(true);
                player.sendMessage("§cTu ne peux pas frapper avec la Magnette !");
            }
        }
    }

    private ItemStack findMagnetInInventory(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(MagnetItem::isMagnetItem)
                .findFirst()
                .orElse(null);
    }

    private boolean isMagnetEnabled(ItemStack item) {
        if (!isMagnetItem(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().getOrDefault(ACTIVE_KEY, PersistentDataType.INTEGER, 1) == 1;
    }

    private void attractNearbyItems(Player player, ItemStack magnetItem) {
        List<Item> attractedItems = player.getWorld().getEntitiesByClass(Item.class).stream()
                .filter(item -> item.getLocation().distance(player.getLocation()) <= MAGNET_RADIUS)
                .collect(Collectors.toList());

        if (!attractedItems.isEmpty()) {
            reduceMagnetDurability(player, magnetItem);
            for (Item item : attractedItems) {
                Vector direction = player.getLocation().toVector().subtract(item.getLocation().toVector()).normalize().multiply(ATTRACTION_STRENGTH);
                item.setVelocity(direction);

                if (item.getLocation().distance(player.getLocation()) < 2) {
                    Map<Integer, ItemStack> couldNotFit = player.getInventory().addItem(item.getItemStack());
                    if (couldNotFit.isEmpty()) {
                        item.remove();
                    }
                }
            }
        }
    }

    private void reduceMagnetDurability(Player player, ItemStack magnetItem) {
        ItemMeta meta = magnetItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int durability = pdc.getOrDefault(DURABILITY_KEY, PersistentDataType.INTEGER, MAX_DURABILITY);

        durability--;

        if (durability <= 0) {
            player.getInventory().remove(magnetItem);
            player.sendMessage("§cTa magnette est cassée !");
        } else {
            pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, durability);
            List<String> lore = meta.getLore();
            if (lore != null && lore.size() > 1) {
                lore.set(1, "§eDurabilité : " + durability);
                meta.setLore(lore);
            }
            magnetItem.setItemMeta(meta);
        }
    }

    public static boolean isMagnetItem(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SWORD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(MAGNET_KEY, PersistentDataType.INTEGER);
    }
}
