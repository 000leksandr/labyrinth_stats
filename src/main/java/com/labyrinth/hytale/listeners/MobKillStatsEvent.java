package com.labyrinth.hytale.listeners;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.ProjectileSource;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.labyrinth.hytale.Main;
import com.labyrinth.hytale.udata.PlayerDataRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobKillStatsEvent extends EntityEventSystem<EntityStore, Damage> {

    public MobKillStatsEvent() {
        super(Damage.class);
    }

    @Override
    public void handle(
            int i,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage event
    ) {
        Ref<EntityStore> targetRef = chunk.getReferenceTo(i);
        if (targetRef == null || !targetRef.isValid()) return;

        Entity targetEntity = EntityUtils.getEntity(i, chunk);
        if (targetEntity == null) return;

        int hp = getCurrentHp(store, chunk, i, targetRef);
        if (hp != 0) return; // only when the target is dead

        Ref<EntityStore> attackerRef = extractAttackerRef(event);
        if (attackerRef == null || !attackerRef.isValid()) return;

        PlayerRef attackerPlayerRef = store.getComponent(attackerRef, Universe.get().getPlayerRefComponentType());
        if (attackerPlayerRef == null || attackerPlayerRef.getUuid() == null) return;

        Main plugin = Main.getInstance();
        if (plugin == null) return;

        PlayerDataRepository repo = plugin.getPlayerDataRepository();
        if (repo == null) return;

        String attackerUuid = attackerPlayerRef.getUuid().toString();

        // Resolve mob id (NPC type id or fallback identifier)
        String rawTargetId = resolveTargetId(store, chunk, i, targetRef, targetEntity);
        String safeTargetId = sanitizePathKey(rawTargetId);

        // Reasonable stats:
        // 1) total mobs killed
        repo.incrementMobsKilledTotal(attackerUuid);

        // 2) mobs killed by id (Skeleton_Ranger, etc.)
        if (!"UNKNOWN".equals(safeTargetId)) {
            repo.incrementMobKilledById(attackerUuid, safeTargetId);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Nullable
    private Ref<EntityStore> extractAttackerRef(@Nonnull Damage event) {
        Damage.Source source = event.getSource();
        if (source instanceof EntitySource es) return es.getRef();
        if (source instanceof ProjectileSource ps) return ps.getRef();
        return null;
    }

    private int getCurrentHp(
            @Nonnull Store<EntityStore> store,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int index,
            @Nonnull Ref<EntityStore> targetRef
    ) {
        EntityStatMap stats = chunk.getComponent(index, EntityStatMap.getComponentType());
        if (stats == null) stats = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (stats == null) return -1;

        EntityStatValue health = stats.get(DefaultEntityStatTypes.getHealth());
        if (health == null) return -1;

        return Math.round(health.get());
    }

    @Nonnull
    private String resolveTargetId(
            @Nonnull Store<EntityStore> store,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            int index,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull Entity targetEntity
    ) {
        // 1) NPCEntity -> npcTypeId (roleName)
        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null) npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc != null) {
            String id = npc.getNPCTypeId();
            if (id != null && !id.isEmpty()) return id;
        }

        // 2) Fallback: entity class identifier
        String classId = EntityModule.get().getIdentifier(targetEntity.getClass());
        if (classId != null && !classId.isEmpty()) return classId;

        return "UNKNOWN";
    }

    /**
     * Makes a safe key for dot-path usage:
     * - Replaces '.', '[', ']', '/', '\\', spaces, ':' and other weird chars.
     * - Keeps letters, digits, '_' and '-' only.
     */
    private String sanitizePathKey(String raw) {
        if (raw == null || raw.isEmpty()) return "UNKNOWN";

        StringBuilder sb = new StringBuilder(raw.length());
        for (int idx = 0; idx < raw.length(); idx++) {
            char c = raw.charAt(idx);

            boolean ok =
                    (c >= 'a' && c <= 'z') ||
                            (c >= 'A' && c <= 'Z') ||
                            (c >= '0' && c <= '9') ||
                            c == '_' || c == '-';

            sb.append(ok ? c : '_');
        }

        // Avoid empty or only underscores
        String out = sb.toString();
        if (out.replace("_", "").isEmpty()) return "UNKNOWN";
        return out;
    }
}
