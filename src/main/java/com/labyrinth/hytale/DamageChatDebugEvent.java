//package com.labyrinth.hytale;
//
//import com.hypixel.hytale.component.Archetype;
//import com.hypixel.hytale.component.ArchetypeChunk;
//import com.hypixel.hytale.component.CommandBuffer;
//import com.hypixel.hytale.component.Ref;
//import com.hypixel.hytale.component.Store;
//import com.hypixel.hytale.component.query.Query;
//import com.hypixel.hytale.component.system.EntityEventSystem;
//import com.hypixel.hytale.server.core.Message;
//import com.hypixel.hytale.server.core.entity.Entity;
//import com.hypixel.hytale.server.core.entity.EntityUtils;
//import com.hypixel.hytale.server.core.modules.entity.EntityModule;
//import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
//import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource;
//import com.hypixel.hytale.server.core.modules.entity.damage.Damage.ProjectileSource;
//import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
//import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
//import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
//import com.hypixel.hytale.server.core.universe.PlayerRef;
//import com.hypixel.hytale.server.core.universe.Universe;
//import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//import com.hypixel.hytale.server.npc.entities.NPCEntity;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//
//public class DamageChatDebugEvent extends EntityEventSystem<EntityStore, Damage> {
//
//    public DamageChatDebugEvent() {
//        super(Damage.class);
//    }
//
//    @Override
//    public void handle(
//            int i,
//            @Nonnull ArchetypeChunk<EntityStore> chunk,
//            @Nonnull Store<EntityStore> store,
//            @Nonnull CommandBuffer<EntityStore> commandBuffer,
//            @Nonnull Damage event
//    ) {
//        // target = entity which received damage (index i)
//        Ref<EntityStore> targetRef = chunk.getReferenceTo(i);
//        if (targetRef == null || !targetRef.isValid()) return;
//
//        Entity targetEntity = EntityUtils.getEntity(i, chunk);
//        if (targetEntity == null) return;
//
//        // attacker (entity or projectile shooter)
//        Ref<EntityStore> attackerRef = extractAttackerRef(event);
//        if (attackerRef == null || !attackerRef.isValid()) return;
//
//        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, Universe.get().getPlayerRefComponentType());
//        if (attackerPlayerRef == null) return;
//
//        String targetId = resolveTargetId(store, chunk, i, targetRef, targetEntity);
//        int hp = getCurrentHp(store, chunk, i, targetRef);
//
//        String msg =
//                "Damage -> target=" + targetId +
//                        " | HP=" + hp +
//                        " | dmg=" + event.getAmount() +
//                        " | attacker=" + attackerPlayerRef.getUsername();
//
//        attackerPlayerRef.sendMessage(Message.raw(msg));
//
//        if(hp == 0){
//            attackerPlayerRef.sendMessage(Message.raw("YOU KILLED IT FUCKER"));
//        }
//    }
//
//    @Override
//    public Query<EntityStore> getQuery() {
//        return Archetype.empty();
//    }
//
//    // ---------------- attacker source ----------------
//
//    @Nullable
//    private Ref<EntityStore> extractAttackerRef(@Nonnull Damage event) {
//        Damage.Source source = event.getSource();
//        if (source instanceof EntitySource es) return es.getRef();          // shooter / attacker ref
//        if (source instanceof ProjectileSource ps) return ps.getRef();      // shooter ref (ProjectileSource extends EntitySource)
//        return null;
//    }
//
//    // ---------------- HP (актуальне, без reflection) ----------------
//
//    private int getCurrentHp(
//            @Nonnull Store<EntityStore> store,
//            @Nonnull ArchetypeChunk<EntityStore> chunk,
//            int index,
//            @Nonnull Ref<EntityStore> targetRef
//    ) {
//        EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
//        if (stats == null) stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
//        if (stats == null) return -1;
//
//        EntityStatValue health = stats.get(DefaultEntityStatTypes.getHealth());
//        if (health == null) return -1;
//
//        return Math.round(health.get()); // ✅ current HP
//    }
//
//    // ---------------- ID (NPC roleName / fallback class id) ----------------
//
//    @Nonnull
//    private String resolveTargetId(
//            @Nonnull Store<EntityStore> store,
//            @Nonnull ArchetypeChunk<EntityStore> chunk,
//            int index,
//            @Nonnull Ref<EntityStore> targetRef,
//            @Nonnull Entity targetEntity
//    ) {
//        // 1) If NPCEntity component exists -> roleName (npc type id)
//        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
//        if (npc == null) npc = store.getComponent(targetRef, NPCEntity.getComponentType());
//        if (npc != null) {
//            String id = npc.getNPCTypeId(); // in твоєму декомпілі: return roleName;
//            if (id != null && !id.isEmpty()) return id;
//        }
//
//        // 2) Fallback: identifier of entity class
//        String classId = EntityModule.get().getIdentifier(targetEntity.getClass());
//        if (classId != null && !classId.isEmpty()) return classId;
//
//        return "UNKNOWN";
//    }
//}
