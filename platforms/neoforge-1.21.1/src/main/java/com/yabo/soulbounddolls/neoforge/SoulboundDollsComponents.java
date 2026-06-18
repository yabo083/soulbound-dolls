package com.yabo.soulbounddolls.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.common.PlayerDollProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoulboundDollsComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DollConstants.MOD_ID);

    public static final Codec<PlayerDollProfile> PLAYER_DOLL_PROFILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(PlayerDollProfile::uuid),
            Codec.STRING.fieldOf("name").forGetter(PlayerDollProfile::name),
            Codec.STRING.optionalFieldOf("skin_value", "").forGetter(PlayerDollProfile::skinValue),
            Codec.STRING.optionalFieldOf("skin_signature", "").forGetter(PlayerDollProfile::skinSignature),
            Codec.BOOL.optionalFieldOf("slim_model", false).forGetter(PlayerDollProfile::slimModel),
            Codec.LONG.optionalFieldOf("last_updated", 0L).forGetter(PlayerDollProfile::lastUpdated)
    ).apply(instance, PlayerDollProfile::of));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PlayerDollProfile> PLAYER_DOLL_PROFILE_STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            PlayerDollProfile::uuid,
            ByteBufCodecs.STRING_UTF8,
            PlayerDollProfile::name,
            ByteBufCodecs.STRING_UTF8,
            PlayerDollProfile::skinValue,
            ByteBufCodecs.STRING_UTF8,
            PlayerDollProfile::skinSignature,
            ByteBufCodecs.BOOL,
            PlayerDollProfile::slimModel,
            ByteBufCodecs.VAR_LONG,
            PlayerDollProfile::lastUpdated,
            PlayerDollProfile::of
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlayerDollProfile>> PLAYER_DOLL_PROFILE = COMPONENTS.register(
            "player_doll_profile",
            () -> DataComponentType.<PlayerDollProfile>builder()
                    .persistent(PLAYER_DOLL_PROFILE_CODEC)
                    .networkSynchronized(PLAYER_DOLL_PROFILE_STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PLAYER_DOLL_POSE = COMPONENTS.register(
            "player_doll_pose",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    private SoulboundDollsComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
