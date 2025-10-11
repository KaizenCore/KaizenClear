package kaizenrpg.kaizenClear.gui;

import kaizenrpg.kaizenClear.KaizenClear;
import kaizenrpg.kaizenClear.managers.CleanupManager;
import kaizenrpg.kaizenClear.managers.TPSMonitor;
import kaizenrpg.kaizenClear.scanners.EntityScanner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {

    private final KaizenClear plugin;
    private static final Component GUI_TITLE = Component.text("KaizenClear Dashboard", NamedTextColor.GOLD, TextDecoration.BOLD);

    // Track active refresh tasks to prevent leaks
    private final Map<UUID, Integer> activeRefreshTasks = new HashMap<>();

    public GUIManager(KaizenClear plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainGUI(Player player) {
        // Cancel any existing refresh task for this player
        cancelRefreshTask(player);

        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Populate GUI items
        populateGUI(gui);

        // Open GUI
        player.openInventory(gui);

        // Auto-refresh if enabled
        if (plugin.getConfigManager().isGuiAutoRefresh()) {
            startAutoRefresh(player, gui);
        }
    }

    private void populateGUI(Inventory gui) {
        TPSMonitor tpsMonitor = plugin.getTpsMonitor();
        CleanupManager cleanupManager = plugin.getCleanupManager();

        // TPS Information (slot 10)
        if (tpsMonitor != null) {
            gui.setItem(10, createTPSItem(tpsMonitor));
        }

        // Memory Information (slot 11)
        if (tpsMonitor != null) {
            gui.setItem(11, createMemoryItem(tpsMonitor));
        }

        // Entity Statistics (slot 12)
        if (tpsMonitor != null) {
            gui.setItem(12, createEntityItem(tpsMonitor));
        }

        // Cleanup Statistics (slot 13)
        if (cleanupManager != null) {
            gui.setItem(13, createCleanupItem(cleanupManager));
        }

        // Quick Actions
        gui.setItem(28, createActionItem(Material.BRUSH, NamedTextColor.YELLOW, TextDecoration.BOLD, "Clear Items",
                "Click to clear all items", "in all worlds"));
        gui.setItem(29, createActionItem(Material.DIAMOND_SWORD, NamedTextColor.RED, TextDecoration.BOLD, "Clear Monsters",
                "Click to clear monsters", "in all worlds"));
        gui.setItem(30, createActionItem(Material.REDSTONE, NamedTextColor.GOLD, TextDecoration.BOLD, "Clear Clusters",
                "Click to clear item", "clusters"));
        gui.setItem(31, createActionItem(Material.TNT, NamedTextColor.DARK_RED, TextDecoration.BOLD, "Emergency Cleanup",
                "Click for aggressive", "cleanup (TPS critical)"));

        // Configuration
        gui.setItem(33, createActionItem(Material.WRITABLE_BOOK, NamedTextColor.GREEN, TextDecoration.BOLD, "Reload Config",
                "Click to reload", "configuration"));

        // World Statistics (slots 46-48)
        int slot = 46;
        for (World world : Bukkit.getWorlds()) {
            if (slot > 48) break;
            gui.setItem(slot++, createWorldItem(world));
        }

        // Close button (slot 49)
        gui.setItem(49, createActionItem(Material.BARRIER, NamedTextColor.RED, TextDecoration.BOLD, "Close",
                "Click to close"));

        // Decorative glass panes
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);

        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53}) {
            gui.setItem(i, glass);
        }
    }

    private ItemStack createTPSItem(TPSMonitor tpsMonitor) {
        Material material = tpsMonitor.getCurrentTPS() >= 19.0 ? Material.LIME_DYE :
                           tpsMonitor.getCurrentTPS() >= 17.0 ? Material.YELLOW_DYE :
                           Material.RED_DYE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Server TPS", NamedTextColor.GOLD, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current: ", NamedTextColor.GRAY).append(Component.text(tpsMonitor.getPlainTPS(), NamedTextColor.WHITE)));
        lore.add(Component.text("Average: ", NamedTextColor.GRAY).append(Component.text(String.format("%.2f", tpsMonitor.getAverageTPS()), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        if (tpsMonitor.getCurrentTPS() >= 19.0) {
            lore.add(Component.text("Server running smoothly!", NamedTextColor.GREEN));
        } else if (tpsMonitor.getCurrentTPS() >= 17.0) {
            lore.add(Component.text("Server experiencing minor lag", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("Server experiencing significant lag!", NamedTextColor.RED));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMemoryItem(TPSMonitor tpsMonitor) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Memory Usage", NamedTextColor.AQUA, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(tpsMonitor.getFormattedMemory(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        double percent = tpsMonitor.getMemoryPercent();
        if (percent < 70) {
            lore.add(Component.text("Memory usage normal", NamedTextColor.GREEN));
        } else if (percent < 85) {
            lore.add(Component.text("Memory usage elevated", NamedTextColor.YELLOW));
        } else {
            lore.add(Component.text("Memory usage critical!", NamedTextColor.RED));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEntityItem(TPSMonitor tpsMonitor) {
        ItemStack item = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Entity Count", NamedTextColor.YELLOW, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Total Entities: ", NamedTextColor.GRAY).append(Component.text(tpsMonitor.getEntityCount(), NamedTextColor.WHITE)));
        lore.add(Component.text("Loaded Chunks: ", NamedTextColor.GRAY).append(Component.text(tpsMonitor.getChunkCount(), NamedTextColor.WHITE)));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCleanupItem(CleanupManager cleanupManager) {
        ItemStack item = new ItemStack(Material.BUCKET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Cleanup Stats", NamedTextColor.GREEN, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(cleanupManager.getCleanupStats(), NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createWorldItem(World world) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(world.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));

        EntityScanner scanner = new EntityScanner(plugin);
        EntityScanner.EntityStatistics stats = scanner.getWorldStatistics(world);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(stats.getFormattedStats(), NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, NamedTextColor color, TextDecoration decoration, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color, decoration));

        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(Component.text(line, NamedTextColor.GRAY));
            }
            meta.lore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void startAutoRefresh(Player player, Inventory gui) {
        int interval = plugin.getConfigManager().getGuiRefreshInterval();
        UUID playerId = player.getUniqueId();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if player is still online and viewing the GUI
                if (!player.isOnline() || !player.getOpenInventory().title().equals(GUI_TITLE)) {
                    activeRefreshTasks.remove(playerId);
                    cancel();
                    return;
                }

                // Update GUI content
                populateGUI(gui);
                player.updateInventory();
            }
        };

        int taskId = task.runTaskTimer(plugin, interval, interval).getTaskId();
        activeRefreshTasks.put(playerId, taskId);
    }

    /**
     * Cancel the refresh task for a specific player
     */
    private void cancelRefreshTask(Player player) {
        UUID playerId = player.getUniqueId();
        Integer taskId = activeRefreshTasks.remove(playerId);

        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    /**
     * Cancel all active refresh tasks (called on plugin disable)
     */
    public void cancelAllRefreshTasks() {
        for (Integer taskId : activeRefreshTasks.values()) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        activeRefreshTasks.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(GUI_TITLE)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        CleanupManager cleanupManager = plugin.getCleanupManager();
        if (cleanupManager == null) return;

        int slot = event.getSlot();

        switch (slot) {
            case 28 -> { // Clear Items
                player.closeInventory();
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.YELLOW).append(Component.text("Clearing items...", NamedTextColor.YELLOW)));
                cleanupManager.cleanupAllWorlds();
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.GREEN).append(Component.text("Items cleared!", NamedTextColor.GREEN)));
            }
            case 29 -> { // Clear Monsters
                player.closeInventory();
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.YELLOW).append(Component.text("Clearing monsters...", NamedTextColor.YELLOW)));
                int removed = 0;
                for (World world : Bukkit.getWorlds()) {
                    removed += cleanupManager.cleanupEntityType(world, org.bukkit.entity.Monster.class);
                }
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.GREEN).append(Component.text("Removed " + removed + " monsters!", NamedTextColor.GREEN)));
            }
            case 30 -> { // Clear Clusters
                player.closeInventory();
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.YELLOW).append(Component.text("Clearing item clusters...", NamedTextColor.YELLOW)));
                int removed = 0;
                for (World world : Bukkit.getWorlds()) {
                    removed += cleanupManager.cleanupItemClusters(world);
                }
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.GREEN).append(Component.text("Cleared " + removed + " clustered items!", NamedTextColor.GREEN)));
            }
            case 31 -> { // Emergency Cleanup
                player.closeInventory();
                cleanupManager.emergencyCleanup();
            }
            case 33 -> { // Reload Config
                plugin.getConfigManager().reloadConfig();
                player.sendMessage(Component.text("[KaizenClear] ", NamedTextColor.GREEN).append(Component.text("Configuration reloaded!", NamedTextColor.GREEN)));
            }
            case 49 -> { // Close
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Cancel refresh task when player closes the GUI
        if (event.getView().title().equals(GUI_TITLE) && event.getPlayer() instanceof Player player) {
            cancelRefreshTask(player);
        }
    }
}
