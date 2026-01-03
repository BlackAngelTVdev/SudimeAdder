package ch.blackangel.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RTPCommand implements CommandExecutor {
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long DEFAULT_COOLDOWN = 24 * 60 * 60 * 1000L; // 24 heures en millisecondes

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Si l'OP ou permission 4 => pas de limite
        if (hasNoLimit(player)) {
            Location safeLocation = getRandomSafeLocation(player.getWorld(), player.getLocation());
            if (safeLocation != null) {
                player.teleport(safeLocation);
                player.sendMessage("\u00a7aTéléporté avec succès ! (no limit)");
            } else {
                player.sendMessage("\u00a7cAucune position sûre trouvée, réessayez.");
            }
            return true;
        }

        long playerCooldown = getPlayerCooldownMillis(player); // en ms

        if (playerCooldown <= 0) {
            // Juste au cas où, mais normalement non atteint car handled par hasNoLimit
            Location safeLocation = getRandomSafeLocation(player.getWorld(), player.getLocation());
            if (safeLocation != null) {
                player.teleport(safeLocation);
                player.sendMessage("\u00a7aTéléporté avec succès !");
            } else {
                player.sendMessage("\u00a7cAucune position sûre trouvée, réessayez.");
            }
            return true;
        }

        if (cooldowns.containsKey(playerId)) {
            long lastUsed = cooldowns.get(playerId);
            long elapsed = currentTime - lastUsed;
            if (elapsed < playerCooldown) {
                long remainingSeconds = (playerCooldown - elapsed) / 1000;
                long hours = remainingSeconds / 3600;
                long minutes = (remainingSeconds % 3600) / 60;
                long seconds = remainingSeconds % 60;
                player.sendMessage("\u00a7cVous devez attendre encore " + hours + "h " + minutes + "m " + seconds + "s pour utiliser cette commande.");
                return true;
            }
        }

        World world = player.getWorld();
        Location safeLocation = getRandomSafeLocation(world, player.getLocation());

        if (safeLocation != null) {
            player.teleport(safeLocation);
            player.sendMessage("\u00a7aTéléporté avec succès !");
            cooldowns.put(playerId, currentTime);
        } else {
            player.sendMessage("\u00a7cAucune position sûre trouvée, réessayez.");
        }
        return true;
    }

    /**
     * Retourne true si le joueur a la permission de ne pas avoir de limite.
     * On considère aussi player.isOp() comme équivalent à pas de limite.
     */
    private boolean hasNoLimit(Player player) {
        return player.isOp() || player.hasPermission("bypassRTP.sudimeadder.4");
    }

    /**
     * Donne le cooldown spécifique du joueur selon sa permission :
     * - bypassRTP.sudimadder.1 => 1 heure
     * - bypassRTP.sudimadder.2 => 2 heures
     * - bypassRTP.sudimadder.3 => 3 heures
     * - sinon => DEFAULT_COOLDOWN (24h)
     *
     * Retourne la valeur en millisecondes.
     */
    private long getPlayerCooldownMillis(Player player) {
        if (player.hasPermission("bypassRTP.sudimeadder.1")) {
            return 1 * 60 * 60 * 1000L; // 1 heure
        }
        if (player.hasPermission("bypassRTP.sudimeadder.2")) {
            return 2 * 60 * 60 * 1000L; // 2 heures
        }
        if (player.hasPermission("bypassRTP.sudimeadder.3")) {
            return 3 * 60 * 60 * 1000L; // 3 heures
        }
        return DEFAULT_COOLDOWN;
    }

    /**
     * Tente de trouver une location sûre autour de l'origin.
     * Vérifie que le bloc de "ground" est solide et n'est ni eau ni lave.
     */
    private Location getRandomSafeLocation(World world, Location origin) {
        int maxAttempts = 20;
        for (int i = 0; i < maxAttempts; i++) {
            int dx = 3500 + random.nextInt(500);
            int dz = 3500 + random.nextInt(500);
            int x = origin.getBlockX() + (random.nextBoolean() ? dx : -dx);
            int z = origin.getBlockZ() + (random.nextBoolean() ? dz : -dz);

            // --- ZONE D'EXCLUSION ---
            // Vérifie si X est entre 117 et 776 ET si Z est entre -42 et 943
            // Note : On utilise Math.min/max pour être sûr que l'ordre des coordonnées ne casse pas le calcul
            boolean isInExcludedZone = (x >= Math.min(117, 776) && x <= Math.max(117, 776))
                    && (z >= Math.min(943, -42) && z <= Math.max(943, -42));

            if (isInExcludedZone) {
                continue; // Recommence la boucle pour trouver une autre coordonnée
            }
            // ------------------------

            int groundY = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x + 0.5, groundY + 1.0, z + 0.5);

            Material groundType = world.getBlockAt(x, groundY, z).getType();
            String name = groundType.name();
            boolean isWater = name.contains("WATER") || name.contains("KELP") || name.contains("SEAGRASS");
            boolean isLava = name.contains("LAVA");
            boolean isSolid = groundType.isSolid();

            Material spawnBlock = world.getBlockAt(loc).getType();
            Material aboveSpawn = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ()).getType();
            boolean spaceClear = spawnBlock == Material.AIR && aboveSpawn == Material.AIR;

            if (!isWater && !isLava && isSolid && spaceClear) {
                return loc;
            }
        }
        return null;
    }
}
