package ch.blackangel.plugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.*;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;


public class MagicSticksListener implements Listener {

    private final JavaPlugin plugin;
    private final MagicSticks magicSticks;

    // Cooldowns pour chaque joueur
    private final Map<UUID, Long> cooldownGood = new HashMap<>();
    private final Map<UUID, Long> cooldownView = new HashMap<>();
    private final Map<UUID, Long> cooldownTNT = new HashMap<>();

    // Joueurs en mode spectateur
    private final Map<UUID, Location> spectatorLocations = new HashMap<>();

    public MagicSticksListener(JavaPlugin plugin, MagicSticks magicSticks) {
        this.plugin = plugin;
        this.magicSticks = magicSticks;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Si le joueur déco alors qu'il est en plein "Stick of View"
        if (spectatorLocations.containsKey(playerId)) {
            // On le TP à sa position de départ immédiatement
            player.teleport(spectatorLocations.get(playerId));
            // On le remet en survie
            player.setGameMode(GameMode.SURVIVAL);

            // On nettoie la liste pour ne pas laisser de données inutiles
            spectatorLocations.remove(playerId);
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Vérifier que c'est un clic droit
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Empêcher l'utilisation normale si c'est un stick magique
        if (magicSticks.isStickOfGood(item) || magicSticks.isStickOfView(item) || magicSticks.isStickOfTNT(item)) {
            event.setCancelled(true);
        }

        // Stick of Good - Soins
        if (magicSticks.isStickOfGood(item)) {
            useStickOfGood(player, item);
            return;
        }

        // Stick of View - Spectateur
        if (magicSticks.isStickOfView(item)) {
            useStickOfView(player, item);
            return;
        }

        // Stick of TNT - Explosions
        if (magicSticks.isStickOfTNT(item)) {
            useStickOfTNT(player, item);
            return;
        }
    }

    private void useStickOfGood(Player player, ItemStack stick) {
        UUID playerId = player.getUniqueId();

        // Vérifier le cooldown (30 secondes au lieu de 1 minute)
        if (cooldownGood.containsKey(playerId)) {
            long secondsLeft = ((cooldownGood.get(playerId) + 30000) - System.currentTimeMillis()) / 1000; // 30000ms = 30s
            if (secondsLeft > 0) {
                player.sendMessage("§cEncore " + secondsLeft + " secondes avant de pouvoir réutiliser le Stick of Good !");
                return;
            }
        }

        // Vérifier les utilisations restantes
        int uses = magicSticks.getUses(stick);
        if (uses <= 0) {
            player.sendMessage("§cLe Stick of Good n'a plus d'utilisations !");
            return;
        }

        // Appliquer les effets de soin
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 1));
        player.setHealth(Math.min(player.getHealth() + 10, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));

        // Effets visuels et sonores
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 10, 1, 1, 1);
        player.sendMessage("§a✦ Le Stick of Good vous soigne magnifiquement !");

        // Réduire la durabilité
        magicSticks.setUses(stick, uses - 1);

        // Appliquer le cooldown
        cooldownGood.put(playerId, System.currentTimeMillis());

        // Mettre à jour l'item
        updateStickInHand(player, stick);
    }

    private void useStickOfView(Player player, ItemStack stick) {
        UUID playerId = player.getUniqueId();

        // Vérifier le cooldown (1 minute)
        if (cooldownView.containsKey(playerId)) {
            long secondsLeft = ((cooldownView.get(playerId) + 30000) - System.currentTimeMillis()) / 1000;
            if (secondsLeft > 0) {
                player.sendMessage("§cEncore " + secondsLeft + " secondes avant de pouvoir réutiliser le Stick of View !");
                return;
            }
        }

        // Vérifier les utilisations restantes
        int uses = magicSticks.getUses(stick);
        if (uses <= 0) {
            player.sendMessage("§cLe Stick of View n'a plus d'utilisations !");
            return;
        }

        // Sauvegarder la position actuelle
        spectatorLocations.put(playerId, player.getLocation());

        // Passer en mode spectateur
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§b✦ Mode spectateur activé pendant 10 secondes !");

        // Effets sonores
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Réduire la durabilité
        magicSticks.setUses(stick, uses - 1);

        // Appliquer le cooldown
        cooldownView.put(playerId, System.currentTimeMillis());

        // Mettre à jour l'item
        updateStickInHand(player, stick);

        // ✅ CORRECTION : Utiliser 'plugin' au lieu de 'this'
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
                    Location returnLoc = spectatorLocations.get(playerId);
                    if (returnLoc != null) {
                        player.teleport(returnLoc);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.sendMessage("§a✦ Retour au mode normal !");
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    }
                    spectatorLocations.remove(playerId);
                }
            }
        }.runTaskLater(plugin, 20 * 10); // 10 secondes
        new BukkitRunnable() {
            @Override
            public void run() {
                // On vérifie si le joueur est en ligne ET s'il est toujours dans notre liste
                if (player.isOnline() && spectatorLocations.containsKey(playerId)) {
                    Location returnLoc = spectatorLocations.get(playerId);
                    player.teleport(returnLoc);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage("§a✦ Retour au mode normal !");
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

                    // On retire le joueur de la liste une fois terminé
                    spectatorLocations.remove(playerId);
                }
            }
        }.runTaskLater(plugin, 20 * 10);
    }

    private void useStickOfTNT(Player player, ItemStack stick) {
        UUID playerId = player.getUniqueId();

        // Vérifier le cooldown (3 secondes au lieu de 30)
        if (cooldownTNT.containsKey(playerId)) {
            long secondsLeft = ((cooldownTNT.get(playerId) + 3000) - System.currentTimeMillis()) / 1000; // 3000ms = 3s
            if (secondsLeft > 0) {
                player.sendMessage("§cEncore " + secondsLeft + " secondes avant de pouvoir réutiliser le Stick of TNT !");
                return;
            }
        }

        // Vérifier les utilisations restantes
        int uses = magicSticks.getUses(stick);
        if (uses <= 0) {
            player.sendMessage("§cLe Stick of TNT n'a plus d'utilisations !");
            return;
        }

        // Lancer une TNT allumée
        Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2));
        TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class);
        tnt.setFuseTicks(40);
        tnt.setVelocity(player.getLocation().getDirection().multiply(1.5));

        // Effets sonores et visuels
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnLoc, 5);
        player.sendMessage("§c✦ TNT lancée !");

        // Réduire la durabilité
        magicSticks.setUses(stick, uses - 1);

        // Appliquer le cooldown
        cooldownTNT.put(playerId, System.currentTimeMillis());

        // Mettre à jour l'item
        updateStickInHand(player, stick);
    }




    private void updateStickInHand(Player player, ItemStack stick) {
        // Mettre à jour l'item dans la main du joueur
        if (player.getInventory().getItemInMainHand().isSimilar(stick)) {
            player.getInventory().setItemInMainHand(stick);
        } else if (player.getInventory().getItemInOffHand().isSimilar(stick)) {
            player.getInventory().setItemInOffHand(stick);
        }

        // Vérifier si l'item est épuisé
        int uses = magicSticks.getUses(stick);
        if (uses <= 0) {
            player.sendMessage("§c✖ Votre stick magique s'est épuisé !");
            // Optionnel: supprimer l'item quand il est épuisé
            if (player.getInventory().getItemInMainHand().isSimilar(stick)) {
                player.getInventory().setItemInMainHand(null);
            } else if (player.getInventory().getItemInOffHand().isSimilar(stick)) {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }
}