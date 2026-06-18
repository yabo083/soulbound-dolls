package com.yabo.soulbounddolls.neoforge.entity;

import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import com.yabo.soulbounddolls.neoforge.item.DollStackHelper;
import com.yabo.soulbounddolls.neoforge.item.PlayerDollItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ZombieMoveToDollGoal extends Goal {
    private static final double PICKUP_DISTANCE_SQR = 2.25D;

    private final Zombie zombie;
    private final double speedModifier;
    private final double searchRange;
    private DollTarget target;

    public ZombieMoveToDollGoal(Zombie zombie, double speedModifier, double searchRange) {
        this.zombie = zombie;
        this.speedModifier = speedModifier;
        this.searchRange = searchRange;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!SoulboundDollsConfig.ENABLE_ATTRACT_UNDEAD.get()
                || !shouldSearchForDolls(ZombieDollCarryHelper.carriedDollCount(zombie), daylightCanBurn(zombie))) {
            return false;
        }

        target = nearestTarget(zombie.position(), findTargets()).orElse(null);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return SoulboundDollsConfig.ENABLE_ATTRACT_UNDEAD.get()
                && shouldSearchForDolls(ZombieDollCarryHelper.carriedDollCount(zombie), daylightCanBurn(zombie))
                && target != null
                && target.isValid()
                && target.position().distanceToSqr(zombie.position()) <= searchRange * searchRange;
    }

    @Override
    public void tick() {
        if (target == null || !target.isValid()) {
            return;
        }

        if (target.position().distanceToSqr(zombie.position()) <= PICKUP_DISTANCE_SQR) {
            if (target.tryConsume(zombie)) {
                zombie.getNavigation().stop();
            }
            target = null;
            return;
        }

        zombie.getNavigation().moveTo(target.entity(), speedModifier);
    }

    @Override
    public void stop() {
        target = null;
    }

    private List<DollTarget> findTargets() {
        AABB searchBox = zombie.getBoundingBox().inflate(searchRange);
        List<DollTarget> targets = new ArrayList<>();

        for (PlayerDollEntity doll : zombie.level().getEntitiesOfClass(PlayerDollEntity.class, searchBox)) {
            if (!doll.isRemoved()) {
                targets.add(DollTarget.placedDoll(doll));
            }
        }

        for (ItemEntity item : zombie.level().getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (!item.isRemoved() && DollStackHelper.isBoundPlayerDoll(item.getItem())) {
                targets.add(DollTarget.droppedDoll(item));
            }
        }

        return targets;
    }

    static Optional<DollTarget> nearestTarget(Vec3 origin, List<DollTarget> targets) {
        return targets.stream()
                .filter(DollTarget::isValid)
                .min(Comparator.comparingDouble(target -> target.position().distanceToSqr(origin)));
    }

    public static boolean shouldSearchForDolls(int carriedDollCount) {
        return carriedDollCount <= 0;
    }

    public static boolean shouldSearchForDolls(int carriedDollCount, boolean daylightCanBurn) {
        return daylightCanBurn && shouldSearchForDolls(carriedDollCount);
    }

    private static boolean daylightCanBurn(Zombie zombie) {
        Level level = zombie.level();
        return level.isDay()
                && !zombie.isInWaterRainOrBubble()
                && level.canSeeSky(zombie.blockPosition());
    }

    static final class DollTarget {
        private final Entity entity;
        private final Vec3 fallbackPosition;

        DollTarget(Vec3 position) {
            this.entity = null;
            this.fallbackPosition = position;
        }

        private DollTarget(Entity entity) {
            this.entity = entity;
            this.fallbackPosition = entity.position();
        }

        private static DollTarget placedDoll(PlayerDollEntity doll) {
            return new DollTarget(doll);
        }

        private static DollTarget droppedDoll(ItemEntity item) {
            return new DollTarget(item);
        }

        private Vec3 position() {
            return entity == null ? fallbackPosition : entity.position();
        }

        private Entity entity() {
            return entity;
        }

        private boolean isValid() {
            if (entity == null) {
                return true;
            }
            if (entity instanceof PlayerDollEntity) {
                return !entity.isRemoved();
            }
            if (entity instanceof ItemEntity item) {
                return !item.isRemoved() && DollStackHelper.isBoundPlayerDoll(item.getItem());
            }
            return false;
        }

        private boolean tryConsume(Zombie zombie) {
            if (entity instanceof PlayerDollEntity doll) {
                ItemStack dollStack = PlayerDollItem.createBoundDoll(doll.getProfile());
                if (!ZombieDollCarryHelper.tryAcceptDoll(zombie, dollStack)) {
                    return false;
                }
                doll.discard();
                return true;
            }

            if (entity instanceof ItemEntity item) {
                ItemStack stack = item.getItem();
                if (!DollStackHelper.isBoundPlayerDoll(stack) || !ZombieDollCarryHelper.tryAcceptDoll(zombie, stack)) {
                    return false;
                }
                if (stack.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(stack);
                }
                return true;
            }

            return false;
        }
    }
}
