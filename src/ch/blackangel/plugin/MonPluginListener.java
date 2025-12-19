package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MonPluginListener implements Listener {

    private final JavaPlugin plugin;

    public MonPluginListener(JavaPlugin plugin) {
        this.plugin = plugin;
        // On enregistre directement le listener ici
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.END_PORTAL_FRAME) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ENDER_EYE) return;

        // Annule l'action côté Bukkit
        event.setCancelled(true);

        // Sécurité : restaure l'état du bloc après le tick courant
        Bukkit.getScheduler().runTaskLater(
                plugin, // ici on utilise directement le plugin
                () -> block.getState().update(true, false), // force la réactualisation du bloc sans œil
                1L // 1 tick plus tard
        );

        event.getPlayer().sendMessage("§cVous ne pouvez pas activer ce portail ici !");
    }
}
