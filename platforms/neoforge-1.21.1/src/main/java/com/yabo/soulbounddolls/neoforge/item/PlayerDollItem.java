package com.yabo.soulbounddolls.neoforge.item;

import com.yabo.soulbounddolls.common.PlayerDollProfile;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsComponents;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsConfig;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsEntities;
import com.yabo.soulbounddolls.neoforge.SoulboundDollsItems;
import com.yabo.soulbounddolls.neoforge.entity.DollProjectileEntity;
import com.yabo.soulbounddolls.neoforge.entity.PlayerDollEntity;
import com.yabo.soulbounddolls.neoforge.skin.DollSkinResolver;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.TooltipFlag;

public final class PlayerDollItem extends Item implements Equipable {
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

        if (SoulboundDollsConfig.ALLOW_DOLL_AS_HELMET.get()) {
            tooltip.add(Component.translatable("item.soulbound_dolls.player_doll.tooltip.equipable")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return SoulboundDollsConfig.ALLOW_DOLL_AS_HELMET.get() ? EquipmentSlot.HEAD : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // If throw feature is enabled and player is not sneaking, allow charging to throw
        if (SoulboundDollsConfig.ENABLE_THROW_DOLL.get() && !player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int ticksLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int chargeTicks = getUseDuration(stack, entity) - ticksLeft;
        if (chargeTicks < 10) {
            return; // Minimum charge time
        }

        PlayerDollProfile profile = stack.get(SoulboundDollsComponents.PLAYER_DOLL_PROFILE.get());
        if (profile == null) {
            profile = DollSkinResolver.fromGameProfile(player.getGameProfile(), System.currentTimeMillis());
        }

        if (!level.isClientSide) {
            // Create and throw the projectile
            DollProjectileEntity projectile = new DollProjectileEntity(
                    SoulboundDollsEntities.DOLL_PROJECTILE.get(),
                    player,
                    level,
                    profile
            );

            // Calculate velocity based on charge time (max 1.5 blocks/tick at 20 ticks charge)
            float power = Math.min(1.0F, chargeTicks / 20.0F);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F * power, 1.0F);

            level.addFreshEntity(projectile);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        player.getCooldowns().addCooldown(this, 20); // 1 second cooldown
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Maximum use duration (1 hour in ticks, effectively infinite for charging)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Use bow animation for charging
    }
}
