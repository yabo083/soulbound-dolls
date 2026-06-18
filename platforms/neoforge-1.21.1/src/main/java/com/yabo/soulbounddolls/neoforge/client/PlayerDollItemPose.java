package com.yabo.soulbounddolls.neoforge.client;

import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity.DollPose;
import java.util.function.Supplier;

final class PlayerDollItemPose {
    private PlayerDollItemPose() {
    }

    static DollPose fromComponent(Integer poseId) {
        return poseId == null ? DollPose.STANDING : DollPose.byId(poseId);
    }

    static DollPose fromHeadComponent(Integer poseId) {
        return poseId == null ? DollPose.SITTING : DollPose.byId(poseId);
    }

    static float headYOffset(DollPose pose) {
        return pose == DollPose.SITTING ? -0.08F : 0.0F;
    }

    static Offset headOffset(DollPose pose) {
        return headOffset(pose, Offset.ZERO);
    }

    static Offset headOffset(DollPose pose, Offset baseOffset) {
        return combineOffsets(baseOffset, poseOffset(pose));
    }

    static Offset poseOffset(DollPose pose) {
        return switch (pose) {
            case SITTING -> new Offset(0.0F, -0.08F, 0.0F);
            case STANDING, CUTE_IDLE -> Offset.ZERO;
        };
    }

    static Offset configOffset(Supplier<Double> x, Supplier<Double> y, Supplier<Double> z) {
        return new Offset(x.get().floatValue(), y.get().floatValue(), z.get().floatValue());
    }

    static Offset combineOffsets(Offset entityOffset, Offset poseOffset) {
        return new Offset(
                entityOffset.x() + poseOffset.x(),
                entityOffset.y() + poseOffset.y(),
                entityOffset.z() + poseOffset.z());
    }

    public record Offset(float x, float y, float z) {
        static final Offset ZERO = new Offset(0.0F, 0.0F, 0.0F);
    }
}
