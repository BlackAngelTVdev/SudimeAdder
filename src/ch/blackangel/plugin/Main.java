package ch.blackangel.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class Main extends JavaPlugin {

    // Instances principales
    private MagicSticks magicSticks;
    private CraftingManager craftingManager;
    private GiantBoss giantBoss;
    private IllusionerSpawner illusionerSpawner;

    @Override
    public void onEnable() {
        logColored("Sudime Adder est opérationnel");

        initializeManagers();
        registerCommands();
        registerListeners();
        registerCrafts();
        loadConfigurations();

        getLogger().info("✅ Plugin entièrement initialisé");
    }

    @Override
    public void onDisable() {
        if (illusionerSpawner != null) {
            illusionerSpawner.stopSpawning();
        }
        logColored("Sudime Adder est stoppé");
    }

    private void initializeManagers() {
        // Initialisation des managers principaux
        this.magicSticks = new MagicSticks(this);
        this.craftingManager = new CraftingManager(this, magicSticks);
        this.giantBoss = new GiantBoss(this);
        this.illusionerSpawner = new IllusionerSpawner(this);
    }

    private void registerCommands() {
        // Commandes d'items
        getCommand("givesudime").setExecutor(new SudimeCommands());
        getCommand("givelaxarium").setExecutor(new SudimeCommands());

        // Commandes utilitaires
        getCommand("furnace").setExecutor(new FurnaceCommand());
        getCommand("wiki").setExecutor(new wiki(this));
        getCommand("discord").setExecutor(new DiscordCommand(this));
        getCommand("rtp").setExecutor(new RTPCommand());
        getCommand("carte").setExecutor(new CarteCommand(this));

        // Commandes de gestion
        ClearLagg clearLagg = new ClearLagg(this);
        getCommand("clearlagg").setExecutor(clearLagg);
        getCommand("clearinfo").setExecutor(clearLagg);

        // Commandes de boss
        getCommand("spawnboss").setExecutor(giantBoss);
    }

    private void registerListeners() {
        // ✅ Gestion des crafts
        ChestDetectorListener chestDetectorListener = new ChestDetectorListener(this);
        Bukkit.getPluginManager().registerEvents(chestDetectorListener, this);
        Bukkit.getPluginManager().registerEvents(craftingManager, this);

        // ✅ Items et gameplay
        Bukkit.getPluginManager().registerEvents(new hammer(this), this);
        Bukkit.getPluginManager().registerEvents(new Hoe(this), this);
        Bukkit.getPluginManager().registerEvents(new SudimePlaceBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MonPluginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AntiCraft(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(), this);
        Bukkit.getPluginManager().registerEvents(new CarteGUI(this), this);
        Bukkit.getPluginManager().registerEvents(new ArmorEffectManager(this), this);

        // ✅ Bâtons magiques
        Bukkit.getPluginManager().registerEvents(new MagicSticksListener(this, magicSticks), this);

        // ✅ Totems
        getLogger().info(">>> Enregistrement de TotemListener...");
        Bukkit.getPluginManager().registerEvents(new TotemListener(this), this);
        getLogger().info("✅ TotemListener enregistré !");

        // ✅ Boss
        Bukkit.getPluginManager().registerEvents(giantBoss, this);
    }

    private void registerCrafts() {
        // ✅ Items custom
        CustomItemCreator.init(this);
        ChestDetectorListener.createCraft(this);

        // ✅ Outils spéciaux
        MagnetItem magnetItem = new MagnetItem(this);
        magnetItem.registerMagnetRecipe();

        // ✅ Outils et armures Sudime
        CraftingManager.createCustomRecipes(this);

        // ✅ Bâtons magiques
        magicSticks.registerRecipes();

        // ✅ Totems
        TotemCraft.registerRecipe(this);

        getLogger().info("✅ Toutes les recettes enregistrées");
    }

    private void loadConfigurations() {
        // Charger la config carte.yml si elle n'existe pas
        File carteFile = new File(getDataFolder(), "carte.yml");
        if (!carteFile.exists()) {
            saveResource("carte.yml", false);
            getLogger().info("✅ Fichier carte.yml créé");
        }
    }

    private void logColored(String message) {
        String blue = "\u001B[34m";
        String bold = "\u001B[1m";
        String reset = "\u001B[0m";
        String frame = "+-------------------------------+";
        System.out.println(frame);
        System.out.println("| " + blue + bold + message + reset + " |");
        System.out.println(frame);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("tpworld")) return false;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUtilisation : /tpworld <nom_du_monde>");
            return true;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§7Chargement du monde §e" + worldName + "§7...");
            try {
                world = new WorldCreator(worldName).createWorld();
                if (world == null) {
                    player.sendMessage("§cImpossible de charger le monde.");
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage("§cErreur lors du chargement du monde.");
                getLogger().warning("Erreur chargement monde " + worldName + ": " + e.getMessage());
                return true;
            }
        }

        player.teleport(world.getSpawnLocation());
        player.sendMessage("§aTéléporté vers §e" + worldName + "§a !");
        return true;
    }
}