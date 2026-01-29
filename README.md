# Hytale Player Statistics System (UnifiedData v2)

A production-ready player statistics module for Hytale servers, built on top of **UnifiedData v2**.  
All data is stored as freeform JSON, updated asynchronously, and written atomically.  
No SQL. No custom storage engines. No manual file handling.

This module is designed to be simple, deterministic, and easily extensible.

---

## Features

The system tracks only meaningful gameplay data:

- Player balance (`currencies.money`)
- Total playtime (`stats.playTimeSeconds`)
- Login count (`stats.loginCount`)
- Last seen timestamp (`stats.lastSeenEpochMs`)
- Total mobs killed (`stats.mobsKilled`)
- Mobs killed per mob ID (`stats.mobsKilledById.<mobId>`)
- Coins converted into money (`stats.coinsConsumed`)

Example stored entity:

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
---

## Requirements

- Java **25**
- Hytale Server running on Java 25
- UnifiedData v2 (`UnifiedData-1.0.0-SNAPSHOT.jar`)
- Gradle with Java toolchain support

Both the server and this plugin must use Java 25.  
UnifiedData is compiled for Java 25 and will not load on lower JVM versions.

---

## Project Structure

The plugin is split into clear and strict layers:

com.labyrinth.hytale
├── Main.java
├── commands
│ ├── BalanceCom.java
│ └── StatsCom.java
├── listeners
│ ├── PlayerSessionTrackerUD.java
│ └── MobKillStatsEvent.java
└── udata
├── PlayerDataPaths.java
├── PlayerDataRepository.java
├── PlayerStatsSnapshot.java
└── UuidUtil.java


Each package has a single responsibility and does not overlap.

---

## Data Model

All player data is stored under the entity type:

PLAYER/<uuid>


Paths used in the system:

nickname
currencies.money
stats.playTimeSeconds
stats.loginCount
stats.lastSeenEpochMs
stats.mobsKilled
stats.mobsKilledById.<mobId>
stats.coinsConsumed


No schema is required because UnifiedData uses freeform JSON.  
Paths are created automatically when first written.

---

## PlayerDataPaths

This class defines all JSON paths used by the plugin.

It is the only place where data keys exist.  
No hardcoded strings appear anywhere else.

Example:

```java
public static final String MONEY = "currencies.money";
public static final String MOBS_KILLED = "stats.mobsKilled";
public static final String MOBS_KILLED_BY_ID_ROOT = "stats.mobsKilledById";
This guarantees consistency and prevents typos across the entire codebase.

PlayerDataRepository
This is the only class that communicates with UnifiedData.
All storage logic is centralized here.

Responsibilities:

Initialize new player data

Atomic numeric updates

Snapshot reads for commands

Safe async handling

Data consistency

Key operations:

Money:
currencies.money += delta

Playtime:
stats.playTimeSeconds += seconds

Mob kills:
stats.mobsKilled += 1
stats.mobsKilledById.<mobId> += 1

Coins:
stats.coinsConsumed += qty

Logins:
stats.loginCount += 1
Every update is atomic and thread-safe.

PlayerSessionTrackerUD
Tracks player join and leave events.

On join:

- Creates default data if player is new
- Increments login count
On leave:

- Calculates session time
- Adds it to total playtime
- Updates lastSeen timestamp
Playtime logic:

sessionSeconds = now - joinTime
stats.playTimeSeconds += sessionSeconds
MobKillStatsEvent
Triggered when a mob dies.

When a player kills an entity:

stats.mobsKilled += 1
stats.mobsKilledById.<mobId> += 1
Example:

"mobsKilledById": {
  "Skeleton_Ranger": 10,
  "Frog_Orange": 3
}
This allows precise analytics per mob type.

CustomMoney
Listens for inventory changes.

When a Coin item is detected:

- Removes coin stack from inventory
- Converts coins into money
- Adds money atomically
- Tracks coins consumed
Logic:

currencies.money += qty
stats.coinsConsumed += qty
This is fully async and safe under heavy load.

Commands
/balance
Shows player money:

Your balance is: <money>
Reads:

currencies.money
/stats
Displays all tracked statistics:

Stats:
Money: 250
Playtime: 02:15:32
Mobs killed: 42
Coins consumed: 250
Logins: 8
Uses PlayerStatsSnapshot to format data.

PlayerStatsSnapshot
Immutable data container.

Purpose:

Collect multiple UnifiedData paths in one call

Convert them into typed Java values

Format playtime into readable form

It contains:

uuid
nickname
money
playtimeSeconds
mobsKilled
coinsConsumed
loginCount
UUID Handling
UUIDs are extracted using the official Hytale API:

Ref<EntityStore> ref = player.getReference();
Store<EntityStore> store = ref.getStore();
PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
UUID id = playerRef.getUuid();
UUID is always used as the primary key in UnifiedData.

Storage Behavior
UnifiedData handles:

Disk persistence

JSON serialization

Threading

Atomicity

Caching

The plugin never touches files directly.

Data is stored in:

mods/unifieddata/data/PLAYER/<uuid>.json
