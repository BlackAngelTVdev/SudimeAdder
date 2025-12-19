package ch.blackangel.plugin;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GiantBoss implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private Giant boss;
    private BossBar bossBar;
    private int currentWave = 0;
    private boolean bossFightActive = false;
    private Location bossLocation;
    private final List<LivingEntity> waveEntities = new ArrayList<>();
    private final Map<UUID, Double> playerDamage = new HashMap<>();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public GiantBoss(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("spawnboss")) return false;

        if (!plugin.getConfig().getBoolean("event.enabled", false)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que sur un serveur d'événement !");
            return true;
        }

        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.isOp()) {
                p.sendMessage("§cTu n'as pas la permission d'exécuter cette commande !");
                return true;
            }
        }

        if (args.length != 3) {
            sender.sendMessage("§cUsage : /spawnboss <x> <y> <z>");
            return true;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);

            World world;
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                world = Bukkit.getWorlds().get(0);
            }

            Location loc = new Location(world, x, y, z);

            if (bossFightActive) {
                sender.sendMessage("§cUn boss est déjà actif !");
                return true;
            }

            spawnBoss(loc);
            sender.sendMessage("§aLe Boss Géant a été invoqué en " + x + " " + y + " " + z + " !");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cLes coordonnées doivent être des nombres !");
        }

        return true;
    }

    private void spawnBoss(Location loc) {
        bossFightActive = true;
        bossLocation = loc;
        currentWave = 0;
        playerDamage.clear();

        boss = (Giant) loc.getWorld().spawnEntity(loc, EntityType.GIANT);
        boss.setCustomName("§4§lBOSS GÉANT");
        boss.setCustomNameVisible(true);
        boss.setAI(false);
        boss.setRemoveWhenFarAway(false);
        boss.setInvulnerable(true);

        Objects.requireNonNull(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(2000);
        boss.setHealth(2000);
        Objects.requireNonNull(boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(1.0);

        bossBar = Bukkit.createBossBar("§4§lBOSS GÉANT", BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);

        startBossFight();
        startBossMovement();
        startBossContactKnockback();

        for (Player p : Bukkit.getOnlinePlayers()) {
            teleportAroundBoss(p);
            bossBar.addPlayer(p);
        }
    }

    private void startBossMovement() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (boss == null || !boss.isValid() || boss.isDead() || !bossFightActive) {
                    cancel();
                    return;
                }

                Player target = getClosestPlayer(boss.getLocation(), 50);
                if (target == null) return;

                Location bossLoc = boss.getLocation();
                Vector dir = target.getLocation().toVector().subtract(bossLoc.toVector());
                dir.setY(0).normalize();

                double step = 0.2;
                Location next = bossLoc.clone().add(dir.multiply(step));
                next.setY(bossLoc.getY());

                float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
                next.setYaw(yaw);

                boss.teleport(next);
                bossLocation = next;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private Player getClosestPlayer(Location loc, double radius) {
        Player closest = null;
        double closestDist = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            double dist = p.getLocation().distanceSquared(loc);
            if (dist < closestDist) {
                closestDist = dist;
                closest = p;
            }
        }
        return closest;
    }

    private void startBossFight() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossFightActive || boss == null || !boss.isValid() || boss.isDead()) {
                    endBossFight(false);
                    cancel();
                    return;
                }

                double maxHealth = Objects.requireNonNull(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
                double healthPercent = boss.getHealth() / maxHealth;
                bossBar.setProgress(Math.max(0, healthPercent));

                if (Math.random() < 0.15) {
                    lightningAttack();
                }

                waveEntities.removeIf(entity -> entity.isDead() || !entity.isValid());

                if (waveEntities.isEmpty() && currentWave < 4) {
                    currentWave++;
                    announceWave();
                    spawnWave();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void announceWave() {
        String message;
        switch (currentWave) {
            case 1:
                message = "§c§l=== VAGUE 1 : INVASION ===";
                break;
            case 2:
                message = "§c§l=== VAGUE 2 : ARMÉE DE FER ===";
                break;
            case 3:
                message = "§c§l=== VAGUE 3 : FORCES ENFLAMMÉES ===";
                break;
            case 4:
                message = "§c§l=== COMBAT FINAL : ÉLITE DE DIAMANT ===";
                break;
            default:
                return;
        }

        for (Player p : getNearbyPlayers(50)) {
            p.sendMessage(message);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
    }

    private void spawnWave() {
        List<Player> players = getNearbyPlayers(50);
        int playerCount = Math.max(1, players.size());

        switch (currentWave) {
            case 1:
                spawnWave1(playerCount);
                break;
            case 2:
                spawnWave2(playerCount);
                break;
            case 3:
                spawnWave3(playerCount);
                break;
            case 4:
                spawnWave4(playerCount);
                startBlockDestruction();
                break;
        }
    }

    private void spawnWave1(int playerCount) {
        int mobCount = playerCount * 10;

        for (int i = 0; i < mobCount; i++) {
            Location spawnLoc = getRandomLocationAround(bossLocation, 15);

            if (Math.random() < 0.5) {
                Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                zombie.setCustomName("§cZombie de la Vague 1");
                Objects.requireNonNull(zombie.getEquipment()).setHelmet(new ItemStack(Material.LEATHER_HELMET));
                waveEntities.add(zombie);
            } else {
                Skeleton skeleton = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
                skeleton.setCustomName("§cSquelette de la Vague 1");
                Objects.requireNonNull(skeleton.getEquipment()).setHelmet(new ItemStack(Material.LEATHER_HELMET));
                waveEntities.add(skeleton);
            }
        }
    }

    private void spawnWave2(int playerCount) {
        int mobCount = playerCount * 12;

        for (int i = 0; i < mobCount; i++) {
            Location spawnLoc = getRandomLocationAround(bossLocation, 15);

            if (Math.random() < 0.5) {
                Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                zombie.setCustomName("§6Zombie Blindé");
                equipIronArmor(zombie);
                waveEntities.add(zombie);
            } else {
                Skeleton skeleton = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
                skeleton.setCustomName("§6Squelette Blindé");
                equipIronArmor(skeleton);
                waveEntities.add(skeleton);
            }
        }
    }

    private void spawnWave3(int playerCount) {
        int mobCount = playerCount * 17;

        for (int i = 0; i < mobCount; i++) {
            Location spawnLoc = getRandomLocationAround(bossLocation, 15);

            if (Math.random() < 0.5) {
                Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                zombie.setCustomName("§cZombie Enflammé");
                equipIronArmor(zombie);
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                Objects.requireNonNull(zombie.getEquipment()).setItemInMainHand(sword);
                waveEntities.add(zombie);
            } else {
                Skeleton skeleton = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
                skeleton.setCustomName("§cSquelette Empoisonneur");
                equipIronArmor(skeleton);
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.ARROW_DAMAGE, 2);
                Objects.requireNonNull(skeleton.getEquipment()).setItemInMainHand(bow);
                ItemStack tippedArrow = new ItemStack(Material.TIPPED_ARROW, 64);
                PotionMeta meta = (PotionMeta) tippedArrow.getItemMeta();
                meta.setBasePotionData(new PotionData(PotionType.POISON));
                tippedArrow.setItemMeta(meta);
                skeleton.getEquipment().setItemInOffHand(tippedArrow);
                waveEntities.add(skeleton);
            }
        }
    }

    private void spawnWave4(int playerCount) {
        int mobCount = playerCount * 25;
        boss.setInvulnerable(false);

        for (int i = 0; i < mobCount; i++) {
            Location spawnLoc = getRandomLocationAround(bossLocation, 15);

            if (Math.random() < 0.5) {
                Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                zombie.setCustomName("§b§lÉLITE ZOMBIE");
                equipDiamondArmor(zombie);
                waveEntities.add(zombie);
            } else {
                Skeleton skeleton = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
                skeleton.setCustomName("§b§lÉLITE SQUELETTE");
                equipDiamondArmor(skeleton);
                waveEntities.add(skeleton);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossFightActive || boss == null || !boss.isValid() || boss.isDead()) {
                    cancel();
                    return;
                }

                int currentPlayers = getNearbyPlayers(50).size();
                int targetMobCount = currentPlayers * 25;

                waveEntities.removeIf(entity -> entity.isDead() || !entity.isValid());

                int toSpawn = targetMobCount - waveEntities.size();
                for (int i = 0; i < toSpawn; i++) {
                    Location spawnLoc = getRandomLocationAround(bossLocation, 15);

                    if (Math.random() < 0.5) {
                        Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                        zombie.setCustomName("§b§lÉLITE ZOMBIE");
                        equipDiamondArmor(zombie);
                        waveEntities.add(zombie);
                    } else {
                        Skeleton skeleton = (Skeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
                        skeleton.setCustomName("§b§lÉLITE SQUELETTE");
                        equipDiamondArmor(skeleton);
                        waveEntities.add(skeleton);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void equipIronArmor(LivingEntity entity) {
        Objects.requireNonNull(entity.getEquipment()).setHelmet(new ItemStack(Material.IRON_HELMET));
        entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        entity.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    private void equipDiamondArmor(LivingEntity entity) {
        Objects.requireNonNull(entity.getEquipment()).setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        entity.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        entity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
        entity.getEquipment().setItemInMainHand(sword);
    }

    private void lightningAttack() {
        List<Player> players = getNearbyPlayers(50);
        if (players.isEmpty()) return;

        Player target = players.get(new Random().nextInt(players.size()));
        target.getWorld().strikeLightningEffect(target.getLocation()); // strikeLightningEffect pour éviter les dégâts réels
    }

    private void startBlockDestruction() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossFightActive || boss == null || !boss.isValid() || boss.isDead() || currentWave != 4) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 3; i++) {
                    Location loc = getRandomLocationAround(bossLocation, 20);
                    if (loc.getBlock().getType() != Material.AIR && loc.getBlock().getType() != Material.BEDROCK) {
                        loc.getBlock().setType(Material.AIR);
                        loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1);
                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private void startBossContactKnockback() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (boss == null || !boss.isValid() || boss.isDead() || !bossFightActive) {
                    cancel();
                    return;
                }

                if (Math.random() < 1.0 / 6.0) {
                    for (Entity entity : boss.getNearbyEntities(3, 3, 3)) {
                        if (entity instanceof Player) {
                            Player p = (Player) entity;
                            Vector direction = p.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize();
                            direction.multiply(2.5).setY(1.2);

                            p.setVelocity(direction);
                            p.damage(10.0, boss);
                            p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Giant)) return;
        if (boss == null || !e.getEntity().getUniqueId().equals(boss.getUniqueId())) return;

        // Si le boss n'est pas encore à la vague 4, il est invincible
        if (currentWave < 4) {
            e.setCancelled(true);
            if (e.getDamager() instanceof Player) {
                ((Player) e.getDamager()).sendMessage("§cLe boss est invincible pour le moment !");
            }
            return;
        }

        // Détecte le joueur qui a infligé le dégât
        Player damager = null;
        if (e.getDamager() instanceof Player) {
            damager = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) e.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }

        // Ajoute les dégâts infligés au total du joueur
        if (damager != null) {
            UUID playerId = damager.getUniqueId();
            double currentDamage = playerDamage.getOrDefault(playerId, 0.0);
            double newDamage = currentDamage + e.getFinalDamage();
            playerDamage.put(playerId, newDamage);

            // Optionnel : log console pour debug
            plugin.getLogger().info("[BossFight] " + damager.getName() + " -> " + e.getFinalDamage() + " dmg (total: " + newDamage + ")");
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        // Vérifie que la mort concerne bien un boss (ici un Giant)
        if (!(event.getEntity() instanceof Giant)) return;

        // Vérifie que c'est bien le boss actif
        if (boss == null || !event.getEntity().equals(boss)) return;

        // Empêche les drops et l'XP normaux
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Vérifie si le boss a bien un "dernier coup" d'un joueur
        Player killer = event.getEntity().getKiller();

        // Annonce publique de la victoire
        Bukkit.broadcastMessage("§a==============================");
        Bukkit.broadcastMessage("§6Le §cBoss Géant §6a été vaincu !");
        if (killer != null) {
            Bukkit.broadcastMessage("§eLe héros du combat est §b" + killer.getName() + "§e !");
        }
        Bukkit.broadcastMessage("§a==============================");

        // Effets visuels
        event.getEntity().getWorld().strikeLightningEffect(event.getEntity().getLocation());

        // Distribuer les récompenses
        distributeRewards();

        // Nettoyage
        endBossFight(true);

        // Message final
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§7Un nouveau boss apparaîtra bientôt...");
        }, 20L * 60); // 60 secondes plus tard
    }

    private void distributeRewards() {
        if (playerDamage.isEmpty()) return;

        List<Map.Entry<UUID, Double>> sortedPlayers = new ArrayList<>(playerDamage.entrySet());
        sortedPlayers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Bukkit.broadcastMessage("§6§l=== RÉCOMPENSES DU BOSS ===");

        for (Map.Entry<UUID, Double> entry : sortedPlayers) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            double damage = entry.getValue();

            // Donne l'argent
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + p.getName() + " 5000");

            // Calcule la quantité de Laxarium à donner
            int laxariumCount = calculateLaxarium(damage);

            if (laxariumCount > 0) {
                ItemStack laxarium = CustomItemCreator.getLaxariumItem();
                laxarium.setAmount(laxariumCount);

                // Si l'inventaire a de la place, on donne directement
                if (p.getInventory().firstEmpty() != -1) {
                    p.getInventory().addItem(laxarium);
                    p.sendMessage("§aVous avez reçu §e5000$ §aet §9" + laxariumCount + " Laxarium §a!");
                } else {
                    p.sendMessage("§eVotre inventaire est plein ! Vos §9Laxarium §eseront ajoutés dès qu'une place se libérera.");

                    // On garde une référence finale pour le scheduler
                    final ItemStack pending = laxarium.clone();

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!p.isOnline()) {
                                cancel();
                                return;
                            }

                            if (p.getInventory().firstEmpty() != -1) {
                                p.getInventory().addItem(pending);
                                p.sendMessage("§aVos §9Laxarium §aont été ajoutés à votre inventaire !");
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 20L, 20L); // vérifie toutes les secondes (20 ticks)
                }
            }

            Bukkit.broadcastMessage("§e" + p.getName() + " §7a infligé §c" + String.format("%.1f", damage) + " dégâts §7et reçu §9" + laxariumCount + " Laxarium");
        }

        playerDamage.clear();
    }

    private int calculateLaxarium(double damage) {
        if (damage < 100) return 0;
        if (damage < 200) return 1;
        if (damage < 400) return 2;
        if (damage < 700) return 3;
        if (damage < 1000) return 4;
        return 5;
    }

    private void endBossFight(boolean victory) {
        bossFightActive = false;

        for (LivingEntity entity : waveEntities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        waveEntities.clear();

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        if (victory) {
            for (Player p : getNearbyPlayers(50)) {
                p.sendMessage("§a§l=== VICTOIRE ! LE BOSS GÉANT EST VAINCU ! ===");
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        } else {
            for (Player p : getNearbyPlayers(50)) {
                p.sendMessage("§c§lLe boss a disparu...");
            }
            playerDamage.clear();
        }

        boss = null;
        currentWave = 0;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        deathLocations.put(p.getUniqueId(), p.getLocation());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();

        // VÉRIFIER SI L'ÉVÉNEMENT EST ACTIVÉ
        if (!plugin.getConfig().getBoolean("event.enabled", false)) {
            return; // Ne rien faire si l'événement n'est pas activé
        }

        String locString = plugin.getConfig().getString("event.respawn-location", "0 100 0");
        String[] parts = locString.split(" ");
        if (parts.length == 3) {
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                World world = Bukkit.getWorlds().get(0);
                if (world != null) {
                    Location spawnLoc = new Location(world, x, y, z);
                    event.setRespawnLocation(spawnLoc);
                }
            } catch (NumberFormatException ignored) {}
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // VÉRIFICATION ENCORE AU CAS OÙ
            if (!plugin.getConfig().getBoolean("event.enabled", false)) {
                return; // Ne pas téléporter si l'événement est désactivé
            }

            Location deathLoc = deathLocations.get(p.getUniqueId());
            if (deathLoc != null) {
                p.teleport(deathLoc);
                p.sendMessage("§aTu retournes là où tu es mort !");
            }
        }, 20L * 10);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // VÉRIFIER SI L'ÉVÉNEMENT EST ACTIVÉ
        if (!plugin.getConfig().getBoolean("event.enabled", false)) {
            return; // Ne rien faire si l'événement n'est pas activé
        }

        Player player = event.getPlayer();

        if (bossFightActive && boss != null && boss.isValid() && !boss.isDead()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                teleportAroundBoss(player);
                if (bossBar != null) {
                    bossBar.addPlayer(player);
                }
                player.sendMessage("§aLe combat est déjà en cours ! Rejoins la bataille !");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            }, 5L);
            return;
        }

        String locString = plugin.getConfig().getString("event.respawn-location", "0 100 0");
        String[] parts = locString.split(" ");
        if (parts.length != 3) return;

        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            World world = Bukkit.getWorlds().get(0);

            if (world == null) return;

            Location eventSpawn = new Location(world, x, y, z);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(eventSpawn);
                player.sendMessage("§aBienvenue sur l'événement ! Prépare-toi au combat !");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            }, 5L);

        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Mauvais format pour event.respawn-location !");
        }
    }

    private void teleportAroundBoss(Player player) {
        if (boss == null || !boss.isValid() || boss.isDead()) return;
        if (player == null || !player.isOnline()) return;

        Location bossLoc = boss.getLocation();
        Random rand = new Random();

        double radius = 5 + rand.nextDouble() * 5;
        double angle = rand.nextDouble() * 2 * Math.PI;

        double x = bossLoc.getX() + radius * Math.cos(angle);
        double z = bossLoc.getZ() + radius * Math.sin(angle);
        double y = bossLoc.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;

        Location tpLoc = new Location(bossLoc.getWorld(), x, y, z);
        player.teleport(tpLoc);

        player.sendMessage("§cTu es téléporté près du Boss Géant !");
        player.playSound(tpLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }

    private List<Player> getNearbyPlayers(double radius) {
        List<Player> players = new ArrayList<>();
        if (bossLocation == null) return players;

        for (Entity entity : bossLocation.getWorld().getNearbyEntities(bossLocation, radius, radius, radius)) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }
        return players;
    }

    private Location getRandomLocationAround(Location center, double radius) {
        Random rand = new Random();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double distance = rand.nextDouble() * radius;

        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        int y = center.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;

        return new Location(center.getWorld(), x, y, z);
    }
}