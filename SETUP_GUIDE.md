# KaizenClear - IntelliJ IDEA Setup Guide

## Run Configurations Created

Your IDE now has **4 pre-configured run buttons** ready to use!

### How to Access Run Configurations

1. Look at the **top toolbar** in IntelliJ IDEA
2. Find the **dropdown menu** (usually shows "Current File" or a configuration name)
3. Click the dropdown to see all available configurations
4. Select one and click the **green play button ▶️** to run

---

## Available Configurations

### 1. 🔨 **Quick Build**
**Purpose:** Fast incremental build

**What it does:**
```bash
./gradlew build --no-daemon
```

**When to use:**
- Quick recompile after code changes
- Testing if your code compiles
- When you need the JAR file fast

**Output:** `build/libs/untitled-1.0.jar`

---

### 2. 🏗️ **Build KaizenClear**
**Purpose:** Clean and full rebuild

**What it does:**
```bash
./gradlew clean build --no-daemon
```

**When to use:**
- After major changes
- When Quick Build isn't working properly
- Before releasing/deploying
- To ensure a fresh build

**Output:** `build/libs/untitled-1.0.jar`

---

### 3. 🎮 **Run Paper Server**
**Purpose:** Start test server with plugin

**What it does:**
```bash
./gradlew runServer --no-daemon
```

**When to use:**
- Testing the plugin in-game
- Debugging commands and features
- Checking logs and behavior
- Development testing

**Features:**
- ✅ Auto-downloads Paper 1.21
- ✅ Auto-installs your plugin
- ✅ Creates test world
- ✅ Accepts EULA automatically

**Server location:** `run/` directory

---

### 4. ⚡ **Build & Run Server**
**Purpose:** Complete build + test cycle

**What it does:**
```bash
./gradlew clean build runServer --no-daemon
```

**When to use:**
- After making code changes
- Full test cycle
- Ensuring latest code is running
- When you want "build it and test it now"

**This is your go-to for development!**

---

## Quick Reference

| I want to... | Use This Configuration |
|--------------|----------------------|
| Test my changes quickly | **Build & Run Server** ⚡ |
| Just compile the plugin | **Quick Build** 🔨 |
| Get a clean JAR file | **Build KaizenClear** 🏗️ |
| Test without rebuilding | **Run Paper Server** 🎮 |

---

## First Time Setup

### If Run Configurations Don't Appear

1. **Restart IntelliJ IDEA** - Sometimes it needs a restart to detect new configurations
2. **Sync Gradle** - Right-click `build.gradle` → "Reload Gradle Project"
3. **Check .idea folder** - Ensure `.idea/runConfigurations/*.xml` files exist

### Server Controls

When running the Paper server:

**Stop Server:**
- Click the **red stop button** (⬛) in the Run panel
- Or type `stop` in the console

**View Logs:**
- Check the **Run** panel at the bottom of IDE
- Or view `run/logs/latest.log`

**EULA:**
- Already accepted automatically in `run/eula.txt`

---

## Development Workflow

### Recommended Flow:

1. **Make code changes** ✏️
2. **Click "Build & Run Server"** ⚡
3. **Wait for server to start** 🎮
4. **Test in-game or console** 🧪
5. **Stop server** ⬛
6. **Repeat!** 🔄

### Quick Testing Flow:

1. **Make small change** ✏️
2. **Click "Quick Build"** 🔨
3. **Copy JAR to your test server manually**
4. **Reload plugin**

---

## Keyboard Shortcuts

**Run Last Configuration:** `Shift + F10`
**Debug Last Configuration:** `Shift + F9`
**Stop:** `Ctrl + F2`
**View Run Panel:** `Alt + 4`

---

## Troubleshooting

### "Could not find or load main class"
➡️ Use **"Build KaizenClear"** for a clean build

### Server won't start
➡️ Check `run/logs/latest.log` for errors
➡️ Delete `run/` folder and try again

### Plugin not loading
➡️ Check `run/plugins/` contains `untitled-1.0.jar`
➡️ Run **"Build & Run Server"** for fresh build + test

### Changes not applying
➡️ Use **"Build & Run Server"** (includes clean build)
➡️ Don't use **"Run Paper Server"** alone after code changes

---

## File Locations

```
Project Root/
├── build/libs/
│   └── untitled-1.0.jar          # Built plugin
├── run/                           # Test server directory
│   ├── plugins/
│   │   └── untitled-1.0.jar      # Plugin copy
│   ├── logs/
│   │   └── latest.log            # Server logs
│   └── eula.txt                  # Auto-accepted
└── .idea/runConfigurations/      # IDE configurations
    ├── Quick_Build.xml
    ├── Build_KaizenClear.xml
    ├── Run_Paper_Server.xml
    └── Build___Run_Server.xml
```

---

## In-Game Testing

Once the server starts, you can test commands via console:

```
> help KaizenClear
> kc info
> kc tps
> kc help
```

**Note:** GUI requires a real Minecraft client connection!

---

## Next Steps

✅ Run configurations are set up
✅ Ready to develop

**Try it now:**
1. Click the Run dropdown
2. Select **"Build & Run Server"**
3. Watch your plugin load!

---

**Need help?** Check `README.md` and `claude.md` for full documentation.
