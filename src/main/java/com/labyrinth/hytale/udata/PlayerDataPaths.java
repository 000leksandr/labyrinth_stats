package com.labyrinth.hytale.udata;

public final class PlayerDataPaths {
    private PlayerDataPaths() {}

    public static final String ENTITY = "PLAYER";

    public static final String NICKNAME = "nickname";

    public static final String MONEY = "currencies.money";

    public static final String PLAYTIME_SECONDS = "stats.playTimeSeconds";
    public static final String LOGIN_COUNT = "stats.loginCount";
    public static final String LAST_SEEN_EPOCH_MS = "stats.lastSeenEpochMs";

    public static final String MOBS_KILLED = "stats.mobsKilled";
    public static final String COINS_CONSUMED = "stats.coinsConsumed";

    // NEW: map-like object { "<mobId>": number }
    public static final String MOBS_KILLED_BY_ID_ROOT = "stats.mobsKilledById";
}
