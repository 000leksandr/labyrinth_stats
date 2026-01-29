# 🎮 Hytale Player Statistics System

<div align="center">

**A production-ready player statistics module for Hytale servers**

Built on top of **UnifiedData v2** • Simple • Deterministic • Extensible

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![UnifiedData](https://img.shields.io/badge/UnifiedData-v2-blue.svg)](https://github.com)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

</div>

---

## ✨ Features

Track meaningful gameplay data with zero configuration:

| Feature | Path | Description |
|---------|------|-------------|
| 💰 **Player Balance** | `currencies.money` | Track player wealth |
| ⏱️ **Total Playtime** | `stats.playTimeSeconds` | Accumulated session time |
| 🔢 **Login Count** | `stats.loginCount` | Number of server joins |
| 📅 **Last Seen** | `stats.lastSeenEpochMs` | Last activity timestamp |
| ⚔️ **Total Kills** | `stats.mobsKilled` | All mobs defeated |
| 🎯 **Kills by Type** | `stats.mobsKilledById.<mobId>` | Per-mob statistics |
| 🪙 **Coins Consumed** | `stats.coinsConsumed` | Currency conversions |

### 📊 Example Data Structure

```json
{
  "nickname": "Steve",
  "currencies": {
    "money": 250
  },
  "stats": {
    "playTimeSeconds": 7200,
    "loginCount": 8,
    "lastSeenEpochMs": 1730148123123,
    "mobsKilled": 42,
    "coinsConsumed": 250,
    "mobsKilledById": {
      "Skeleton_Ranger": 12,
      "Frog_Orange": 4
    }
  }
}
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 25** (Required for both server and plugin)
- **Hytale Server** running on Java 25
- **UnifiedData v2** (`UnifiedData-1.0.0-SNAPSHOT.jar`)
- **Gradle** with Java toolchain support

> ⚠️ **Important**: UnifiedData is compiled for Java 25 and will not load on lower JVM versions.

### Installation

1. Place `UnifiedData-1.0.0-SNAPSHOT.jar` in your server's mods folder
2. Build this plugin using Gradle
3. Place the compiled JAR in your server's plugins folder
4. Restart your server

---

## 🏗️ Architecture

### Project Structure

```
com.labyrinth.hytale
├── 📄 Main.java
├── 📁 commands
│   ├── BalanceCom.java
│   └── StatsCom.java
├── 📁 listeners
│   ├── MobKillStatsEvent.java
│   └── PlayerSessionTrackerUD.java
├── 📁 udata
│   ├── PlayerDataPaths.java
│   ├── PlayerDataRepository.java
│   ├── PlayerStatsSnapshot.java
│   └── UuidUtil.java
├── 📄 BlockBreakDenyEvent.java
├── 📄 CustomMoney.java
└── 📄 DamageChatDebugEvent.java
```

**Each package has a single responsibility with no overlap.**

### 🗂️ Data Model

All player data is stored under the entity type:

```
PLAYER/<uuid>
```

#### Available Paths

| Path | Type | Purpose |
|------|------|---------|
| `nickname` | String | Player display name |
| `currencies.money` | Integer | Current balance |
| `stats.playTimeSeconds` | Long | Total playtime |
| `stats.loginCount` | Integer | Login attempts |
| `stats.lastSeenEpochMs` | Long | Last activity |
| `stats.mobsKilled` | Integer | Total mob kills |
| `stats.mobsKilledById.<mobId>` | Integer | Per-mob kills |
| `stats.coinsConsumed` | Integer | Coins converted |

> 💡 **No schema required** — UnifiedData uses freeform JSON. Paths are created automatically on first write.

---

## 🔧 Core Components

### PlayerDataPaths

**The single source of truth for all data keys.**

Defines all JSON paths used by the plugin. No hardcoded strings appear anywhere else.

```java
public static final String MONEY = "currencies.money";
public static final String MOBS_KILLED = "stats.mobsKilled";
public static final String MOBS_KILLED_BY_ID_ROOT = "stats.mobsKilledById";
```

✅ Guarantees consistency and prevents typos across the entire codebase.

---

### PlayerDataRepository

**The only class that communicates with UnifiedData.**

Centralizes all storage logic with atomic, thread-safe operations.

#### Core Methods

```java
// Initialize new player
CompletableFuture<Void> ensureDefaults(String uuid, String nickname)

// Money operations
CompletableFuture<Integer> addMoney(String uuid, int delta)
CompletableFuture<Integer> getMoney(String uuid)

// Playtime
CompletableFuture<Long> addPlaytimeSeconds(String uuid, long seconds)

// Login tracking
CompletableFuture<Void> incrementLoginCount(String uuid)
CompletableFuture<Void> setLastSeenNow(String uuid)

// Mob statistics
CompletableFuture<Void> incrementMobsKilledTotal(String uuid)
CompletableFuture<Void> incrementMobKilledById(String uuid, String mobIdSanitized)

// Coin tracking
CompletableFuture<Void> addCoinsConsumed(String uuid, int qty)

// Snapshot for commands
CompletableFuture<PlayerStatsSnapshot> getSnapshot(String uuid)
```

#### Key Operations

| Operation | Action |
|-----------|--------|
| 💰 **Money** | `currencies.money += delta` |
| ⏱️ **Playtime** | `stats.playTimeSeconds += seconds` |
| ⚔️ **Mob Kills** | `stats.mobsKilled += 1`<br>`stats.mobsKilledById.<mobId> += 1` |
| 🪙 **Coins** | `stats.coinsConsumed += qty` |
| 🔑 **Logins** | `stats.loginCount += 1` |

#### Default Values

When `ensureDefaults()` is called for a new player:

```java
{
    "nickname": playerName,
    "currencies.money": 0,
    "stats.playTimeSeconds": 0,
    "stats.loginCount": 0,
    "stats.lastSeenEpochMs": 0,
    "stats.mobsKilled": 0,
    "stats.coinsConsumed": 0
}
```

**Note:** `mobsKilledById` is not pre-created; UnifiedData creates nested objects automatically on first write.

**Every update is atomic and thread-safe.**

---

### PlayerSessionTrackerUD

**Tracks player join and leave events with thread-safe session management.**

#### Session Storage

```java
private static final ConcurrentHashMap<String, Long> joinNano = new ConcurrentHashMap<>();
```

Stores join timestamps in nanoseconds for precise playtime calculation.

#### On Join (`PlayerReadyEvent`)

```java
public static void onJoin(PlayerReadyEvent event) {
    // 1. Record join time
    joinNano.put(uuid, System.nanoTime());
    
    // 2. Initialize player data if new
    repo.ensureDefaults(uuid, nickname)
        .thenCompose(v -> repo.incrementLoginCount(uuid))
        .exceptionally(ex -> {
            Main.LOGGER.atSevere().log("[UnifiedData] onJoin error: " + ex);
            return null;
        });
}
```

- ✅ Creates default data if player is new
- ✅ Increments login count
- ✅ Records session start time

#### On Leave (`PlayerDisconnectEvent`)

```java
public static void onLeave(PlayerDisconnectEvent event) {
    Long start = joinNano.remove(uuid);
    if (start == null) return;
    
    long sessionSeconds = (long) ((System.nanoTime() - start) / 1_000_000_000.0);
    if (sessionSeconds <= 0) return;
    
    repo.addPlaytimeSeconds(uuid, sessionSeconds)
        .thenCompose(v -> repo.setLastSeenNow(uuid))
        .exceptionally(ex -> {
            Main.LOGGER.atSevere().log("[UnifiedData] onLeave error: " + ex);
            return null;
        });
}
```

- 📊 Calculates session duration in seconds
- 💾 Adds to total playtime
- 🕒 Updates `lastSeenEpochMs` timestamp
- 🧹 Removes session from memory

#### Playtime Calculation

```java
sessionSeconds = (System.nanoTime() - joinTime) / 1_000_000_000.0
stats.playTimeSeconds += sessionSeconds
```

**Uses nanosecond precision for accuracy.**

---

### MobKillStatsEvent

**Triggered when a mob dies (ECS System).**

When a player kills an entity:

```java
stats.mobsKilled += 1
stats.mobsKilledById.<mobId> += 1
```

#### Mob ID Resolution Priority

1. **NPCEntity component** → `npcTypeId` (e.g., `Skeleton_Ranger`)
2. **Entity class identifier** → from `EntityModule.getIdentifier()`
3. **Fallback** → `UNKNOWN`

#### Path Key Sanitization

Mob IDs are sanitized to ensure safe JSON path usage:

```java
private String sanitizePathKey(String raw) {
    StringBuilder sb = new StringBuilder(raw.length());
    for (int idx = 0; idx < raw.length(); idx++) {
        char c = raw.charAt(idx);
        
        boolean ok = (c >= 'a' && c <= 'z') ||
                     (c >= 'A' && c <= 'Z') ||
                     (c >= '0' && c <= '9') ||
                     c == '_' || c == '-';
        
        sb.append(ok ? c : '_');
    }
    return sb.toString();
}
```

**Replaces:** `.`, `[`, `]`, `/`, `\`, spaces, `:`, and other special characters with `_`

**Keeps only:** letters, digits, `_`, and `-`

#### Example Output

```json
"mobsKilledById": {
  "Skeleton_Ranger": 10,
  "Frog_Orange": 3,
  "Cave_Spider": 5
}
```

**Enables precise analytics per mob type.**

---

### CustomMoney

**Listens for inventory changes.**

When a Coin item is detected:
1. 🗑️ Removes coin stack from inventory
2. 💱 Converts coins into money
3. ➕ Adds money atomically
4. 📈 Tracks coins consumed
5. ❤️ Heals player (+3 HP)

```java
currencies.money += qty
stats.coinsConsumed += qty
```

**Searches all inventory sections:**
- Hotbar
- Storage
- Utility
- Armor
- Backpack

**Fully async and safe under heavy load.**

---

### Additional Components

#### BlockBreakDenyEvent.java
Custom event handler for block breaking logic.

#### DamageChatDebugEvent.java
Debug utility for damage event monitoring.

---

## 🚀 Plugin Initialization

### Main.java

The entry point that wires everything together.

#### Setup Phase

```java
@Override
public void setup() {
    // 1. Initialize repository
    this.playerDataRepository = new PlayerDataRepository();
    
    // 2. Register schema indexes (optional, for Data.query support)
    Data.schema(PlayerDataPaths.ENTITY)
            .index(PlayerDataPaths.MONEY)
            .index(PlayerDataPaths.PLAYTIME_SECONDS)
            .index(PlayerDataPaths.MOBS_KILLED)
            .register();
    
    // 3. Register event listeners
    getEventRegistry().registerGlobal(PlayerReadyEvent.class, 
        PlayerSessionTrackerUD::onJoin);
    getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, 
        PlayerSessionTrackerUD::onLeave);
    getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, 
        CustomMoney::LivingEntityInventoryChangeEvent);
    
    // 4. Register commands
    getCommandRegistry().registerCommand(new BalanceCom());
    getCommandRegistry().registerCommand(new StatsCom());
    
    // 5. Register ECS systems
    getEntityStoreRegistry().registerSystem(new MobKillStatsEvent());
}
```

#### Shutdown Phase

```java
@Override
public void shutdown() {
    PlayerSessionTrackerUD.clear();
}
```

**Singleton Access:** `Main.getInstance()` provides global plugin access.

---

## 💻 Commands

### `/balance`

Shows player money balance.

```
Your balance is: 250
```

**Implementation:**
```java
public BalanceCom() {
    super("balance", "Check your bank balance");
}

@Override
protected void execute(...) {
    repo.getMoney(uuid).thenAccept(money ->
        world.execute(() -> 
            player.sendMessage(Message.raw("Your balance is: " + money))
        )
    );
}
```

**Reads:** `currencies.money`

---

### `/stats`

Displays all tracked statistics in a single line.

```
Stats | money=250 | time=02:15:32 | mobsKilled=42 | coinsConsumed=250 | logins=8
```

**Implementation:**
```java
public StatsCom() {
    super("stats", "Show your player statistics");
}

@Override
protected void execute(...) {
    repo.getSnapshot(uuid).thenAccept(s -> {
        String time = PlayerStatsSnapshot.formatTime(s.playtimeSeconds);
        String msg = "Stats | money=" + s.money +
                " | time=" + time +
                " | mobsKilled=" + s.mobsKilled +
                " | coinsConsumed=" + s.coinsConsumed +
                " | logins=" + s.loginCount;
        
        world.execute(() -> player.sendMessage(Message.raw(msg)));
    });
}
```

**Uses:** `PlayerStatsSnapshot` for formatted output

---

## 📦 Data Components

### PlayerStatsSnapshot

**Immutable data container for read operations.**

#### Purpose
- 📥 Collect multiple UnifiedData paths in one call
- 🔄 Convert to typed Java values
- 🕐 Format playtime into readable form

#### Contains
```java
public final String uuid;
public final String nickname;
public final int money;
public final long playtimeSeconds;
public final int mobsKilled;
public final int coinsConsumed;
public final int loginCount;
```

#### Time Formatting
```java
public static String formatTime(long seconds) {
    long total = Math.max(0, seconds);
    long h = total / 3600;
    long m = (total % 3600) / 60;
    long s = total % 60;
    
    if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
    return String.format("%02d:%02d", m, s);
}
```

**Examples:**
- `7200` seconds → `02:00:00`
- `150` seconds → `02:30`

---

### UUID Handling

**UuidUtil.java** extracts UUIDs using the official Hytale API.

```java
public static String uuid(Player player) {
    Ref<EntityStore> ref = player.getReference();
    Store<EntityStore> store = ref.getStore();
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
    
    UUID id = (playerRef == null) ? null : playerRef.getUuid();
    return id == null ? null : id.toString();
}
```

**UUID is always used as the primary key in UnifiedData.**

Used throughout the codebase:
- `BalanceCom` - for balance queries
- `StatsCom` - for stats retrieval
- `CustomMoney` - for money transactions
- `PlayerSessionTrackerUD` - for session tracking
- `MobKillStatsEvent` - for kill statistics

---

## 💾 Storage Behavior

### What UnifiedData Handles

- 💿 Disk persistence
- 📝 JSON serialization
- 🧵 Threading
- 🔒 Atomicity
- ⚡ Caching

**The plugin never touches files directly.**

### Data Location

```
mods/unifieddata/data/PLAYER/<uuid>.json
```

---

## 🎯 Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Simplicity** | No SQL, no custom storage engines |
| **Deterministic** | All operations are predictable |
| **Extensible** | Easy to add new stats |
| **Safe** | Atomic updates, async handling |
| **Centralized** | Single source of truth |

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📧 Support

For issues and questions, please open an issue on GitHub.

---

<div align="center">

**Made with ❤️ for the Hytale community**

[Documentation](https://docs.example.com) • [Report Bug](https://github.com/issues) • [Request Feature](https://github.com/issues)

</div>
