package com.yabo.soulbounddolls.neoforge.compat.jade;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(DollConstants.MOD_ID)
public final class SoulboundDollsJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(BoundPlayerProvider.INSTANCE, PlayerDollEntity.class);
    }

    private enum BoundPlayerProvider implements IEntityComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                DollConstants.MOD_ID,
                "bound_player");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof PlayerDollEntity doll) {
                PlayerDollProfile profile = doll.getProfile();
                tooltip.add(Component.translatable("jade.soulbound_dolls.bound_player", profile.name()));
            }
        }
    }
}
