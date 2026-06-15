package com.yabo.soulbounddolls.neoforge.network;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Packet sent from client to server when player uses the teleport keybind while holding a bound doll.
 * Attempts to teleport the player to the doll's bound player if they are online and in the same dimension.
 */
public record TeleportToDollPlayerPacket() implements CustomPacketPayload {
    public static final Type<TeleportToDollPlayerPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DollConstants.MOD_ID, "teleport_to_doll_player")
    );

    public static final StreamCodec<ByteBuf, TeleportToDollPlayerPacket> STREAM_CODEC = StreamCodec.unit(new TeleportToDollPlayerPacket());

    // Track cooldowns per player UUID (server-side only)
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TeleportToDollPlayerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }

            // Check if feature is enabled
            if (!SoulboundDollsConfig.ENABLE_TELEPORT_TO_PLAYER.get()) {
                sender.displayClientMessage(
                        Component.translatable("message.soulbound_dolls.teleport.disabled")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // Check cooldown
            long now = System.currentTimeMillis();
            long cooldownMs = SoulboundDollsConfig.TELEPORT_COOLDOWN_SECONDS.get() * 1000L;
            Long lastTeleport = COOLDOWNS.get(sender.getUUID());
            if (lastTeleport != null && (now - lastTeleport) < cooldownMs) {
                long remainingSec = (cooldownMs - (now - lastTeleport)) / 1000L;
                sender.displayClientMessage(
                        Component.translatable("message.soulbound_dolls.teleport.cooldown", remainingSec)
                                .withStyle(ChatFormatting.YELLOW),
                        true
                );
                return;
            }

            // Get the held doll item
            ItemStack mainHand = sender.getMainHandItem();
            ItemStack offHand = sender.getOffhandItem();
            ItemStack dollStack = null;

            PlayerDollProfile profile = mainHand.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
            if (profile != null) {
                dollStack = mainHand;
            } else {
                profile = offHand.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
                if (profile != null) {
                    dollStack = offHand;
                }
            }

            if (profile == null || dollStack == null) {
                sender.displayClientMessage(
                        Component.translatable("message.soulbound_dolls.teleport.no_doll")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // Find the target player
            ServerLevel senderLevel = sender.serverLevel();
            ServerPlayer targetPlayer = senderLevel.getServer().getPlayerList().getPlayer(profile.uuid());

            if (targetPlayer == null) {
                sender.displayClientMessage(
                        Component.translatable("message.soulbound_dolls.teleport.offline", profile.name())
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // Check if in same dimension
            if (targetPlayer.level().dimension() != senderLevel.dimension()) {
                sender.displayClientMessage(
                        Component.translatable("message.soulbound_dolls.teleport.wrong_dimension", profile.name())
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // Find a safe teleport location near the target
            Vec3 targetPos = targetPlayer.position();
            BlockPos safePos = findSafeTeleportPos(senderLevel, targetPos);

            if (safePos == null) {
                sender.displayClientMessage(
                        Component.translatable("message.soulbound_dolls.teleport.no_safe_location")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            BlockPos originPos = sender.blockPosition();
            sender.teleportTo(senderLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                    sender.getYRot(), sender.getXRot());

            // Effects
            senderLevel.playSound(null, originPos, SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            senderLevel.playSound(null, safePos, SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

            // Messages
            sender.displayClientMessage(
                    Component.translatable("message.soulbound_dolls.teleport.success", profile.name())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );

            // Update cooldown
            COOLDOWNS.put(sender.getUUID(), now);
        });
    }

    /**
     * Find a safe position to teleport near the target player (within 3 blocks).
     */
    private static BlockPos findSafeTeleportPos(ServerLevel level, Vec3 targetPos) {
        BlockPos center = BlockPos.containing(targetPos);

        // Try positions in a 3x3 area around the target
        for (int xOff = -1; xOff <= 1; xOff++) {
            for (int zOff = -1; zOff <= 1; zOff++) {
                BlockPos candidate = center.offset(xOff, 0, zOff);

                // Check if position is safe (solid ground, air above)
                if (level.getBlockState(candidate.below()).isSolid() &&
                        level.getBlockState(candidate).isAir() &&
                        level.getBlockState(candidate.above()).isAir()) {
                    return candidate;
                }
            }
        }

        return null;
    }
}
