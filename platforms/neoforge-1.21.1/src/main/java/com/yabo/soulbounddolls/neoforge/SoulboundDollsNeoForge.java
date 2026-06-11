package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.command.SoulboundDollsCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DollConstants.MOD_ID)
public final class SoulboundDollsNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(DollConstants.MOD_ID);

    public SoulboundDollsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SoulboundDollsConfig.SPEC);
        SoulboundDollsComponents.register(modEventBus);
        SoulboundDollsEntities.register(modEventBus);
        SoulboundDollsItems.register(modEventBus);
        SoulboundDollsCreativeTab.register(modEventBus);
        NeoForge.EVENT_BUS.register(SoulboundDollsCommands.class);
        NeoForge.EVENT_BUS.register(SoulboundDollsRuntimeEvents.class);
        LOGGER.info("Soulbound Dolls loaded for NeoForge 1.21.1.");
    }
}
