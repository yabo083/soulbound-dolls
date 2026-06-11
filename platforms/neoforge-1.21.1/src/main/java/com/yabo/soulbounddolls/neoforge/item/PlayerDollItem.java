package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsEntities;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.TooltipFlag;

public final class PlayerDollItem extends Item {
    public PlayerDollItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createBoundDoll(PlayerDollProfile profile) {
        ItemStack stack = new ItemStack(SoulboundDollsItems.PLAYER_DOLL.get());
        stack.set(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get(), profile);
        return stack;
    }

    static Component boundDollName(PlayerDollProfile profile) {
        return Component.literal(profile.name() + "的玩偶");
    }

    @Override
    public Component getName(ItemStack stack) {
        PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
        if (profile == null) {
            return Component.translatable("item.soulbound_dolls.player_doll.unbound_name");
        }
        return boundDollName(profile);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());

        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (profile == null) {
            if (player == null) {
                return InteractionResult.PASS;
            }
            profile = DollSkinResolver.fromGameProfile(player.getGameProfile(), System.currentTimeMillis());
        }

        PlayerDollEntity doll = SoulboundDollsEntities.PLAYER_DOLL.get().create(serverLevel);
        if (doll == null) {
            return InteractionResult.FAIL;
        }

        BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spawnPos = Vec3.atBottomCenterOf(targetPos);
        doll.moveTo(spawnPos.x(), spawnPos.y(), spawnPos.z(), context.getRotation() + 180.0F, 0.0F);
        doll.setProfile(profile);

        if (player != null) {
            doll.setCreator(player);
        }

        serverLevel.addFreshEntity(doll);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
        if (profile == null) {
            tooltip.add(Component.translatable("item.soulbound_dolls.player_doll.tooltip.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("item.soulbound_dolls.player_doll.tooltip.bound", profile.name())
                .withStyle(ChatFormatting.GRAY));
    }
}
