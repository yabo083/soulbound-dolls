package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.DollConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoulboundDollsCreativeTab {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DollConstants.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.soulbound_dolls.main"))
                    .icon(() -> new ItemStack(SoulboundDollsItems.PLAYER_DOLL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(SoulboundDollsItems.PLAYER_DOLL.get());
                        output.accept(SoulboundDollsItems.DOLL_CATALOG.get());
                    })
                    .build()
    );

    private SoulboundDollsCreativeTab() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
