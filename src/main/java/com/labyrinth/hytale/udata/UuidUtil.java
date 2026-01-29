package com.labyrinth.hytale.udata;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public final class UuidUtil {
    private UuidUtil() {}

    /**
     * way to get UUID via PlayerRef component
     */
    public static String uuid(Player player) {
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        UUID id = (playerRef == null) ? null : playerRef.getUuid();
        return id == null ? null : id.toString();
    }
}
