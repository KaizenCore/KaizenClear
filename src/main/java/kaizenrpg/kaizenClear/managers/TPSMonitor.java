package kaizenrpg.kaizenClear.managers;

import kaizenrpg.kaizenClear.KaizenClear;
import kaizenrpg.kaizenClear.database.StatisticsManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayDeque;
import java.util.Deque;

@Getter
public class TPSMonitor {

    private final KaizenClear plugin;
    private final Deque<Double> tpsHistory;
    private final MemoryMXBean memoryBean;

    private double currentTPS = 20.0;
    private double averageTPS = 20.0;
    private long lastCheck = System.currentTimeMillis();
    private long lastCleanupTime = 0;
    private int entityCount = 0;
    private int chunkCount = 0;
    private int tickCounter = 0;

    private static final int HISTORY_SIZE = 10;
    private static final int STATS_SAVE_INTERVAL = 6000; // Save stats every 5 minutes (6000 ticks)
    private static final long CLEANUP_COOLDOWN = 120000; // 2 minutes cooldown between cleanups

    public TPSMonitor(KaizenClear plugin) {
        this.plugin = plugin;
        this.tpsHistory = new ArrayDeque<>(HISTORY_SIZE);
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public void startMonitoring() {
        ConfigManager config = plugin.getConfigManager();

        new BukkitRunnable() {
            @Override
            public void run() {
                updateTPS();
                updateEntityCount();
                updateChunkCount();

                // Check for TPS drops and trigger cleanup if needed
                if (config.isAutoCleanupEnabled()) {
                    checkTPSThresholds();
                }

                // Save statistics to database periodically
                tickCounter += config.getTpsUpdateInterval();
                if (tickCounter >= STATS_SAVE_INTERVAL) {
                    saveStatistics();
                    tickCounter = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, config.getTpsUpdateInterval());

        plugin.getLogger().info("TPS monitoring started");
    }

    private void updateTPS() {
        try {
            // Use Paper's TPS API if available
            double tps = Bukkit.getTPS()[0]; // 1-minute average
            currentTPS = Math.min(20.0, tps);

            // Add to history
            tpsHistory.addLast(currentTPS);
            if (tpsHistory.size() > HISTORY_SIZE) {
                tpsHistory.removeFirst();
            }

            // Calculate average
            averageTPS = tpsHistory.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(20.0);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get TPS: " + e.getMessage());
            currentTPS = 20.0;
        }
    }

    private void updateEntityCount() {
        entityCount = Bukkit.getWorlds().stream()
                .mapToInt(world -> world.getEntities().size())
                .sum();
    }

    private void updateChunkCount() {
        chunkCount = Bukkit.getWorlds().stream()
                .mapToInt(world -> world.getLoadedChunks().length)
                .sum();
    }

    private void checkTPSThresholds() {
        ConfigManager config = plugin.getConfigManager();
        CleanupManager cleanupManager = plugin.getCleanupManager();

        if (cleanupManager == null) return;

        // Check cooldown to prevent spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_COOLDOWN) {
            return;
        }

        if (currentTPS < config.getTpsCriticalThreshold()) {
            // Critical TPS - emergency cleanup
            plugin.getLogger().warning("TPS critically low (" + String.format("%.2f", currentTPS) + "), triggering emergency cleanup");
            cleanupManager.emergencyCleanup();
            lastCleanupTime = currentTime;
        } else if (currentTPS < config.getTpsWarningThreshold()) {
            // Warning TPS - normal cleanup
            plugin.getLogger().info("TPS below warning threshold (" + String.format("%.2f", currentTPS) + "), triggering cleanup");
            cleanupManager.cleanupAllWorlds();
            lastCleanupTime = currentTime;
        }
    }

    public String getTPSColor() {
        if (currentTPS >= 19.0) return "§a"; // Green
        if (currentTPS >= 17.0) return "§e"; // Yellow
        if (currentTPS >= 15.0) return "§6"; // Orange
        return "§c"; // Red
    }

    public MemoryUsage getMemoryUsage() {
        return memoryBean.getHeapMemoryUsage();
    }

    public double getMemoryPercent() {
        MemoryUsage memory = getMemoryUsage();
        return (double) memory.getUsed() / memory.getMax() * 100;
    }

    public String getFormattedTPS() {
        return getTPSColor() + String.format("%.2f", currentTPS);
    }

    /**
     * Get TPS as plain text without color codes (for Adventure API)
     */
    public String getPlainTPS() {
        return String.format("%.2f", currentTPS);
    }

    public String getFormattedMemory() {
        MemoryUsage memory = getMemoryUsage();
        long usedMB = memory.getUsed() / (1024 * 1024);
        long maxMB = memory.getMax() / (1024 * 1024);
        return String.format("%dMB / %dMB (%.1f%%)", usedMB, maxMB, getMemoryPercent());
    }

    /**
     * Save server statistics to database
     */
    private void saveStatistics() {
        StatisticsManager statsManager = plugin.getStatisticsManager();
        if (statsManager != null) {
            MemoryUsage memory = getMemoryUsage();
            statsManager.saveServerStats(
                    currentTPS,
                    entityCount,
                    chunkCount,
                    memory.getUsed(),
                    memory.getMax()
            );
        }
    }
}
