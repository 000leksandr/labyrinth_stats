package com.labyrinth.hytale.udata;

import com.unifieddata.api.Data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class PlayerDataRepository {

    public CompletableFuture<Void> ensureDefaults(String uuid, String nickname) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .exists(PlayerDataPaths.LOGIN_COUNT)
                .thenCompose(exists -> {
                    if (exists) return CompletableFuture.completedFuture(null);

                    return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                            .setAll(Map.of(
                                    PlayerDataPaths.NICKNAME, nickname,
                                    PlayerDataPaths.MONEY, 0,
                                    PlayerDataPaths.PLAYTIME_SECONDS, 0,
                                    PlayerDataPaths.LOGIN_COUNT, 0,
                                    PlayerDataPaths.LAST_SEEN_EPOCH_MS, 0,
                                    PlayerDataPaths.MOBS_KILLED, 0,
                                    PlayerDataPaths.COINS_CONSUMED, 0
                                    // Note: we don't need to pre-create mobsKilledById - dot paths will create objects.
                            ))
                            .thenApply(ignored -> null);
                });
    }

    public CompletableFuture<Long> addPlaytimeSeconds(String uuid, long seconds) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .add(PlayerDataPaths.PLAYTIME_SECONDS, seconds)   // CompletableFuture<Number>
                .thenApply(n -> n == null ? 0L : n.longValue());
    }

    public CompletableFuture<Integer> addMoney(String uuid, int delta) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .add(PlayerDataPaths.MONEY, delta)                // CompletableFuture<Number>
                .thenApply(n -> n == null ? 0 : n.intValue());
    }

    public CompletableFuture<Integer> getMoney(String uuid) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .getInt(PlayerDataPaths.MONEY, 0);
    }

    public CompletableFuture<Void> incrementLoginCount(String uuid) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .increment(PlayerDataPaths.LOGIN_COUNT)
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> setLastSeenNow(String uuid) {
        long now = System.currentTimeMillis();
        Data.from(PlayerDataPaths.ENTITY).entity(uuid).set(PlayerDataPaths.LAST_SEEN_EPOCH_MS, now);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> incrementMobsKilledTotal(String uuid) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .increment(PlayerDataPaths.MOBS_KILLED)
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> addCoinsConsumed(String uuid, int qty) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .add(PlayerDataPaths.COINS_CONSUMED, qty)
                .thenApply(ignored -> null);
    }

    /**
     * NEW: Increment kill counter for a specific mob id:
     * stats.mobsKilledById.<mobId> += 1
     */
    public CompletableFuture<Void> incrementMobKilledById(String uuid, String mobIdSanitized) {
        String path = PlayerDataPaths.MOBS_KILLED_BY_ID_ROOT + "." + mobIdSanitized;
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .increment(path)
                .thenApply(ignored -> null);
    }

    public CompletableFuture<PlayerStatsSnapshot> getSnapshot(String uuid) {
        return Data.from(PlayerDataPaths.ENTITY).entity(uuid)
                .get(
                        PlayerDataPaths.NICKNAME,
                        PlayerDataPaths.MONEY,
                        PlayerDataPaths.PLAYTIME_SECONDS,
                        PlayerDataPaths.MOBS_KILLED,
                        PlayerDataPaths.COINS_CONSUMED,
                        PlayerDataPaths.LOGIN_COUNT
                )
                .thenApply(map -> new PlayerStatsSnapshot(
                        uuid,
                        map.get(PlayerDataPaths.NICKNAME).asString("Unknown"),
                        map.get(PlayerDataPaths.MONEY).asInt(0),
                        map.get(PlayerDataPaths.PLAYTIME_SECONDS).asLong(0L),
                        map.get(PlayerDataPaths.MOBS_KILLED).asInt(0),
                        map.get(PlayerDataPaths.COINS_CONSUMED).asInt(0),
                        map.get(PlayerDataPaths.LOGIN_COUNT).asInt(0)
                ));
    }
}
