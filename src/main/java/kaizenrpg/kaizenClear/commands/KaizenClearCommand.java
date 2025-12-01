package kaizenrpg.kaizenClear.commands;

import kaizenrpg.kaizenClear.KaizenClear;
import kaizenrpg.kaizenClear.managers.CleanupManager;
import kaizenrpg.kaizenClear.managers.TPSMonitor;
import kaizenrpg.kaizenClear.scanners.EntityScanner;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KaizenClearCommand implements CommandExecutor, TabCompleter {

    private final KaizenClear plugin;

    public KaizenClearCommand(KaizenClear plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }
                if (!player.hasPermission("kaizenclear.gui")) {
                    player.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (plugin.getGuiManager() == null) {
                    player.sendMessage("§cGUI is disabled in config.yml!");
                    return true;
                }
                plugin.getGuiManager().openMainGUI(player);
                return true;
            }

            case "clear" -> {
                if (!sender.hasPermission("kaizenclear.clear")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                return handleClear(sender, args);
            }

            case "info", "stats" -> {
                if (!sender.hasPermission("kaizenclear.commands")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                return handleInfo(sender);
            }

            case "tps" -> {
                if (!sender.hasPermission("kaizenclear.commands")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                return handleTPS(sender);
            }

            case "reload" -> {
                if (!sender.hasPermission("kaizenclear.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage("§a[KaizenClear] Configuration reloaded successfully!");
                return true;
            }

            case "help" -> {
                sendHelp(sender);
                return true;
            }

            default -> {
                sender.sendMessage("§cUnknown subcommand. Use /kaizenclear help for help.");
                return true;
            }
        }
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        CleanupManager cleanupManager = plugin.getCleanupManager();
        if (cleanupManager == null) {
            sender.sendMessage("§cCleanupManager not initialized!");
            return true;
        }

        String type = args.length > 1 ? args[1].toLowerCase() : "items";
        String worldName = args.length > 2 ? args[2] : null;

        World targetWorld = null;
        if (worldName != null) {
            targetWorld = Bukkit.getWorld(worldName);
            if (targetWorld == null) {
                sender.sendMessage("§cWorld not found: " + worldName);
                return true;
            }
        }

        sender.sendMessage("§e[KaizenClear] Starting cleanup...");

        int removed = 0;
        switch (type) {
            case "items" -> {
                // Manual clear = force clear (ignores age check)
                if (targetWorld != null) {
                    removed = cleanupManager.forceCleanupItems(targetWorld);
                } else {
                    for (World world : Bukkit.getWorlds()) {
                        removed += cleanupManager.forceCleanupItems(world);
                    }
                }
            }
            case "clusters" -> {
                if (targetWorld != null) {
                    removed = cleanupManager.cleanupItemClusters(targetWorld);
                } else {
                    for (World world : Bukkit.getWorlds()) {
                        removed += cleanupManager.cleanupItemClusters(world);
                    }
                }
            }
            case "all" -> {
                cleanupManager.emergencyCleanup();
                removed = cleanupManager.getLastCleanupCount();
            }
            default -> {
                sender.sendMessage("§cInvalid cleanup type. Use: items, clusters, or all");
                return true;
            }
        }

        sender.sendMessage("§a[KaizenClear] Cleanup complete! Removed " + removed + " entities.");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        TPSMonitor tpsMonitor = plugin.getTpsMonitor();
        CleanupManager cleanupManager = plugin.getCleanupManager();

        sender.sendMessage("§6╔══════════════════════════════════════╗");
        sender.sendMessage("§6║        §eKaizenClear Statistics       §6║");
        sender.sendMessage("§6╠══════════════════════════════════════╣");

        if (tpsMonitor != null) {
            sender.sendMessage("§6║ §eTPS: " + tpsMonitor.getFormattedTPS());
            sender.sendMessage("§6║ §eMemory: §f" + tpsMonitor.getFormattedMemory());
            sender.sendMessage("§6║ §eEntities: §f" + tpsMonitor.getEntityCount());
            sender.sendMessage("§6║ §eChunks: §f" + tpsMonitor.getChunkCount());
        }

        if (cleanupManager != null) {
            sender.sendMessage("§6║ §eCleanup: §f" + cleanupManager.getCleanupStats());
        }

        sender.sendMessage("§6╚══════════════════════════════════════╝");
        return true;
    }

    private boolean handleTPS(CommandSender sender) {
        TPSMonitor tpsMonitor = plugin.getTpsMonitor();
        if (tpsMonitor == null) {
            sender.sendMessage("§cTPS monitoring is disabled!");
            return true;
        }

        sender.sendMessage("§6═══════ §eTPS Information §6═══════");
        sender.sendMessage("§eCurrent TPS: " + tpsMonitor.getFormattedTPS());
        sender.sendMessage("§eAverage TPS: §f" + String.format("%.2f", tpsMonitor.getAverageTPS()));
        sender.sendMessage("§eMemory Usage: §f" + tpsMonitor.getFormattedMemory());
        sender.sendMessage("§eTotal Entities: §f" + tpsMonitor.getEntityCount());
        sender.sendMessage("§eLoaded Chunks: §f" + tpsMonitor.getChunkCount());
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6╔══════════════════════════════════════╗");
        sender.sendMessage("§6║     §eKaizenClear Commands Help      §6║");
        sender.sendMessage("§6╠══════════════════════════════════════╣");
        sender.sendMessage("§6║ §e/kc gui §7- Open GUI dashboard");
        sender.sendMessage("§6║ §e/kc clear <type> [world] §7- Clear entities");
        sender.sendMessage("§6║   §7Types: items, clusters, all");
        sender.sendMessage("§6║ §e/kc info §7- Show statistics");
        sender.sendMessage("§6║ §e/kc tps §7- Show TPS information");
        sender.sendMessage("§6║ §e/kc reload §7- Reload configuration");
        sender.sendMessage("§6║ §e/kc help §7- Show this help");
        sender.sendMessage("§6╚══════════════════════════════════════╝");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("gui", "clear", "info", "tps", "reload", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            completions.addAll(Arrays.asList("items", "clusters", "all"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("clear")) {
            completions.addAll(Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.toList()));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
