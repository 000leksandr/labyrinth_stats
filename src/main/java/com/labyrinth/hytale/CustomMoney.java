package com.labyrinth.hytale;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.labyrinth.hytale.udata.PlayerDataRepository;
import com.labyrinth.hytale.udata.UuidUtil;

public class CustomMoney {

    public static void LivingEntityInventoryChangeEvent(LivingEntityInventoryChangeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        World world = player.getWorld();
        if (world == null) return;

        // Run inventory logic on world thread
        world.execute(() -> {
            Inventory inv = player.getInventory();

            SlotFound found = findIn(inv.getHotbar(), "Coin", "HOTBAR");
            if (found == null) found = findIn(inv.getStorage(), "Coin", "STORAGE");
            if (found == null) found = findIn(inv.getUtility(), "Coin", "UTILITY");
            if (found == null) found = findIn(inv.getArmor(), "Coin", "ARMOR");
            if (found == null) found = findIn(inv.getBackpack(), "Coin", "BACKPACK");
            if (found == null) return;

            ItemContainer target = switch (found.sectionName) {
                case "HOTBAR" -> inv.getHotbar();
                case "STORAGE" -> inv.getStorage();
                case "UTILITY" -> inv.getUtility();
                case "ARMOR" -> inv.getArmor();
                case "BACKPACK" -> inv.getBackpack();
                default -> null;
            };
            if (target == null) return;

            ItemStack before = target.getItemStack(found.slot);
            if (before == null) return;

            String id = null;
            try { id = before.getItemId(); } catch (Throwable ignored) {}
            if (id == null) {
                try { id = before.getItem().getId(); } catch (Throwable ignored) {}
            }
            if (!"Coin".equals(id)) return;

            int qty = before.getQuantity();
            if (qty <= 0) return;

            // Remove stack from inventory
            target.removeItemStackFromSlot(found.slot);

            // Apply money + stats async (UnifiedData)
            onCoinRemoved(player, qty);

            // Keep your HP logic (same behavior)
            Store<EntityStore> store = world.getEntityStore().getStore();
            EntityStatMap entityStatMap = store.getComponent(player.getReference(), EntityStatMap.getComponentType());
            if (entityStatMap != null) {
                float hp = entityStatMap.get(DefaultEntityStatTypes.getHealth()).get();
                entityStatMap.setStatValue(DefaultEntityStatTypes.getHealth(), hp + 3f);
            }
        });
    }

    private static void onCoinRemoved(Player player, int qty) {
        Main plugin = Main.getInstance();
        if (plugin == null) return;

        PlayerDataRepository repo = plugin.getPlayerDataRepository();
        if (repo == null) return;

        World world = player.getWorld();
        if (world == null) return;

        String uuid = UuidUtil.uuid(player);
        if (uuid == null) return;

        // Atomic add money and also track coinsConsumed
        repo.addMoney(uuid, qty)
                .thenAccept(newBalance -> {
                    world.execute(() ->
                            player.sendMessage(Message.raw("Money: " + newBalance + " (+" + qty + ")"))
                    );
                })
                .exceptionally(ex -> {
                    world.execute(() -> player.sendMessage(Message.raw("UnifiedData error: " + ex.getMessage())));
                    return null;
                });

        repo.addCoinsConsumed(uuid, qty);
    }

    private static SlotFound findIn(ItemContainer container, String targetItemId, String sectionName) {
        if (container == null) return null;

        short cap = container.getCapacity();
        for (short i = 0; i < cap; i++) {
            ItemStack st = container.getItemStack(i);
            if (st == null) continue;

            String idA = null;
            try { idA = st.getItemId(); } catch (Throwable ignored) {}
            if (idA != null && targetItemId.equals(idA)) return new SlotFound(sectionName, i);

            try {
                String idB = st.getItem().getId();
                if (targetItemId.equals(idB)) return new SlotFound(sectionName, i);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static final class SlotFound {
        final String sectionName;
        final short slot;

        SlotFound(String sectionName, short slot) {
            this.sectionName = sectionName;
            this.slot = slot;
        }
    }
}
