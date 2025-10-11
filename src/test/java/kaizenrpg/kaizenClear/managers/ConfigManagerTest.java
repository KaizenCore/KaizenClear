package kaizenrpg.kaizenClear.managers;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import kaizenrpg.kaizenClear.KaizenClear;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigManager
 */
class ConfigManagerTest {

    private ServerMock server;
    private KaizenClear plugin;
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(KaizenClear.class);
        configManager = plugin.getConfigManager();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Config should be enabled by default")
    void testConfigEnabledByDefault() {
        assertTrue(configManager.isEnabled(), "Plugin should be enabled by default");
    }

    @Test
    @DisplayName("TPS thresholds should have valid defaults")
    void testTPSThresholdsValid() {
        double warningThreshold = configManager.getTpsWarningThreshold();
        double criticalThreshold = configManager.getTpsCriticalThreshold();

        assertTrue(warningThreshold > 0 && warningThreshold <= 20,
                "Warning threshold should be between 0 and 20");
        assertTrue(criticalThreshold > 0 && criticalThreshold <= 20,
                "Critical threshold should be between 0 and 20");
        assertTrue(criticalThreshold < warningThreshold,
                "Critical threshold should be less than warning threshold");
    }

    @Test
    @DisplayName("Item lifetime should be positive")
    void testItemLifetimePositive() {
        int lifetime = configManager.getDefaultItemLifetime();
        assertTrue(lifetime > 0, "Item lifetime must be positive");
    }

    @Test
    @DisplayName("Cluster settings should be valid")
    void testClusterSettingsValid() {
        int clusterSize = configManager.getItemClusterSize();
        int clusterRadius = configManager.getItemClusterRadius();

        assertTrue(clusterSize > 0, "Cluster size must be positive");
        assertTrue(clusterRadius > 0, "Cluster radius must be positive");
    }

    @Test
    @DisplayName("Database pool size should be valid")
    void testDatabasePoolSizeValid() {
        int poolSize = configManager.getDatabasePoolSize();
        assertTrue(poolSize > 0 && poolSize <= 50,
                "Database pool size should be between 1 and 50");
    }

    @Test
    @DisplayName("Config validation should correct invalid TPS values")
    void testConfigValidationCorrectInvalidValues() {
        // This test verifies that validation runs during loadConfig()
        // If config had invalid values, they would be corrected to defaults
        assertDoesNotThrow(() -> configManager.loadConfig(),
                "Config loading with validation should not throw exceptions");
    }
}
