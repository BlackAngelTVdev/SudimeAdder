package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;

public class ArmorEffectManager implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey sudimeKey;

    public ArmorEffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.sudimeKey = new NamespacedKey(plugin, "sudime_armor");
        startArmorCheckTask();
    }

    private void startArmorCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isWearingFullSudimeArmor(player)) {
                        // Donne un effet avec une durée très longue (99999 ticks)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 99999, 0, true, false));
                    } else {
                        // Retire seulement si l’effet est présent
                        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Vérifie toutes les 1 seconde
    }

    private boolean isWearingFullSudimeArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            if (item == null || !item.hasItemMeta()) return false;
            if (!item.getItemMeta().getPersistentDataContainer().has(sudimeKey, PersistentDataType.BYTE)) {
                return false;
            }
        }
        return true;
    }
}
