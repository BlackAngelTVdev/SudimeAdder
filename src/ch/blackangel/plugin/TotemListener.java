package ch.blackangel.plugin;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TotemListener implements Listener {

    private final JavaPlugin plugin;
    private final Map<Zombie, Long> activeTotemTimers = new HashMap<>();
    private final Map<UUID, Long> lastInteraction = new HashMap<>(); // Anti-spam

    public TotemListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startGrowthBoost();
    }

    // UNE SEULE méthode pour gérer les interactions avec le totem
    @EventHandler
    public void onTotemInteract(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Zombie)) return;

        Zombie zombie = (Zombie) e.getRightClicked();
        String name = zombie.getCustomName();

        if (name == null || !name.contains("Totem")) return;

        // ANTI-DOUBLE CLICK : Vérifier si c'est un click trop rapide
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (lastInteraction.containsKey(playerId)) {
            long lastTime = lastInteraction.get(playerId);
            if (currentTime - lastTime < 500) { // 500ms = 0.5 seconde
                e.setCancelled(true);
                return; // Ignorer les clicks trop rapides
            }
        }
        lastInteraction.put(playerId, currentTime);

        e.setCancelled(true); // Toujours annuler l'événement

        ItemStack item = p.getInventory().getItemInMainHand();

        // Si c'est un totem invalide
        if (name.contains("Invalide")) {
            if (p.isSneaking()) {
                handleTotemBreak(zombie, p);
            } else {
                p.sendMessage("§cCe totem n'est pas sur une structure valide !");
                p.sendMessage("§7Maintiens §eShift §7pour le casser.");
            }
            return;
        }

        if (!name.contains("Totem de Fertilité")) return;

        // Si le joueur est en sneak et n'a rien en main -> CASSER
        if (p.isSneaking() && (item == null || item.getType() == Material.AIR)) {
            handleTotemBreak(zombie, p);
            return;
        }

        // Si le joueur a un bloc de diamant -> ACTIVER
        if (item != null && item.getType() == Material.DIAMOND_BLOCK) {
            handleTotemActivation(zombie, p, item);
            return;
        }

        // Sinon afficher les informations
        if (activeTotemTimers.containsKey(zombie)) {
            long timeLeft = (activeTotemTimers.get(zombie) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                p.sendMessage("§eTemps restant: §a" + formatTime(timeLeft));
            } else {
                p.sendMessage("§cTotem inactif ! Donne-lui un bloc de diamant.");
            }
        } else {
            p.sendMessage("§cTotem inactif ! Donne-lui un bloc de diamant.");
        }
        p.sendMessage("§7Shift + main vide pour casser le totem");
    }

    private void handleTotemActivation(Zombie zombie, Player p, ItemStack item) {
        // Vérifier si le totem n'est pas déjà en cours de traitement
        if (!zombie.isValid()) return;

        // Activer le totem pour 15 minutes
        long endTime = System.currentTimeMillis() + (15 * 60 * 1000);
        activeTotemTimers.put(zombie, endTime);

        zombie.setCustomName("§bTotem de Fertilité §a(Actif - 15:00)");

        item.setAmount(item.getAmount() - 1);
        p.sendMessage("§aTotem de Fertilité activé pour 15 minutes !");
        p.sendMessage("§eLes plantes poussent 5x plus vite dans ce chunk !");
    }

    private void handleTotemBreak(Zombie zombie, Player p) {
        // Vérifier si le zombie est toujours valide et n'a pas déjà été traité
        if (!zombie.isValid()) return;

        // Marquer le zombie comme en cours de suppression
        zombie.setInvulnerable(false); // Le rendre vulnérable temporairement
        zombie.setCustomName("§cTotem en cours de destruction...");

        // Supprimer le totem de la liste active
        activeTotemTimers.remove(zombie);

        // Drop le Totem de Fertilité custom (UN SEUL)
        Location loc = zombie.getLocation();
        loc.getWorld().dropItem(loc, TotemCraft.getTotemArmorStand());

        // Supprimer le zombie avec un petit délai pour éviter les doubles traitements
        new BukkitRunnable() {
            @Override
            public void run() {
                if (zombie.isValid()) {
                    zombie.remove();
                }
            }
        }.runTaskLater(plugin, 1L); // 1 tick de délai

        p.sendMessage("§aTotem récupéré !");
    }

    // AJOUTER cette méthode pour empêcher les dégâts normaux
    @EventHandler
    public void onTotemDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Zombie)) return;

        Zombie zombie = (Zombie) e.getEntity();
        String name = zombie.getCustomName();

        if (name != null && name.contains("Totem")) {
            e.setCancelled(true); // Annuler tous les dégâts
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent e) {
        // Empêcher les zombies totems de cibler des entités
        if (!(e.getEntity() instanceof Zombie)) return;

        Zombie zombie = (Zombie) e.getEntity();
        String name = zombie.getCustomName();

        if (name != null && name.contains("Totem")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent e) {
        // Empêcher les zombies totems de brûler au soleil
        if (!(e.getEntity() instanceof Zombie)) return;

        Zombie zombie = (Zombie) e.getEntity();
        String name = zombie.getCustomName();

        if (name != null && name.contains("Totem")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        ItemStack item = p.getInventory().getItemInMainHand();

        // Vérifier que c'est bien le Totem de Fertilité custom
        if (!TotemCraft.isTotemArmorStand(item)) return;

        e.setCancelled(true);

        Location loc = block.getLocation().add(0.5, 1, 0.5);
        Zombie zombie = (Zombie) p.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        // Configuration du zombie
        zombie.setAI(false); // Désactive l'IA (ne bouge pas)
        zombie.setInvulnerable(true); // Invulnérable
        zombie.setSilent(true); // Pas de sons
        zombie.setCollidable(false); // Ne peut pas être poussé
        zombie.setCanPickupItems(false); // Ne ramasse pas d'objets
        zombie.setAdult(); // Toujours adulte
        zombie.setAgeLock(true); // Verrouille l'âge

        // Ajouter la résistance au feu pour ne jamais brûler
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));

        // Vérifier si c'est sur la bonne structure
        if (block.getType() == Material.DIAMOND_BLOCK && checkObsidianAround(block)) {
            // Structure valide
            zombie.setCustomName("§bTotem de Fertilité §7(Inactif)");
            zombie.setCustomNameVisible(true);
            p.sendMessage("§aTotem posé !");
            p.sendMessage("§eClic droit avec un bloc de diamant pour l'activer !");
            p.sendMessage("§7Shift + main vide pour casser");
        } else {
            // Structure invalide
            zombie.setCustomName("§cTotem de Fertilité Invalide §7(Mauvaise structure)");
            zombie.setCustomNameVisible(true);
            p.sendMessage("§cStructure invalide !");
            p.sendMessage("§7Place-le sur un bloc de diamant entouré d'obsidienne.");
            p.sendMessage("§7Shift + main vide pour casser");
        }

        item.setAmount(item.getAmount() - 1);
    }

    private boolean checkObsidianAround(Block center) {
        Location loc = center.getLocation();

        int[][] offsets = {
                {-1, 0, -1}, {0, 0, -1}, {1, 0, -1},
                {-1, 0,  0},             {1, 0,  0},
                {-1, 0,  1}, {0, 0,  1}, {1, 0,  1}
        };

        for (int[] offset : offsets) {
            Block b = loc.clone().add(offset[0], offset[1], offset[2]).getBlock();
            if (b.getType() != Material.OBSIDIAN) {
                return false;
            }
        }

        return true;
    }

    private void startGrowthBoost() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                // Mettre à jour les timers et supprimer les totems expirés
                activeTotemTimers.entrySet().removeIf(entry -> {
                    Zombie zombie = entry.getKey();
                    long endTime = entry.getValue();

                    if (!zombie.isValid()) {
                        return true;
                    }

                    long timeLeft = (endTime - currentTime) / 1000;

                    if (timeLeft <= 0) {
                        zombie.setCustomName("§bTotem de Fertilité §7(Inactif)");
                        return true;
                    }

                    // Mettre à jour le nom avec le temps restant
                    zombie.setCustomName("§bTotem de Fertilité §a(Actif - " + formatTime(timeLeft) + ")");

                    // Faire tourner le zombie quand il est actif
                    Location loc = zombie.getLocation();
                    float newYaw = loc.getYaw() + 5; // Rotation de 5 degrés par tick
                    loc.setYaw(newYaw);
                    zombie.teleport(loc);

                    // Spawner des particules autour du zombie actif
                    Location particleLoc = zombie.getLocation().add(0, 1.25, 0);
                    zombie.getWorld().spawnParticle(
                            Particle.END_ROD,
                            particleLoc,
                            8, // nombre de particules
                            0.4, 0.4, 0.4, // spread X, Y, Z (toutes directions)
                            0.05 // vitesse
                    );

                    // Booster la croissance dans le chunk
                    boostGrowthInChunk(zombie.getLocation().getChunk());

                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void boostGrowthInChunk(Chunk chunk) {
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Block block = chunk.getWorld().getBlockAt(minX + x, y, minZ + z);

                    if (block.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) block.getBlockData();

                        if (Math.random() < 0.2 && ageable.getAge() < ageable.getMaximumAge()) {
                            ageable.setAge(ageable.getAge() + 1);
                            block.setBlockData(ageable);

                            // Spawner les particules de bone meal au-dessus de la plante
                            Location particleLoc = block.getLocation().add(0.5, 1.2, 0.5);
                            block.getWorld().spawnParticle(
                                    Particle.VILLAGER_HAPPY,
                                    particleLoc,
                                    3, // nombre de particules
                                    0.3, 0.1, 0.3, // spread X, Y, Z
                                    0 // vitesse
                            );
                        }
                    }
                }
            }
        }
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}