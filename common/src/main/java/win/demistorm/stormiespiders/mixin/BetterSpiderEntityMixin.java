package win.demistorm.stormiespiders.mixin;

import win.demistorm.stormiespiders.config.RotationOverrideConfig;
import win.demistorm.stormiespiders.config.Config;
import win.demistorm.stormiespiders.config.NonClimbableBlocksConfig;
import win.demistorm.stormiespiders.common.ModTags;
import win.demistorm.stormiespiders.common.entity.goal.BetterLeapAtTargetGoal;
import win.demistorm.stormiespiders.common.entity.goal.WaterEscapeGoal;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.IMobEntityRegisterGoalsHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Predicate;

@Mixin(value = Spider.class, priority = 1001)
public abstract class BetterSpiderEntityMixin extends Monster implements IClimberEntity, IMobEntityRegisterGoalsHook {

	private static final UUID FOLLOW_RANGE_INCREASE_ID = UUID.fromString("9e815957-3a8e-4b65-afbc-eba39d2a06b4");
	private static final AttributeModifier FOLLOW_RANGE_INCREASE = new AttributeModifier(FOLLOW_RANGE_INCREASE_ID, "Follow range increase", 8.0D, AttributeModifier.Operation.ADDITION);

	private BetterSpiderEntityMixin(EntityType<? extends Monster> type, Level worldIn) {
		super(type, worldIn);
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(FOLLOW_RANGE_INCREASE);
	}

	@Override
	public void onRegisterGoals() {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		this.goalSelector.addGoal(1, new WaterEscapeGoal<>(this));
	}

	@Redirect(method = "registerGoals()V", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V"
			))
	private void onAddGoal(GoalSelector selector, int priority, Goal task) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			selector.addGoal(priority, task);
			return;
		}
		if(task instanceof LeapAtTargetGoal) {
			selector.addGoal(3, new BetterLeapAtTargetGoal<>(this, 0.4f));
		} else if(task instanceof TargetGoal) {
			selector.addGoal(2, ((TargetGoal) task).setUnseenMemoryTicks(200));
		} else {
			selector.addGoal(priority, task);
		}
	}

	@Override
	public boolean canClimbOnBlock(BlockState state, BlockPos pos) {
		// Check if block is in non-climbable config list
		if (NonClimbableBlocksConfig.isBlockNonClimbable(state)) {
			return false;
		}

		return !state.is(ModTags.NON_CLIMBABLE);
	}

	@Override
	public float getBlockSlipperiness(BlockPos pos) {
		BlockState offsetState = this.level().getBlockState(pos);

		float slipperiness = offsetState.getBlock().getFriction() * 0.91f;

		if(offsetState.is(ModTags.NON_CLIMBABLE)) {
			slipperiness = 1 - (1 - slipperiness) * 0.25f;
		}

		return slipperiness;
	}

	@Override
	public float getPathingMalus(BlockGetter cache, Mob entity, BlockPathTypes nodeType, BlockPos pos, Vec3i direction, Predicate<Direction> sides) {
		// Avoid water when not already in it
		if(!this.isInWater()) {
			if(nodeType == BlockPathTypes.WATER || nodeType == BlockPathTypes.WATER_BORDER) {
				return -1.0f;
			}
		}

		// Check all pathable surface blocks
		BlockPos.MutableBlockPos offsetPos = new BlockPos.MutableBlockPos();

		for(Direction offset : Direction.values()) {
			if(sides.test(offset)) {
				offsetPos.set(pos.getX() + offset.getStepX(), pos.getY() + offset.getStepY(), pos.getZ() + offset.getStepZ());
				BlockState surfaceState = cache.getBlockState(offsetPos);

				// If any surface block is non-climbable, reject this path
				if(!this.canClimbOnBlock(surfaceState, offsetPos)) {
					return -1.0f;
				}
			}
		}

		if(direction.getY() != 0) {
			// Prevent vertical climbing pathfinding during rain when config is enabled
			if(Config.COMMON.preventClimbingInRain() && this.level().isRaining() && this.level().isRainingAt(pos) &&
			   !sides.test(Direction.UP) && !sides.test(Direction.DOWN)) {
				return -1.0f;
			}

			// Already checked climbable neighbors above
		}

		return entity.getPathfindingMalus(nodeType);
	}
}
