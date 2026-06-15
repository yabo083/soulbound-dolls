package com.yabo.soulbounddolls.neoforge;

import com.yabo.soulbounddolls.common.DollConstants;
import com.yabo.soulbounddolls.neoforge.entity.DollProjectileEntity;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoulboundDollsEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DollConstants.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<PlayerDollEntity>> PLAYER_DOLL = ENTITIES.register(
            "player_doll",
            () -> EntityType.Builder.<PlayerDollEntity>of(PlayerDollEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.75F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("player_doll")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<DollProjectileEntity>> DOLL_PROJECTILE = ENTITIES.register(
            "doll_projectile",
            () -> EntityType.Builder.<DollProjectileEntity>of(DollProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("doll_projectile")
    );

    private SoulboundDollsEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
