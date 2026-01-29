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
│   ├── PlayerSessionTrackerUD.java
│   └── MobKillStatsEvent.java
└── 📁 udata
    ├── PlayerDataPaths.java
    ├── PlayerDataRepository.java
    ├── PlayerStatsSnapshot.java
    └── UuidUtil.java
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

#### Key Operations

| Operation | Action |
|-----------|--------|
| 💰 **Money** | `currencies.money += delta` |
| ⏱️ **Playtime** | `stats.playTimeSeconds += seconds` |
| ⚔️ **Mob Kills** | `stats.mobsKilled += 1`<br>`stats.mobsKilledById.<mobId> += 1` |
| 🪙 **Coins** | `stats.coinsConsumed += qty` |
| 🔑 **Logins** | `stats.loginCount += 1` |

**Every update is atomic and thread-safe.**

---

### PlayerSessionTrackerUD

**Tracks player join and leave events.**

#### On Join
- ✅ Creates default data if player is new
- ✅ Increments login count

#### On Leave
- 📊 Calculates session time
- 💾 Adds to total playtime
- 🕒 Updates `lastSeen` timestamp

```java
sessionSeconds = now - joinTime
stats.playTimeSeconds += sessionSeconds
```

---

### MobKillStatsEvent

**Triggered when a mob dies.**

When a player kills an entity:

```java
stats.mobsKilled += 1
stats.mobsKilledById.<mobId> += 1
```

#### Example Output

```json
"mobsKilledById": {
  "Skeleton_Ranger": 10,
  "Frog_Orange": 3
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

```java
currencies.money += qty
stats.coinsConsumed += qty
```

**Fully async and safe under heavy load.**

---

## 💻 Commands

### `/balance`

Shows player money balance.

```
Your balance is: 250
```

**Reads:** `currencies.money`

---

### `/stats`

Displays all tracked statistics.

```
Stats:
Money: 250
Playtime: 02:15:32
Mobs killed: 42
Coins consumed: 250
Logins: 8
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
- `uuid`
- `nickname`
- `money`
- `playtimeSeconds`
- `mobsKilled`
- `coinsConsumed`
- `loginCount`

---

### UUID Handling

UUIDs are extracted using the official Hytale API:

```java
Ref<EntityStore> ref = player.getReference();
Store<EntityStore> store = ref.getStore();
PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
UUID id = playerRef.getUuid();
```

**UUID is always used as the primary key in UnifiedData.**

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
