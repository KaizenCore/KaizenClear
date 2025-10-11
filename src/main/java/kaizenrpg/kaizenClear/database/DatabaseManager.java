package kaizenrpg.kaizenClear.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kaizenrpg.kaizenClear.KaizenClear;
import kaizenrpg.kaizenClear.managers.ConfigManager;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@Getter
public class DatabaseManager {

    private final KaizenClear plugin;
    private final ConfigManager config;
    private final DatabaseType type;

    private HikariDataSource hikariDataSource;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    private boolean connected = false;

    public DatabaseManager(KaizenClear plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.type = DatabaseType.fromString(config.getDatabaseType());
    }

    /**
     * Initialize database connection
     */
    public boolean connect() {
        if (type == DatabaseType.NONE) {
            plugin.getLogger().info("Database disabled in configuration");
            return false;
        }

        try {
            switch (type) {
                case MYSQL -> connectMySQL();
                case MONGODB -> connectMongoDB();
                default -> {
                    return false;
                }
            }

            connected = true;
            plugin.getLogger().info("Successfully connected to " + type.name() + " database");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Initialize MySQL connection pool
     */
    private void connectMySQL() {
        HikariConfig hikariConfig = new HikariConfig();

        // Connection settings
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.getDatabaseHost(),
                config.getDatabasePort(),
                config.getDatabaseName());

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getDatabaseUsername());
        hikariConfig.setPassword(config.getDatabasePassword());

        // Pool settings
        hikariConfig.setMaximumPoolSize(config.getDatabasePoolSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
        hikariConfig.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        hikariConfig.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));

        // Performance settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        // Connection test
        hikariConfig.setConnectionTestQuery("SELECT 1");

        hikariDataSource = new HikariDataSource(hikariConfig);

        // Test connection
        try (Connection conn = hikariDataSource.getConnection()) {
            plugin.getLogger().info("MySQL connection pool initialized");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to test MySQL connection", e);
        }
    }

    /**
     * Initialize MongoDB connection
     */
    private void connectMongoDB() {
        String connectionString = String.format("mongodb://%s:%s@%s:%d/%s",
                config.getDatabaseUsername(),
                config.getDatabasePassword(),
                config.getDatabaseHost(),
                config.getDatabasePort(),
                config.getDatabaseName());

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToConnectionPoolSettings(builder ->
                        builder.maxSize(config.getDatabasePoolSize())
                                .minSize(2)
                                .maxConnectionIdleTime(10, TimeUnit.MINUTES)
                                .maxConnectionLifeTime(30, TimeUnit.MINUTES))
                .build();

        mongoClient = MongoClients.create(settings);
        mongoDatabase = mongoClient.getDatabase(config.getDatabaseName());

        // Test connection
        mongoDatabase.listCollectionNames().first();
        plugin.getLogger().info("MongoDB connection initialized");
    }

    /**
     * Create database schema/tables
     */
    public void createSchema() {
        if (!connected) {
            plugin.getLogger().warning("Cannot create schema: not connected to database");
            return;
        }

        try {
            switch (type) {
                case MYSQL -> createMySQLSchema();
                case MONGODB -> createMongoDBSchema();
            }
            plugin.getLogger().info("Database schema created successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create MySQL tables
     */
    private void createMySQLSchema() throws SQLException {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();

            // Cleanup statistics table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS cleanup_stats (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        server_name VARCHAR(100) NOT NULL,
                        world_name VARCHAR(100) NOT NULL,
                        cleanup_type VARCHAR(50) NOT NULL,
                        entities_removed INT NOT NULL,
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_server (server_name),
                        INDEX idx_timestamp (timestamp)
                    )
                    """);

            // Server statistics table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS server_stats (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        server_name VARCHAR(100) NOT NULL,
                        tps DOUBLE NOT NULL,
                        entity_count INT NOT NULL,
                        chunk_count INT NOT NULL,
                        memory_used BIGINT NOT NULL,
                        memory_max BIGINT NOT NULL,
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_server (server_name),
                        INDEX idx_timestamp (timestamp)
                    )
                    """);

            // Configuration table (for multi-server sync)
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS plugin_config (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        config_key VARCHAR(255) NOT NULL UNIQUE,
                        config_value TEXT NOT NULL,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);

            plugin.getLogger().info("MySQL tables created successfully");
        }
    }

    /**
     * Create MongoDB collections and indexes
     */
    private void createMongoDBSchema() {
        // Collections are created automatically in MongoDB
        // Just create indexes for performance

        mongoDatabase.getCollection("cleanup_stats")
                .createIndex(new org.bson.Document("server_name", 1).append("timestamp", -1));

        mongoDatabase.getCollection("server_stats")
                .createIndex(new org.bson.Document("server_name", 1).append("timestamp", -1));

        mongoDatabase.getCollection("plugin_config")
                .createIndex(new org.bson.Document("config_key", 1));

        plugin.getLogger().info("MongoDB indexes created successfully");
    }

    /**
     * Get a MySQL connection from the pool
     */
    public Connection getConnection() throws SQLException {
        if (type != DatabaseType.MYSQL || hikariDataSource == null) {
            throw new SQLException("MySQL is not configured");
        }
        return hikariDataSource.getConnection();
    }

    /**
     * Get MongoDB database instance
     */
    public MongoDatabase getMongoDatabase() {
        if (type != DatabaseType.MONGODB || mongoDatabase == null) {
            throw new IllegalStateException("MongoDB is not configured");
        }
        return mongoDatabase;
    }

    /**
     * Execute a MySQL update asynchronously
     */
    public void executeUpdateAsync(String sql, Object... params) {
        if (type != DatabaseType.MYSQL) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to execute async update: " + e.getMessage());
            }
        });
    }

    /**
     * Close all database connections
     */
    public void disconnect() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
            plugin.getLogger().info("MySQL connection pool closed");
        }

        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("MongoDB connection closed");
        }

        connected = false;
    }

    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Database type enum
     */
    public enum DatabaseType {
        NONE,
        MYSQL,
        MONGODB;

        public static DatabaseType fromString(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
    }
}
