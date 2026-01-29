package com.labyrinth.hytale.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.labyrinth.hytale.Main;
import com.labyrinth.hytale.udata.PlayerDataRepository;
import com.labyrinth.hytale.udata.PlayerStatsSnapshot;
import com.labyrinth.hytale.udata.UuidUtil;

import javax.annotation.Nonnull;

public class StatsCom extends AbstractPlayerCommand {

    public StatsCom() {
        super("stats", "Show your player statistics");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        Main plugin = Main.getInstance();
        if (plugin == null) return;

        PlayerDataRepository repo = plugin.getPlayerDataRepository();
        if (repo == null) return;

        String uuid = UuidUtil.uuid(player);
        if (uuid == null) return;

        repo.getSnapshot(uuid).thenAccept(s -> {
            String time = PlayerStatsSnapshot.formatTime(s.playtimeSeconds);

            String msg =
                    "Stats | money=" + s.money +
                            " | time=" + time +
                            " | mobsKilled=" + s.mobsKilled +
                            " | coinsConsumed=" + s.coinsConsumed +
                            " | logins=" + s.loginCount;

            // Always send messages on the world thread
            world.execute(() -> player.sendMessage(Message.raw(msg)));
        });
    }
}
