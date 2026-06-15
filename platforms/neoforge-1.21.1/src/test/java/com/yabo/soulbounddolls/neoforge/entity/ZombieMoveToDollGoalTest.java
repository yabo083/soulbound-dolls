package com.yabo.soulbounddolls.neoforge.entity;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieMoveToDollGoalTest {
    @Test
    void choosesNearestAcceptableTarget() {
        ZombieMoveToDollGoal.DollTarget farther = new ZombieMoveToDollGoal.DollTarget(new Vec3(4.0D, 0.0D, 0.0D));
        ZombieMoveToDollGoal.DollTarget nearer = new ZombieMoveToDollGoal.DollTarget(new Vec3(1.0D, 0.0D, 0.0D));
        ZombieMoveToDollGoal.DollTarget sameDistanceLater = new ZombieMoveToDollGoal.DollTarget(new Vec3(0.0D, 0.0D, 1.0D));

        assertEquals(nearer, ZombieMoveToDollGoal.nearestTarget(Vec3.ZERO, List.of(farther, nearer, sameDistanceLater)).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoAcceptableTargetsExist() {
        assertTrue(ZombieMoveToDollGoal.nearestTarget(Vec3.ZERO, List.of()).isEmpty());
    }

    @Test
    void zombieOnlyChasesDollsBeforeCarryingAnyDoll() {
        assertTrue(ZombieMoveToDollGoal.shouldSearchForDolls(0));
        assertTrue(ZombieMoveToDollGoal.shouldSearchForDolls(-1));
        assertTrue(ZombieMoveToDollGoal.shouldSearchForDolls(Integer.MIN_VALUE));
    }

    @Test
    void zombieStopsChasingDollsAfterCarryingOne() {
        assertTrue(!ZombieMoveToDollGoal.shouldSearchForDolls(1));
        assertTrue(!ZombieMoveToDollGoal.shouldSearchForDolls(2));
        assertTrue(!ZombieMoveToDollGoal.shouldSearchForDolls(3));
    }
}
