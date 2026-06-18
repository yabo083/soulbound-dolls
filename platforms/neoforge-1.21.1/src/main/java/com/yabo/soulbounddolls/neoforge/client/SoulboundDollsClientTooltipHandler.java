package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import com.yabo.soulbounddolls.neoforge.compat.curios.CuriosDollLookup;
import com.yabo.soulbounddolls.neoforge.item.DollStackHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = DollConstants.MOD_ID, value = Dist.CLIENT)
public final class SoulboundDollsClientTooltipHandler {
    private static final String PLAYER_DOLL_ITEM_ID = "soulbound_dolls:player_doll";

    private SoulboundDollsClientTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!DollStackHelper.isBoundPlayerDoll(stack)) {
            return;
        }

        event.getToolTip().removeIf(component -> PLAYER_DOLL_ITEM_ID.equals(component.getString()));

        if (player == null || !SoulboundDollsConfig.ENABLE_ENDER_MASK_PROTECTION.get()) {
            return;
        }

        if (isHeadStack(player, stack) || CuriosDollLookup.isEquippedDoll(player, stack)) {
            event.getToolTip().add(Component.translatable("item.soulbound_dolls.player_doll.tooltip.ender_mask")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static boolean isHeadStack(Player player, ItemStack stack) {
        return ItemStack.matches(player.getItemBySlot(EquipmentSlot.HEAD), stack);
    }
}
