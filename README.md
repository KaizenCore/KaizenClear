# KaizenClear - Advanced Lag Remover & Entity Manager

Advanced Minecraft plugin for Paper/Spigot 1.21+ that intelligently manages entities, items, and server performance.

## Features

✅ **Real-time TPS Monitoring** - Track server performance with color-coded indicators
✅ **Smart Item Cleanup** - Configurable item removal with whitelist/blacklist support
✅ **Cluster Detection** - Removes dense entity clusters to prevent lag
✅ **Interactive GUI Dashboard** - Manage everything from an in-game menu
✅ **Multi-World Support** - Different settings per world
✅ **Scheduled Cleanup** - Automated cleanup with player warnings
✅ **Permission System** - Granular control over who7 can use features
✅ **Statistics Tracking** - Monitor cleanup history and performance


| Configuration | Description |
|---------------|-------------|
| **Quick Build** | Fast build (no clean) |
| **Build KaizenClear** | Clean + build |
| **Run Paper Server** | Start test server |
| **Build & Run Server** | Build plugin + start server |

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/kc gui` | Open the GUI dashboard | `kaizenclear.gui` |
| `/kc clear <type> [world]` | Manual cleanup | `kaizenclear.clear` |
| `/kc info` | Show statistics | `kaizenclear.commands` |
| `/kc tps` | Display TPS information | `kaizenclear.commands` |
| `/kc reload` | Reload configuration | `kaizenclear.admin` |
| `/kc help` | Show help | `kaizenclear.commands` |

**Cleanup Types:** `items`, `clusters`, `all`

## Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `kaizenclear.*` | All permissions | OP |
| `kaizenclear.admin` | Admin access | OP |
| `kaizenclear.gui` | Access GUI | OP |
| `kaizenclear.clear` | Manual cleanup | OP |
| `kaizenclear.bypass` | Immune to cleanup | False |
| `kaizenclear.notify` | Receive notifications | OP |

## Configuration

The plugin creates `config.yml` with extensive options:

```yaml
tps:
  warning-threshold: 18.0
  critical-threshold: 15.0
  auto-cleanup-enabled: true

items:
  default-lifetime: 300  # 5 minutes
  whitelist:
    - DIAMOND
    - NETHERITE_INGOT
  blacklist:
    - COBBLESTONE
    - DIRT

entities:
  types:
    items:
      cluster-size: 20
      cluster-radius: 5
```



## GUI Dashboard

Open with `/kc gui` to access:

- **Live TPS & Memory Stats**
- **Entity Counts** - Per-world breakdown
- **Quick Actions** - One-click cleanup buttons
- **World Statistics** - Detailed entity info
- **Config Reload** - Apply changes instantly

## Development

### Project Structure

```
src/main/java/kaizenrpg/kaizenClear/
├── KaizenClear.java           # Main plugin class
├── commands/
│   └── KaizenClearCommand.java  # Command handler
├── managers/
│   ├── ConfigManager.java        # Configuration
│   ├── TPSMonitor.java           # TPS tracking
│   └── CleanupManager.java       # Cleanup logic
├── scanners/
│   └── EntityScanner.java        # Entity analysis
└── gui/
    └── GUIManager.java           # GUI system
```

KaizenClear (1.0)
```

**Version**: 1.21+
**API Version**: 1.21 (Paper 1.21.8+)
**Author**: KaizenCore
**Folia Support**: Yes
