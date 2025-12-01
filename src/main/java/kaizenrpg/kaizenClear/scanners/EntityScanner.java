package kaizenrpg.kaizenClear.scanners;

import kaizenrpg.kaizenClear.KaizenClear;
import kaizenrpg.kaizenClear.managers.ConfigManager;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class EntityScanner {

    private final KaizenClear plugin;
    private final ConfigManager config;

    public EntityScanner(KaizenClear plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /**
     * Scan and collect all items in a world that should be removed (respects age check)
     */
    public List<Item> scanItems(World world) {
        return scanItems(world, true);
    }

    /**
     * Scan and collect all items in a world
     * @param checkAge if true, only returns items older than configured lifetime
     */
    public List<Item> scanItems(World world, boolean checkAge) {
        List<Item> itemsToRemove = new ArrayList<>();

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item item) {
                if (checkAge) {
                    if (shouldRemoveItem(item)) {
                        itemsToRemove.add(item);
                    }
                } else {
                    // Force mode - only check whitelist, not age
                    if (shouldForceRemoveItem(item)) {
                        itemsToRemove.add(item);
                    }
                }
            }
        }

        return itemsToRemove;
    }

    /**
     * Check if item should be force-removed (ignores age, respects whitelist only)
     */
    private boolean shouldForceRemoveItem(Item item) {
        ItemStack stack = item.getItemStack();
        Material type = stack.getType();

        // Check whitelist - never remove whitelisted items
        if (config.getItemWhitelist().contains(type.name())) {
            return false;
        }

        // Check if player has bypass permission
        if (item.getOwner() != null) {
            Player owner = plugin.getServer().getPlayer(item.getOwner());
            if (owner != null && owner.hasPermission("kaizenclear.bypass")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Scan for item clusters (many items in a small area)
     */
    public List<Item> scanItemClusters(World world) {
        List<Item> allItems = world.getEntitiesByClass(Item.class).stream().toList();
        List<Item> clusteredItems = new ArrayList<>();
        Set<Item> processed = new HashSet<>();

        int clusterSize = config.getItemClusterSize();
        int clusterRadius = config.getItemClusterRadius();

        for (Item item : allItems) {
            if (processed.contains(item)) continue;

            // Find nearby items
            List<Item> nearbyItems = allItems.stream()
                    .filter(other -> !processed.contains(other))
                    .filter(other -> other.getLocation().distance(item.getLocation()) <= clusterRadius)
                    .collect(Collectors.toList());

            // If cluster is large enough, mark for removal
            if (nearbyItems.size() >= clusterSize) {
                clusteredItems.addAll(nearbyItems);
                processed.addAll(nearbyItems);
            }
        }

        return clusteredItems;
    }

    /**
     * Scan entities in a specific chunk
     */
    public Map<EntityType, Integer> scanChunk(Chunk chunk) {
        Map<EntityType, Integer> entityCounts = new HashMap<>();

        for (Entity entity : chunk.getEntities()) {
            EntityType type = entity.getType();
            entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
        }

        return entityCounts;
    }

    /**
     * Find chunks with excessive entities
     */
    public List<Chunk> findLaggyChunks(World world) {
        List<Chunk> laggyChunks = new ArrayList<>();
        int maxItemsPerChunk = config.getMaxItemsPerChunk();

        for (Chunk chunk : world.getLoadedChunks()) {
            long itemCount = Arrays.stream(chunk.getEntities())
                    .filter(e -> e instanceof Item)
                    .count();

            if (itemCount > maxItemsPerChunk) {
                laggyChunks.add(chunk);
            }
        }

        return laggyChunks;
    }

    /**
     * Get total entity statistics for a world
     */
    public EntityStatistics getWorldStatistics(World world) {
        EntityStatistics stats = new EntityStatistics();

        for (Entity entity : world.getEntities()) {
            stats.totalEntities++;

            if (entity instanceof Item) {
                stats.items++;
            } else if (entity instanceof Monster) {
                stats.monsters++;
            } else if (entity instanceof Animals) {
                stats.animals++;
            } else if (entity instanceof Projectile) {
                stats.projectiles++;
            } else if (entity instanceof Vehicle) {
                stats.vehicles++;
            }
        }

        stats.chunks = world.getLoadedChunks().length;
        return stats;
    }

    /**
     * Check if an item should be removed
     */
    private boolean shouldRemoveItem(Item item) {
        ItemStack stack = item.getItemStack();
        Material type = stack.getType();

        // Check whitelist
        if (config.getItemWhitelist().contains(type.name())) {
            return false;
        }

        // Check if player has bypass permission
        if (item.getOwner() != null) {
            Player owner = plugin.getServer().getPlayer(item.getOwner());
            if (owner != null && owner.hasPermission("kaizenclear.bypass")) {
                return false;
            }
        }

        // Check blacklist (always remove)
        if (config.getItemBlacklist().contains(type.name())) {
            return true;
        }

        // Check item age
        int ticksLived = item.getTicksLived();
        int maxTicks = config.getDefaultItemLifetime() * 20; // Convert seconds to ticks

        return ticksLived >= maxTicks;
    }

    @Getter
    public static class EntityStatistics {
        private int totalEntities = 0;
        private int items = 0;
        private int monsters = 0;
        private int animals = 0;
        private int projectiles = 0;
        private int vehicles = 0;
        private int chunks = 0;

        public String getFormattedStats() {
            return String.format(
                    "Total: %d | Items: %d | Monsters: %d | Animals: %d | Chunks: %d",
                    totalEntities, items, monsters, animals, chunks
            );
        }
    }
}
