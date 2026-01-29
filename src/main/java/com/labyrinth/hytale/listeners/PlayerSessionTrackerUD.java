package com.labyrinth.hytale.listeners;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.labyrinth.hytale.Main;
import com.labyrinth.hytale.udata.PlayerDataRepository;
import com.labyrinth.hytale.udata.UuidUtil;

import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSessionTrackerUD {

    // uuid -> join time in nano
    private static final ConcurrentHashMap<String, Long> joinNano = new ConcurrentHashMap<>();

    private PlayerSessionTrackerUD() {}

    public static void clear() {
        joinNano.clear();
    }

    public static void onJoin(PlayerReadyEvent event) {
        Main plugin = Main.getInstance();
        if (plugin == null) return;

        Player player = event.getPlayer();
        if (player == null) return;

        String uuid = UuidUtil.uuid(player);
        if (uuid == null) return;

        joinNano.put(uuid, System.nanoTime());

        PlayerDataRepository repo = plugin.getPlayerDataRepository();
        if (repo == null) return;

        String nickname = player.getDisplayName();

        // Ensure defaults + increment loginCount (async)
        repo.ensureDefaults(uuid, nickname)
                .thenCompose(v -> repo.incrementLoginCount(uuid))
                .exceptionally(ex -> {
                    Main.LOGGER.atSevere().log("[UnifiedData] onJoin error: " + ex);
                    return null;
                });
    }

    public static void onLeave(PlayerDisconnectEvent event) {
        Main plugin = Main.getInstance();
        if (plugin == null) return;

        PlayerDataRepository repo = plugin.getPlayerDataRepository();
        if (repo == null) return;

        PlayerRef pref = event.getPlayerRef();
        if (pref == null || pref.getUuid() == null) return;

        String uuid = pref.getUuid().toString();

        Long start = joinNano.remove(uuid);
        if (start == null) return;

        long sessionSeconds = (long) ((System.nanoTime() - start) / 1_000_000_000.0);
        if (sessionSeconds <= 0) return;

        // Add playtime + update lastSeen (async)
        repo.addPlaytimeSeconds(uuid, sessionSeconds)
                .thenCompose(v -> repo.setLastSeenNow(uuid))
                .exceptionally(ex -> {
                    Main.LOGGER.atSevere().log("[UnifiedData] onLeave error: " + ex);
                    return null;
                });
    }
}
