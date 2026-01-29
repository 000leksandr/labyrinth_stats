//package com.labyrinth.hytale;
//
//import com.hypixel.hytale.component.Archetype;
//import com.hypixel.hytale.component.ArchetypeChunk;
//import com.hypixel.hytale.component.CommandBuffer;
//import com.hypixel.hytale.component.Ref;
//import com.hypixel.hytale.component.Store;
//import com.hypixel.hytale.component.query.Query;
//import com.hypixel.hytale.component.system.EntityEventSystem;
//import com.hypixel.hytale.protocol.GameMode;
//import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
//import com.hypixel.hytale.server.core.Message;
//import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
//import com.hypixel.hytale.server.core.permissions.PermissionsModule;
//import com.hypixel.hytale.server.core.universe.PlayerRef;
//import com.hypixel.hytale.server.core.universe.Universe;
//import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//import com.hypixel.hytale.server.core.util.NotificationUtil;
//
//import javax.annotation.Nonnull;
//import java.util.Set;
//
//public class BlockBreakDenyEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {
//
//    public BlockBreakDenyEvent() {
//        super(BreakBlockEvent.class);
//    }
//
//    @Override
//    public void handle(
//            int i,
//            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
//            @Nonnull Store<EntityStore> store,
//            @Nonnull CommandBuffer<EntityStore> commandBuffer,
//            @Nonnull BreakBlockEvent event
//    ) {
//        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
//
//        PlayerRef playerRef = store.getComponent(ref, Universe.get().getPlayerRefComponentType());
//        if (playerRef == null) {
//            event.setCancelled(true);
//            return;
//        }
//
//        // Витягуємо всі групи гравця
//        Set<String> groups = PermissionsModule.get().getGroupsForUser(playerRef.getUuid());
//
//        // Якщо є Creative → можна ламати
//        boolean isCreative = groups.contains(GameMode.Creative.toString());
//
//        if (isCreative) {
//            return; // Creative можна ламати
//        }
//
//        // Інакше (Adventure) — забороняємо
//        event.setCancelled(true);
//
//        var packetHandler = playerRef.getPacketHandler();
//        var msg = Message.raw("You can't break blocks in Adventure mode").color("#ff5b5b");
//        NotificationUtil.sendNotification(packetHandler, msg, NotificationStyle.Danger);
//    }
//
//    @Override
//    public Query<EntityStore> getQuery() {
//        return Archetype.empty();
//    }
//}
