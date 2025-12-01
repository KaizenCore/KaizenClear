package kaizenrpg.kaizenClear.managers;

import kaizenrpg.kaizenClear.KaizenClear;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;

@Getter
public class ConfigManager {

    private final KaizenClear plugin;
    private FileConfiguration config;
    private File configFile;

    // General settings
    private boolean enabled;
    private String language;
    private boolean debugMode;

    // TPS settings
    private boolean tpsMonitoring;
    private int tpsUpdateInterval;
    private double tpsWarningThreshold;
    private double tpsCriticalThreshold;
    private boolean autoCleanupEnabled;

    // Item settings
    private boolean itemsEnabled;
    private int defaultItemLifetime;
    private int warningBeforeClear;
    private boolean broadcastWarnings;
    private List<String> itemWhitelist;
    private List<String> itemBlacklist;

    // Entity settings
    private boolean entitiesEnabled;
    private int maxItemsPerChunk;
    private int itemClusterSize;
    private int itemClusterRadius;

    // Database settings
    private String databaseType;
    private String databaseHost;
    private int databasePort;
    private String databaseName;
    private String databaseUsername;
    private String databasePassword;
    private int databasePoolSize;

    // GUI settings
    private boolean guiEnabled;
    private boolean guiAutoRefresh;
    private int guiRefreshInterval;

    public ConfigManager(KaizenClear plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        // Use Kaizen data folder: plugins/kaizen/kaizenclear/
        File dataFolder = plugin.getKaizenDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        configFile = new File(dataFolder, "config.yml");

        // Create config file if it doesn't exist
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        // Load config from custom location
        config = YamlConfiguration.loadConfiguration(configFile);

        // Set defaults from resource
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            config.setDefaults(defaultConfig);
        }

        // Load general settings
        enabled = config.getBoolean("general.enabled", true);
        language = config.getString("general.language", "en_US");
        debugMode = config.getBoolean("general.debug", false);

        // Load TPS settings
        tpsMonitoring = config.getBoolean("tps.monitoring", true);
        tpsUpdateInterval = config.getInt("tps.update-interval", 20);
        tpsWarningThreshold = config.getDouble("tps.warning-threshold", 18.0);
        tpsCriticalThreshold = config.getDouble("tps.critical-threshold", 15.0);
        autoCleanupEnabled = config.getBoolean("tps.auto-cleanup-enabled", true);

        // Load item settings
        itemsEnabled = config.getBoolean("items.enabled", true);
        defaultItemLifetime = config.getInt("items.default-lifetime", 300);
        warningBeforeClear = config.getInt("items.warning-before-clear", 30);
        broadcastWarnings = config.getBoolean("items.broadcast-warnings", true);
        itemWhitelist = config.getStringList("items.whitelist");
        itemBlacklist = config.getStringList("items.blacklist");

        // Load entity settings
        entitiesEnabled = config.getBoolean("entities.enabled", true);
        maxItemsPerChunk = config.getInt("entities.types.items.max-per-chunk", 100);
        itemClusterSize = config.getInt("entities.types.items.cluster-size", 20);
        itemClusterRadius = config.getInt("entities.types.items.cluster-radius", 5);

        // Load database settings
        databaseType = config.getString("database.type", "none");
        databaseHost = config.getString("database.host", "localhost");
        databasePort = config.getInt("database.port", 3306);
        databaseName = config.getString("database.database", "kaizenclear");
        databaseUsername = config.getString("database.username", "root");
        databasePassword = config.getString("database.password", "password");
        databasePoolSize = config.getInt("database.pool-size", 10);

        // Load GUI settings
        guiEnabled = config.getBoolean("gui.enabled", true);
        guiAutoRefresh = config.getBoolean("gui.auto-refresh", true);
        guiRefreshInterval = config.getInt("gui.refresh-interval", 20);

        // Validate configuration values
        validateConfig();

        if (debugMode) {
            plugin.getLogger().info("Configuration loaded successfully");
        }
    }

    /**
     * Validate configuration values and fix/warn about invalid settings
     */
    private void validateConfig() {
        boolean hasIssues = false;

        // Validate TPS thresholds
        if (tpsWarningThreshold < 0 || tpsWarningThreshold > 20) {
            plugin.getLogger().warning("Invalid TPS warning threshold (" + tpsWarningThreshold + "). Must be between 0-20. Using default: 18.0");
            tpsWarningThreshold = 18.0;
            hasIssues = true;
        }

        if (tpsCriticalThreshold < 0 || tpsCriticalThreshold > 20) {
            plugin.getLogger().warning("Invalid TPS critical threshold (" + tpsCriticalThreshold + "). Must be between 0-20. Using default: 15.0");
            tpsCriticalThreshold = 15.0;
            hasIssues = true;
        }

        if (tpsCriticalThreshold >= tpsWarningThreshold) {
            plugin.getLogger().warning("TPS critical threshold must be lower than warning threshold. Adjusting...");
            tpsCriticalThreshold = tpsWarningThreshold - 2.0;
            hasIssues = true;
        }

        // Validate item lifetime
        if (defaultItemLifetime <= 0) {
            plugin.getLogger().warning("Invalid item lifetime (" + defaultItemLifetime + "). Must be positive. Using default: 300");
            defaultItemLifetime = 300;
            hasIssues = true;
        }

        if (defaultItemLifetime < 60) {
            plugin.getLogger().warning("Item lifetime is very short (" + defaultItemLifetime + "s). Recommended: 180-600 seconds");
        }

        // Validate warning time
        if (warningBeforeClear < 0) {
            plugin.getLogger().warning("Invalid warning time (" + warningBeforeClear + "). Must be non-negative. Using default: 30");
            warningBeforeClear = 30;
            hasIssues = true;
        }

        if (warningBeforeClear > defaultItemLifetime) {
            plugin.getLogger().warning("Warning time (" + warningBeforeClear + "s) is longer than item lifetime (" + defaultItemLifetime + "s). Adjusting...");
            warningBeforeClear = Math.max(30, defaultItemLifetime / 10);
            hasIssues = true;
        }

        // Validate entity limits
        if (maxItemsPerChunk <= 0) {
            plugin.getLogger().warning("Invalid max items per chunk (" + maxItemsPerChunk + "). Must be positive. Using default: 100");
            maxItemsPerChunk = 100;
            hasIssues = true;
        }

        if (itemClusterSize <= 0) {
            plugin.getLogger().warning("Invalid cluster size (" + itemClusterSize + "). Must be positive. Using default: 20");
            itemClusterSize = 20;
            hasIssues = true;
        }

        if (itemClusterRadius <= 0) {
            plugin.getLogger().warning("Invalid cluster radius (" + itemClusterRadius + "). Must be positive. Using default: 5");
            itemClusterRadius = 5;
            hasIssues = true;
        }

        // Validate database pool size
        if (databasePoolSize <= 0) {
            plugin.getLogger().warning("Invalid database pool size (" + databasePoolSize + "). Must be positive. Using default: 10");
            databasePoolSize = 10;
            hasIssues = true;
        }

        if (databasePoolSize > 50) {
            plugin.getLogger().warning("Database pool size is very large (" + databasePoolSize + "). Recommended: 5-20");
        }

        // Validate update intervals
        if (tpsUpdateInterval <= 0) {
            plugin.getLogger().warning("Invalid TPS update interval (" + tpsUpdateInterval + "). Must be positive. Using default: 20");
            tpsUpdateInterval = 20;
            hasIssues = true;
        }

        if (guiRefreshInterval <= 0) {
            plugin.getLogger().warning("Invalid GUI refresh interval (" + guiRefreshInterval + "). Must be positive. Using default: 20");
            guiRefreshInterval = 20;
            hasIssues = true;
        }

        if (hasIssues) {
            plugin.getLogger().warning("Configuration validation found issues. Please review your config.yml");
            plugin.getLogger().warning("The plugin will continue with corrected values.");
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Save the default config from resources to the Kaizen data folder
     */
    private void saveDefaultConfig() {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save default config: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        loadConfig();
    }
}
