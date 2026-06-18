package com.yabo.soulbounddolls.neoforge.network;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity.DollPose;
import com.yabo.soulbounddolls.neoforge.item.DollStackHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleWornDollPosePacket() implements CustomPacketPayload {
    public static final Type<CycleWornDollPosePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DollConstants.MOD_ID, "cycle_worn_doll_pose")
    );
    public static final StreamCodec<ByteBuf, CycleWornDollPosePacket> STREAM_CODEC = StreamCodec.unit(new CycleWornDollPosePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleWornDollPosePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
            if (cyclePose(headStack)) {
                player.setItemSlot(EquipmentSlot.HEAD, headStack);
                DollPose pose = DollPose.byId(headStack.getOrDefault(SoulboundDollsComponents.PLAYER_DOLL_POSE.get(), DollPose.STANDING.id()));
                player.displayClientMessage(Component.translatable("entity.soulbound_dolls.player_doll.pose." + pose.serializedName()), true);
            }
        });
    }

    public static boolean cyclePose(ItemStack stack) {
        if (!DollStackHelper.isBoundPlayerDoll(stack)) {
            return false;
        }
        DollPose currentPose = DollPose.byId(stack.getOrDefault(SoulboundDollsComponents.PLAYER_DOLL_POSE.get(), DollPose.SITTING.id()));
        stack.set(SoulboundDollsComponents.PLAYER_DOLL_POSE.get(), currentPose.next().id());
        return true;
    }

    public static boolean resetPose(ItemStack stack) {
        if (!DollStackHelper.isBoundPlayerDoll(stack) || !stack.has(SoulboundDollsComponents.PLAYER_DOLL_POSE.get())) {
            return false;
        }
        stack.remove(SoulboundDollsComponents.PLAYER_DOLL_POSE.get());
        return true;
    }
}
