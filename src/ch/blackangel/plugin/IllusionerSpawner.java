package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import java.util.Random;

public class IllusionerSpawner implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public IllusionerSpawner(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startIllusionerSpawning();
    }

    private void startIllusionerSpawning() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // ➡️ AJOUT DE LA VÉRIFICATION DE CONFIGURATION ICI ⬅️
                if (!plugin.getConfig().getBoolean("event.enabled", false)) {
                    plugin.getLogger().fine("Le spawning de l'Illusionniste est désactivé par la configuration.");
                    return; // Ne rien faire si l'événement n'est pas activé
                }

                // 5 illusionnistes par heure = environ 1 toutes les 12 minutes
                if (shouldSpawnIllusioner()) {
                    spawnIllusionerNearPlayer();
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // Vérifie toutes les minutes
    }

    private boolean shouldSpawnIllusioner() {
        // 5 par heure = environ 8.3% de chance par minute
        return random.nextDouble() < 0.083;
    }

    private void spawnIllusionerNearPlayer() {
        // Trouver un joueur aléatoire en ligne
        Player[] onlinePlayers = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (onlinePlayers.length == 0) return;

        Player targetPlayer = onlinePlayers[random.nextInt(onlinePlayers.length)];

        // Générer une position aléatoire près du joueur (15-30 blocs)
        Location spawnLocation = getRandomLocationNearPlayer(targetPlayer, 15, 30);

        if (spawnLocation != null) {
            Illusioner illusioner = (Illusioner) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ILLUSIONER);

            // --- Configuration de la Santé ---
            double customMaxHealth = 150.0; // Par exemple, 50 points de vie (25 cœurs)

            // S'assurer que l'entité a l'attribut General.MAX_HEALTH (c'est le cas pour les mobs)
            AttributeInstance maxHealthAttribute = illusioner.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttribute != null) {
                // 1. Définir la Santé Maximale
                maxHealthAttribute.setBaseValue(customMaxHealth);
            }

            // 2. Définir la Santé Actuelle (la mettre à la valeur max)
            illusioner.setHealth(customMaxHealth);
            // ---------------------------------

            // Configuration de l'illusionniste
            illusioner.setCustomName("§5Illusionniste Mystérieux");
            illusioner.setCustomNameVisible(true);
            illusioner.setRemoveWhenFarAway(true);

            // Message d'avertissement au joueur le plus proche
            Player nearestPlayer = getNearestPlayer(spawnLocation, 20);
            if (nearestPlayer != null) {
                nearestPlayer.sendMessage("§5Un Illusionniste mystérieux apparaît dans les parages...");
                nearestPlayer.playSound(nearestPlayer.getLocation(),
                        org.bukkit.Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.0f);
            }

            // Log console seulement (pas de message de coordonnées)
            plugin.getLogger().info("Illusionniste spawné près de " + targetPlayer.getName());
        }
    }

    @EventHandler
    public void onIllusionerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Illusioner)) return;

        Illusioner illusioner = (Illusioner) event.getEntity();

        // Vérifier si c'est notre illusionniste custom
        if ("§5Illusionniste Mystérieux".equals(illusioner.getCustomName())) {
            // Clear les drops normaux
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Ajouter le drop de Sudime
            ItemStack sudime = CustomItemCreator.getSudimeItem();
            int amount = 1 + random.nextInt(3); // 1 à 3 Sudimes
            sudime.setAmount(amount);

            // Drop le Sudime
            illusioner.getWorld().dropItemNaturally(illusioner.getLocation(), sudime);

            // Message aux joueurs proches
            for (Player player : illusioner.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(illusioner.getLocation()) <= 400) { // 20 blocs de rayon
                    player.sendMessage("§6L'Illusionniste a lâché §e" + amount + " Sudime(s)§6 !");
                }
            }
        }
    }

    private Location getRandomLocationNearPlayer(Player player, int minDistance, int maxDistance) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        for (int attempts = 0; attempts < 10; attempts++) {
            // Générer une position aléatoire dans un cercle autour du joueur
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);

            double x = playerLoc.getX() + Math.cos(angle) * distance;
            double z = playerLoc.getZ() + Math.sin(angle) * distance;

            // Trouver le Y le plus haut à cette position
            int y = world.getHighestBlockYAt((int) x, (int) z);
            Location spawnLoc = new Location(world, x, y + 1, z);

            // Vérifier que la position est safe (pas dans l'eau, pas dans le vide, etc.)
            if (isSafeLocation(spawnLoc)) {
                return spawnLoc;
            }
        }

        return null; // Aucune position safe trouvée
    }

    private boolean isSafeLocation(Location location) {
        // Vérifier que le bloc en dessous est solide
        if (!location.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            return false;
        }

        // Vérifier que le bloc de spawn n'est pas dangereux (lave, eau, etc.)
        if (location.getBlock().isLiquid() ||
                location.getBlock().getType().name().contains("LAVA") ||
                location.getBlock().getType().name().contains("WATER")) {
            return false;
        }

        // Vérifier qu'il y a de l'espace pour spawn (au moins 2 blocs de haut)
        if (!location.clone().add(0, 1, 0).getBlock().isEmpty() ||
                !location.clone().add(0, 2, 0).getBlock().isEmpty()) {
            return false;
        }

        return true;
    }

    private Player getNearestPlayer(Location location, double radius) {
        Player nearest = null;
        double nearestDistance = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    // Méthode pour arrêter le spawn si nécessaire
    public void stopSpawning() {

    }
}