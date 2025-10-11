package kaizenrpg.kaizenClear;

import kaizenrpg.kaizenClear.commands.KaizenClearCommand;
import kaizenrpg.kaizenClear.database.DatabaseManager;
import kaizenrpg.kaizenClear.database.StatisticsManager;
import kaizenrpg.kaizenClear.gui.GUIManager;
import kaizenrpg.kaizenClear.managers.CleanupManager;
import kaizenrpg.kaizenClear.managers.ConfigManager;
import kaizenrpg.kaizenClear.managers.TPSMonitor;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class KaizenClear extends JavaPlugin {

    // Managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private StatisticsManager statisticsManager;
    private TPSMonitor tpsMonitor;
    private CleanupManager cleanupManager;
    private GUIManager guiManager;

    private static KaizenClear instance;

    @Override
    public void onEnable() {
        instance = this;

        // Print startup banner
        getLogger().info("╔══════════════════════════════════════╗");
        getLogger().info("║      KaizenClear - Starting...       ║");
        getLogger().info("║   Lag Remover & Entity Manager       ║");
        getLogger().info("╚══════════════════════════════════════╝");

        // Initialize configuration
        getLogger().info("Loading configuration...");
        configManager = new ConfigManager(this);

        // Check if plugin is enabled
        if (!configManager.isEnabled()) {
            getLogger().warning("Plugin is disabled in config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize TPS monitor
        if (configManager.isTpsMonitoring()) {
            getLogger().info("Starting TPS monitor...");
            tpsMonitor = new TPSMonitor(this);
            tpsMonitor.startMonitoring();
        }

        // Initialize cleanup manager
        getLogger().info("Initializing cleanup manager...");
        cleanupManager = new CleanupManager(this);
        if (configManager.isItemsEnabled()) {
            cleanupManager.startScheduler();
        }

        // Initialize GUI manager
        if (configManager.isGuiEnabled()) {
            getLogger().info("Initializing GUI manager...");
            guiManager = new GUIManager(this);
        }

        // Register commands
        getLogger().info("Registering commands...");
        KaizenClearCommand commandExecutor = new KaizenClearCommand(this);
        getCommand("kaizenclear").setExecutor(commandExecutor);
        getCommand("kaizenclear").setTabCompleter(commandExecutor);

        // Initialize database manager if configured
        if (!configManager.getDatabaseType().equals("none")) {
            getLogger().info("Connecting to database...");
            databaseManager = new DatabaseManager(this);
            if (databaseManager.connect()) {
                databaseManager.createSchema();
                statisticsManager = new StatisticsManager(this);
                getLogger().info("Statistics manager initialized");
            } else {
                getLogger().warning("Failed to connect to database, continuing without database support");
            }
        }

        getLogger().info("╔══════════════════════════════════════╗");
        getLogger().info("║    KaizenClear Enabled Successfully  ║");
        getLogger().info("║          Version: " + getDescription().getVersion() + "               ║");
        getLogger().info("╚══════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down KaizenClear...");

        // Cancel GUI refresh tasks
        if (guiManager != null) {
            guiManager.cancelAllRefreshTasks();
        }

        // Cancel any running tasks
        getServer().getScheduler().cancelTasks(this);

        // Close database connections
        if (databaseManager != null && databaseManager.isConnected()) {
            getLogger().info("Closing database connections...");
            databaseManager.disconnect();
        }

        getLogger().info("KaizenClear disabled successfully");
    }

    public static KaizenClear getInstance() {
        return instance;
    }
}
