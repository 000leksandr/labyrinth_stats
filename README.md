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
