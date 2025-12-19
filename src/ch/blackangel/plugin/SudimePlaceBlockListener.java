package ch.blackangel.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SudimePlaceBlockListener implements Listener {

    public SudimePlaceBlockListener(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSudimePlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        // Vérifie si c’est bien une redstone
        if (item == null || item.getType() != Material.REDSTONE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Vérifie si le nom de l'item est "Sudime"
        if (ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("Sudime")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Tu ne peux pas poser le Sudime !");
        }
    }
}
