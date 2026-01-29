package com.labyrinth.hytale.udata;

public final class PlayerStatsSnapshot {
    public final String uuid;
    public final String nickname;

    public final int money;
    public final long playtimeSeconds;

    public final int mobsKilled;
    public final int coinsConsumed;
    public final int loginCount;

    public PlayerStatsSnapshot(
            String uuid,
            String nickname,
            int money,
            long playtimeSeconds,
            int mobsKilled,
            int coinsConsumed,
            int loginCount
    ) {
        this.uuid = uuid;
        this.nickname = nickname;
        this.money = money;
        this.playtimeSeconds = playtimeSeconds;
        this.mobsKilled = mobsKilled;
        this.coinsConsumed = coinsConsumed;
        this.loginCount = loginCount;
    }

    public static String formatTime(long seconds) {
        long total = Math.max(0, seconds);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;

        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}
