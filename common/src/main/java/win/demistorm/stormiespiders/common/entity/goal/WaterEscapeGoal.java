package win.demistorm.stormiespiders.common.entity.goal;

import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class WaterEscapeGoal<T extends Mob & IClimberEntity> extends Goal {
	private final T spider;

	private Vec3 escapeTarget;
	private int struggleTickCounter;
	private int jitterInterval;
	private float struggleYawOffset;

	public WaterEscapeGoal(T spider) {
		this.spider = spider;
		this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if(!Config.COMMON.canSwim()) {
			return false;
		}
		return this.spider.isInWater();
	}

	@Override
	public boolean canContinueToUse() {
		return Config.COMMON.canSwim() && this.spider.isInWater();
	}

	@Override
	public void start() {
		this.spider.setTarget(null);
		this.spider.getNavigation().stop();
		this.spider.setEscapingWater(true);
		this.struggleTickCounter = 0;
		this.jitterInterval = 5 + this.spider.getRandom().nextInt(4);
		this.struggleYawOffset = 0;
		this.escapeTarget = this.findNearestShore();
		this.spider.setWaterEscapeTarget(this.escapeTarget);
	}

	@Override
	public void tick() {
		this.spider.setTarget(null);

		this.struggleTickCounter++;

		// Periodically update escape target
		if(this.struggleTickCounter % 20 == 0) {
			this.escapeTarget = this.findNearestShore();
			this.spider.setWaterEscapeTarget(this.escapeTarget);
		}

		// Struggling effect
		if(this.struggleTickCounter % this.jitterInterval == 0) {
			this.struggleYawOffset = (this.spider.getRandom().nextFloat() - 0.5f) * 20.0f;
			this.jitterInterval = 8 + this.spider.getRandom().nextInt(6);
		}

		// Face toward escape direction
		if(this.escapeTarget != null) {
			this.spider.getLookControl().setLookAt(this.escapeTarget.x, this.escapeTarget.y, this.escapeTarget.z);
			Vec3 currentPos = this.spider.position();
			double dx = this.escapeTarget.x - currentPos.x;
			double dz = this.escapeTarget.z - currentPos.z;
			float targetYaw = (float) Mth.atan2(-dx, dz) * (180.0f / (float) Math.PI);
			this.spider.setYRot(targetYaw + this.struggleYawOffset);
		} else {
			this.spider.setYRot(this.spider.getYRot() + this.struggleYawOffset * 0.1f);
		}

		this.spider.yBodyRot = this.spider.getYRot();
		this.spider.yHeadRot = this.spider.getYRot();
	}

	@Override
	public void stop() {
		this.spider.setEscapingWater(false);
		this.spider.setWaterEscapeTarget(null);
		this.escapeTarget = null;
		this.struggleYawOffset = 0;
	}

	@Nullable
	private Vec3 findNearestShore() {
		BlockPos spiderPos = this.spider.blockPosition();
		Level level = this.spider.level();

		// Find the water surface Y at spider position
		int surfaceY = spiderPos.getY();
		for(int y = spiderPos.getY() + 10; y >= spiderPos.getY() - 5; y--) {
			BlockPos pos = new BlockPos(spiderPos.getX(), y, spiderPos.getZ());
			BlockState state = level.getBlockState(pos);
			BlockState belowState = level.getBlockState(pos.below());
			if(!state.is(Blocks.WATER) && belowState.is(Blocks.WATER)) {
				surfaceY = y;
				break;
			}
		}

		// Search outward in expanding square for land at water surface level
		for(int radius = 1; radius <= 24; radius++) {
			for(int dx = -radius; dx <= radius; dx++) {
				for(int dz = -radius; dz <= radius; dz++) {
					// Only check perimeter of the square
					if(Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

					for(int dy = -1; dy <= 2; dy++) {
						BlockPos checkPos = new BlockPos(spiderPos.getX() + dx, surfaceY + dy, spiderPos.getZ() + dz);
						BlockState state = level.getBlockState(checkPos);
						BlockState belowState = level.getBlockState(checkPos.below());
						if(!state.is(Blocks.WATER) && !state.isAir() && belowState.isSolid()) {
							boolean adjacentToWater = false;
							for(BlockPos neighbor : new BlockPos[]{checkPos.north(), checkPos.south(), checkPos.east(), checkPos.west()}) {
								if(level.getBlockState(neighbor).is(Blocks.WATER)) {
									adjacentToWater = true;
									break;
								}
							}
							if(adjacentToWater) {
								return Vec3.atCenterOf(checkPos);
							}
						}
                        if(!state.is(Blocks.WATER) && !state.isSolid() && state.isAir() && belowState.isSolid()) {
							boolean adjacentToWater = false;
							for(BlockPos neighbor : new BlockPos[]{checkPos.north(), checkPos.south(), checkPos.east(), checkPos.west()}) {
								if(level.getBlockState(neighbor).is(Blocks.WATER)) {
									adjacentToWater = true;
									break;
								}
							}
							if(adjacentToWater) {
								return Vec3.atCenterOf(checkPos);
							}
						}
					}
				}
			}
		}

		// Fallback: Find the nearest non-water block in a wider search and aim toward it
		Vec3 bestDir = null;
		double bestDist = Double.MAX_VALUE;
		for(int radius = 1; radius <= 24; radius++) {
			for(int dx = -radius; dx <= radius; dx++) {
				for(int dz = -radius; dz <= radius; dz++) {
					if(Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

					for(int dy = -2; dy <= 3; dy++) {
						BlockPos checkPos = new BlockPos(spiderPos.getX() + dx, surfaceY + dy, spiderPos.getZ() + dz);
						BlockState state = level.getBlockState(checkPos);
						if(!state.is(Blocks.WATER) && state.isSolid()) {
							double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
							if(dist < bestDist) {
								bestDist = dist;
								bestDir = Vec3.atCenterOf(checkPos).subtract(this.spider.position()).normalize();
							}
						}
					}
				}
			}
			// Stop searching once a direction is found
			if(bestDir != null) {
				break;
			}
		}

		if(bestDir != null) {
			return this.spider.position().add(bestDir.scale(10));
		}

		// Just in case, aim above at the surface
		return new Vec3(this.spider.getX(), surfaceY + 1, this.spider.getZ());
	}
}
