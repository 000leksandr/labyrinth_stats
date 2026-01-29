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
import com.labyrinth.hytale.udata.UuidUtil;

import javax.annotation.Nonnull;

public class BalanceCom extends AbstractPlayerCommand {

    public BalanceCom() {
        super("balance", "Check your bank balance");
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

        repo.getMoney(uuid).thenAccept(money ->
                world.execute(() -> player.sendMessage(Message.raw("Your balance is: " + money)))
        );
    }
}
