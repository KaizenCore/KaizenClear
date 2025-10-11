package kaizenrpg.kaizenClear.managers;

import kaizenrpg.kaizenClear.KaizenClear;
import kaizenrpg.kaizenClear.database.StatisticsManager;
import kaizenrpg.kaizenClear.scanners.EntityScanner;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;

@Getter
public class CleanupManager {

    private final KaizenClear plugin;
    private final ConfigManager config;
    private final EntityScanner scanner;

    private int lastCleanupCount = 0;
    private long lastCleanupTime = 0;
    private int totalCleaned = 0;

    public CleanupManager(KaizenClear plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.scanner = new EntityScanner(plugin);
    }

    /**
     * Start automatic cleanup scheduler
     */
    public void startScheduler() {
        try {
            var schedulesSection = config.getConfig().getConfigurationSection("cleanup.schedules");

            if (schedulesSection == null || schedulesSection.getKeys(false).isEmpty()) {
                plugin.getLogger().warning("No cleanup schedules configured, using default");
                startDefaultScheduler();
                return;
            }

            // Start a scheduler for each schedule entry
            int scheduleCount = 0;
            for (String scheduleKey : schedulesSection.getKeys(false)) {
                String basePath = "cleanup.schedules." + scheduleKey;
                long interval = config.getConfig().getLong(basePath + ".interval", 600L);
                List<String> worlds = config.getConfig().getStringList(basePath + ".worlds");
                boolean broadcast = config.getConfig().getBoolean(basePath + ".broadcast", true);
                List<String> types = config.getConfig().getStringList(basePath + ".types");

                // Convert to ticks (20 ticks = 1 second)
                long intervalTicks = interval * 20;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (config.isEnabled() && config.isItemsEnabled()) {
                            performScheduledCleanup(worlds, broadcast, types);
                        }
                    }
                }.runTaskTimer(plugin, intervalTicks, intervalTicks);

                scheduleCount++;
                plugin.getLogger().info("Cleanup schedule #" + scheduleCount + " started (interval: " + interval + "s, worlds: " + worlds + ")");
            }

            plugin.getLogger().info("Total cleanup schedulers started: " + scheduleCount);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load cleanup schedules from config: " + e.getMessage());
            plugin.getLogger().warning("Starting default scheduler instead");
            startDefaultScheduler();
        }
    }

    /**
     * Start a default cleanup scheduler (fallback)
     */
    private void startDefaultScheduler() {
        long interval = 600L; // 10 minutes
        long intervalTicks = interval * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (config.isEnabled() && config.isItemsEnabled()) {
                    performScheduledCleanup();
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        plugin.getLogger().info("Default cleanup scheduler started (interval: " + interval + " seconds)");
    }

    /**
     * Perform scheduled cleanup (default - all worlds)
     */
    private void performScheduledCleanup() {
        performScheduledCleanup(null, config.isBroadcastWarnings(), List.of("items"));
    }

    /**
     * Perform scheduled cleanup with custom parameters
     */
    private void performScheduledCleanup(List<String> worldNames, boolean broadcast, List<String> types) {
        // Warn players before cleanup
        if (broadcast) {
            int warningTime = config.getWarningBeforeClear();
            broadcastMessage("§e[KaizenClear] Cleanup starting in " + warningTime + " seconds!");

            // Schedule actual cleanup after warning
            new BukkitRunnable() {
                @Override
                public void run() {
                    executeScheduledCleanup(worldNames, types);
                }
            }.runTaskLater(plugin, warningTime * 20L);
        } else {
            executeScheduledCleanup(worldNames, types);
        }
    }

    /**
     * Execute the actual cleanup based on configuration
     */
    private void executeScheduledCleanup(List<String> worldNames, List<String> types) {
        int totalRemoved = 0;

        // Determine which worlds to clean
        List<World> targetWorlds;
        if (worldNames == null || worldNames.isEmpty()) {
            targetWorlds = Bukkit.getWorlds();
        } else {
            targetWorlds = worldNames.stream()
                    .map(Bukkit::getWorld)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        // Clean each world based on types
        for (World world : targetWorlds) {
            if (!isWorldEnabled(world)) continue;

            for (String type : types) {
                switch (type.toLowerCase()) {
                    case "items" -> totalRemoved += cleanupItems(world);
                    case "clusters" -> totalRemoved += cleanupItemClusters(world);
                    case "monsters" -> totalRemoved += cleanupEntityType(world, Monster.class);
                }
            }
        }

        if (totalRemoved > 0) {
            broadcastMessage("§a[KaizenClear] Cleared " + totalRemoved + " entities!");
            plugin.getLogger().info("Scheduled cleanup complete: removed " + totalRemoved + " entities");
        }
    }

    /**
     * Clean items in all configured worlds
     */
    public void cleanupAllWorlds() {
        int totalRemoved = 0;

        for (World world : Bukkit.getWorlds()) {
            if (isWorldEnabled(world)) {
                totalRemoved += cleanupItems(world);
            }
        }

        lastCleanupCount = totalRemoved;
        lastCleanupTime = System.currentTimeMillis();
        totalCleaned += totalRemoved;

        if (config.isBroadcastWarnings() && totalRemoved > 0) {
            broadcastMessage("§a[KaizenClear] Cleared " + totalRemoved + " items!");
        }

        plugin.getLogger().info("Cleanup complete: removed " + totalRemoved + " items");
    }

    /**
     * Clean items in a specific world
     */
    public int cleanupItems(World world) {
        List<Item> items = scanner.scanItems(world);
        int removed = 0;

        for (Item item : items) {
            item.remove();
            removed++;
        }

        // Save statistics to database
        StatisticsManager statsManager = plugin.getStatisticsManager();
        if (statsManager != null && removed > 0) {
            statsManager.saveCleanupStats(world, "items", removed);
        }

        return removed;
    }

    /**
     * Clean item clusters in a world
     */
    public int cleanupItemClusters(World world) {
        List<Item> clusteredItems = scanner.scanItemClusters(world);
        int removed = 0;

        for (Item item : clusteredItems) {
            item.remove();
            removed++;
        }

        // Save statistics to database
        StatisticsManager statsManager = plugin.getStatisticsManager();
        if (statsManager != null && removed > 0) {
            statsManager.saveCleanupStats(world, "clusters", removed);
        }

        plugin.getLogger().info("Removed " + removed + " clustered items in " + world.getName());
        return removed;
    }

    /**
     * Clean all entities of a specific type
     */
    public int cleanupEntityType(World world, Class<? extends Entity> entityClass) {
        Collection<? extends Entity> entities = world.getEntitiesByClass(entityClass);
        int removed = 0;

        for (Entity entity : entities) {
            // Don't remove players
            if (entity instanceof Player) continue;

            // Check if entity has bypass permission (for owned entities)
            if (entity instanceof Monster) {
                entity.remove();
                removed++;
            }
        }

        // Save statistics to database
        StatisticsManager statsManager = plugin.getStatisticsManager();
        if (statsManager != null && removed > 0) {
            String type = entityClass.getSimpleName().toLowerCase();
            statsManager.saveCleanupStats(world, type, removed);
        }

        return removed;
    }

    /**
     * Emergency cleanup when TPS is critical
     */
    public void emergencyCleanup() {
        plugin.getLogger().warning("Performing emergency cleanup!");
        broadcastMessage("§c[KaizenClear] Emergency cleanup in progress!");

        int totalRemoved = 0;

        for (World world : Bukkit.getWorlds()) {
            // Remove all items
            totalRemoved += cleanupItems(world);

            // Remove excess monsters
            totalRemoved += cleanupEntityType(world, Monster.class);
        }

        broadcastMessage("§a[KaizenClear] Emergency cleanup complete! Removed " + totalRemoved + " entities.");
        plugin.getLogger().info("Emergency cleanup removed " + totalRemoved + " entities");
    }

    /**
     * Check if a world has cleanup enabled
     */
    private boolean isWorldEnabled(World world) {
        String path = "items.world-settings." + world.getName() + ".enabled";
        return config.getConfig().getBoolean(path, true);
    }

    /**
     * Broadcast message to all players with notify permission
     */
    private void broadcastMessage(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("kaizenclear.notify") || player.hasPermission("kaizenclear.admin")) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Get statistics about last cleanup
     */
    public String getCleanupStats() {
        long timeSince = (System.currentTimeMillis() - lastCleanupTime) / 1000;
        return String.format(
                "Last cleanup: %d items | %d seconds ago | Total: %d",
                lastCleanupCount, timeSince, totalCleaned
        );
    }
}
