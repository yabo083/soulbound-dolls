package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoulboundDollsItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DollConstants.MOD_ID);

    public static final DeferredHolder<Item, PlayerDollItem> PLAYER_DOLL = ITEMS.register(
            "player_doll",
            () -> new PlayerDollItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredHolder<Item, Item> DOLL_CATALOG = ITEMS.register(
            "doll_catalog",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    private SoulboundDollsItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
