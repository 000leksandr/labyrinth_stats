package com.labyrinth.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.labyrinth.hytale.commands.BalanceCom;
import com.labyrinth.hytale.commands.StatsCom;
import com.labyrinth.hytale.listeners.MobKillStatsEvent;
import com.labyrinth.hytale.listeners.PlayerSessionTrackerUD;
import com.labyrinth.hytale.udata.PlayerDataPaths;
import com.labyrinth.hytale.udata.PlayerDataRepository;
import com.unifieddata.api.Data;

public final class Main extends JavaPlugin {

    private static Main instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private PlayerDataRepository playerDataRepository;

    public Main(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static Main getInstance() {
        return instance;
    }

    public PlayerDataRepository getPlayerDataRepository() {
        return playerDataRepository;
    }

    @Override
    public void setup() {
        Main.LOGGER.atInfo().log("[Main] setup ENTER");

        // Create repository (no init required for UnifiedData)
        this.playerDataRepository = new PlayerDataRepository();

        // Optional: register schema indexes (only needed if you plan to use Data.query)
        Data.schema(PlayerDataPaths.ENTITY)
                .index(PlayerDataPaths.MONEY)
                .index(PlayerDataPaths.PLAYTIME_SECONDS)
                .index(PlayerDataPaths.MOBS_KILLED)
                .register();

        // --- Event listeners (same style as your example) ---
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerSessionTrackerUD::onJoin);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, PlayerSessionTrackerUD::onLeave);

        getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, CustomMoney::LivingEntityInventoryChangeEvent);

        // --- Commands ---
        this.getCommandRegistry().registerCommand(new BalanceCom());
        this.getCommandRegistry().registerCommand(new StatsCom());

        // --- ECS/System events ---
        this.getEntityStoreRegistry().registerSystem(new MobKillStatsEvent());

        Main.LOGGER.atInfo().log("[Main] setup DONE");
    }

    @Override
    public void shutdown() {
        // Nothing required for UnifiedData
        PlayerSessionTrackerUD.clear();
    }
}
