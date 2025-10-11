package kaizenrpg.kaizenClear.database;

import com.mongodb.client.MongoCollection;
import kaizenrpg.kaizenClear.KaizenClear;
import org.bson.Document;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Handles statistics persistence to database
 */
public class StatisticsManager {

    private final KaizenClear plugin;
    private final DatabaseManager databaseManager;
    private final String serverName;

    public StatisticsManager(KaizenClear plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.serverName = plugin.getServer().getName();
    }

    /**
     * Save cleanup statistics to database
     */
    public void saveCleanupStats(World world, String cleanupType, int entitiesRemoved) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                switch (databaseManager.getType()) {
                    case MYSQL -> saveCleanupStatsMySQL(world.getName(), cleanupType, entitiesRemoved);
                    case MONGODB -> saveCleanupStatsMongoDB(world.getName(), cleanupType, entitiesRemoved);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save cleanup stats: " + e.getMessage());
            }
        });
    }

    /**
     * Save server performance statistics
     */
    public void saveServerStats(double tps, int entityCount, int chunkCount, long memoryUsed, long memoryMax) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                switch (databaseManager.getType()) {
                    case MYSQL -> saveServerStatsMySQL(tps, entityCount, chunkCount, memoryUsed, memoryMax);
                    case MONGODB -> saveServerStatsMongoDB(tps, entityCount, chunkCount, memoryUsed, memoryMax);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save server stats: " + e.getMessage());
            }
        });
    }

    /**
     * Save cleanup stats to MySQL
     */
    private void saveCleanupStatsMySQL(String worldName, String cleanupType, int entitiesRemoved) throws SQLException {
        String sql = "INSERT INTO cleanup_stats (server_name, world_name, cleanup_type, entities_removed, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, serverName);
            stmt.setString(2, worldName);
            stmt.setString(3, cleanupType);
            stmt.setInt(4, entitiesRemoved);
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

            stmt.executeUpdate();
        }
    }

    /**
     * Save cleanup stats to MongoDB
     */
    private void saveCleanupStatsMongoDB(String worldName, String cleanupType, int entitiesRemoved) {
        MongoCollection<Document> collection = databaseManager.getMongoDatabase().getCollection("cleanup_stats");

        Document doc = new Document()
                .append("server_name", serverName)
                .append("world_name", worldName)
                .append("cleanup_type", cleanupType)
                .append("entities_removed", entitiesRemoved)
                .append("timestamp", new java.util.Date());

        collection.insertOne(doc);
    }

    /**
     * Save server stats to MySQL
     */
    private void saveServerStatsMySQL(double tps, int entityCount, int chunkCount, long memoryUsed, long memoryMax) throws SQLException {
        String sql = "INSERT INTO server_stats (server_name, tps, entity_count, chunk_count, memory_used, memory_max, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, serverName);
            stmt.setDouble(2, tps);
            stmt.setInt(3, entityCount);
            stmt.setInt(4, chunkCount);
            stmt.setLong(5, memoryUsed);
            stmt.setLong(6, memoryMax);
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));

            stmt.executeUpdate();
        }
    }

    /**
     * Save server stats to MongoDB
     */
    private void saveServerStatsMongoDB(double tps, int entityCount, int chunkCount, long memoryUsed, long memoryMax) {
        MongoCollection<Document> collection = databaseManager.getMongoDatabase().getCollection("server_stats");

        Document doc = new Document()
                .append("server_name", serverName)
                .append("tps", tps)
                .append("entity_count", entityCount)
                .append("chunk_count", chunkCount)
                .append("memory_used", memoryUsed)
                .append("memory_max", memoryMax)
                .append("timestamp", new java.util.Date());

        collection.insertOne(doc);
    }

    /**
     * Save or update configuration value
     */
    public void saveConfig(String key, String value) {
        if (databaseManager == null || !databaseManager.isConnected()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                switch (databaseManager.getType()) {
                    case MYSQL -> saveConfigMySQL(key, value);
                    case MONGODB -> saveConfigMongoDB(key, value);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save config: " + e.getMessage());
            }
        });
    }

    /**
     * Save config to MySQL (upsert)
     */
    private void saveConfigMySQL(String key, String value) throws SQLException {
        String sql = "INSERT INTO plugin_config (config_key, config_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE config_value = ?, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, value);

            stmt.executeUpdate();
        }
    }

    /**
     * Save config to MongoDB (upsert)
     */
    private void saveConfigMongoDB(String key, String value) {
        MongoCollection<Document> collection = databaseManager.getMongoDatabase().getCollection("plugin_config");

        Document filter = new Document("config_key", key);
        Document update = new Document("$set", new Document()
                .append("config_key", key)
                .append("config_value", value)
                .append("updated_at", new java.util.Date()));

        collection.updateOne(filter, update, new com.mongodb.client.model.UpdateOptions().upsert(true));
    }
}
