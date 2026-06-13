package win.demistorm.stormiespiders.mixin;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import win.demistorm.stormiespiders.config.Config;
import win.demistorm.stormiespiders.config.NonClimbableBlocksConfig;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;
import win.demistorm.stormiespiders.common.CollisionSmoothingUtil;
import win.demistorm.stormiespiders.common.Matrix4f;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.IEntityMovementHook;
import win.demistorm.stormiespiders.common.entity.mob.IEntityReadWriteHook;
import win.demistorm.stormiespiders.common.entity.mob.ILivingEntityDataManagerHook;
import win.demistorm.stormiespiders.common.entity.mob.ILivingEntityJumpHook;
import win.demistorm.stormiespiders.common.entity.mob.ILivingEntityLookAtHook;
import win.demistorm.stormiespiders.common.entity.mob.IEntityRotationHook;
import win.demistorm.stormiespiders.common.entity.mob.ILivingEntityTravelHook;
import win.demistorm.stormiespiders.common.entity.mob.IMobEntityLivingTickHook;
import win.demistorm.stormiespiders.common.entity.mob.IMobEntityTickHook;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import win.demistorm.stormiespiders.common.entity.movement.BetterSpiderPathNavigator;
import win.demistorm.stormiespiders.common.entity.movement.ClimberJumpController;
import win.demistorm.stormiespiders.common.entity.movement.ClimberLookController;
import win.demistorm.stormiespiders.common.entity.movement.ClimberMoveController;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.resources.Identifier;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = { Spider.class })
public abstract class ClimberEntityMixin extends PathfinderMob implements IClimberEntity, IMobEntityLivingTickHook, ILivingEntityLookAtHook, IMobEntityTickHook, IEntityRotationHook, ILivingEntityDataManagerHook, ILivingEntityTravelHook, IEntityMovementHook, IEntityReadWriteHook, ILivingEntityJumpHook {

	// Copy from LivingEntity
	private static final UUID SLOW_FALLING_ID = UUID.fromString("A5B6CF2A-2F7C-31EF-9022-7C3E7D5E6ABA");
	private static final AttributeModifier SLOW_FALLING = new AttributeModifier(Identifier.fromNamespaceAndPath("stormiespiders", "slow_falling"), -0.07, AttributeModifier.Operation.ADD_VALUE);


	private static final EntityDataAccessor<Rotations> ROTATION_BODY;
	private static final EntityDataAccessor<Rotations> ROTATION_HEAD;
	private static final EntityDataAccessor<Rotations> ATTACHMENT_NORMAL;
	private static final EntityDataAccessor<Rotations> ATTACHMENT_OFFSET;

	static {
		@SuppressWarnings("unchecked")
		Class<Entity> cls = (Class<Entity>) MethodHandles.lookup().lookupClass();

		ROTATION_BODY = SynchedEntityData.defineId(cls, EntityDataSerializers.ROTATIONS);
		ROTATION_HEAD = SynchedEntityData.defineId(cls, EntityDataSerializers.ROTATIONS);
		ATTACHMENT_NORMAL = SynchedEntityData.defineId(cls, EntityDataSerializers.ROTATIONS);
		ATTACHMENT_OFFSET = SynchedEntityData.defineId(cls, EntityDataSerializers.ROTATIONS);
	}

	private double prevAttachmentOffsetX, prevAttachmentOffsetY, prevAttachmentOffsetZ;
	private double attachmentOffsetX, attachmentOffsetY, attachmentOffsetZ;

	private Vec3 attachmentNormal = new Vec3(0, 1, 0);
	private Vec3 prevAttachmentNormal = new Vec3(0, 1, 0);

	// Client-side smoothing targets (what server sent us)
	private Vec3 targetAttachmentNormal = new Vec3(0, 1, 0);
	private double targetAttachmentOffsetX, targetAttachmentOffsetY, targetAttachmentOffsetZ;

	// Client-side smoothed values (what we actually render)
	private Vec3 smoothedAttachmentNormal = new Vec3(0, 1, 0);
	private Vec3 prevSmoothedAttachmentNormal = new Vec3(0, 1, 0);
	private double smoothedOffsetX, smoothedOffsetY, smoothedOffsetZ;
	private double prevSmoothedOffsetX, prevSmoothedOffsetY, prevSmoothedOffsetZ;

	private float prevOrientationYawDelta;
	private float orientationYawDelta;

	private double lastAttachmentOffsetX, lastAttachmentOffsetY, lastAttachmentOffsetZ;
	private Vec3 lastAttachmentOrientationNormal = new Vec3(0, 1, 0);

	private int attachedTicks = 5;

	private Vec3 attachedSides = new Vec3(0, 0, 0);
	private Vec3 prevAttachedSides = new Vec3(0, 0, 0);

	private boolean canClimbInWater = false;
	private boolean canClimbInLava = false;


	private boolean isTravelingInFluid = false;

	private boolean isEscapingWater = false;
	private Vec3 waterEscapeTarget = null;

	private float collisionsInclusionRange = 2.0f;
	private float collisionsSmoothingRange = 1.25f;

	private Orientation orientation;
	private Pair<Direction, Vec3> groundDirection = Pair.of(Direction.DOWN, new Vec3(0, -1, 0));

	private Orientation renderOrientation;

	private float nextStepDistance, nextFlap;
	private Vec3 preWalkingPosition;

	private double preMoveY;

	private Vec3 jumpDir;

	private Vec3 lastStuckCheckPos = null;

	private boolean isJumping = false;

	private boolean clientTickedThisFrame = false;

	// Fallback attachment system for vanilla servers
	private boolean hasReceivedAttachmentData = false;
	private int vanillaServerDetectionTimer = 0;
	private boolean isUsingFallbackAttachment = false;
	private int fallbackAttachmentUpdateTimer = 0;

	private ClimberEntityMixin(EntityType<? extends PathfinderMob> type, Level worldIn) {
		super(type, worldIn);
	}

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		this.orientation = this.calculateOrientation(1);
		this.groundDirection = this.getGroundDirection();
		this.targetAttachmentNormal = new Vec3(0, 1, 0);
		this.smoothedAttachmentNormal = new Vec3(0, 1, 0);
		this.prevSmoothedAttachmentNormal = new Vec3(0, 1, 0);
		this.targetAttachmentOffsetY = 0.075;
		this.smoothedOffsetY = 0.075;
		this.prevSmoothedOffsetY = 0.075;

		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		this.moveControl = new ClimberMoveController<>(this);
		this.lookControl = new ClimberLookController<>(this);
		this.jumpControl = new ClimberJumpController<>(this);
	}

	@Inject(method = "createNavigation", at = @At("HEAD"), cancellable = true)
	private void onCreateNavigator(Level world, CallbackInfoReturnable<PathNavigation> ci) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		BetterSpiderPathNavigator<ClimberEntityMixin> navigate = new BetterSpiderPathNavigator<ClimberEntityMixin>(this, world, false);
		navigate.setCanFloat(true);
		ci.setReturnValue(navigate);
	}

	@Redirect(method = "defineSynchedData", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/syncher/SynchedEntityData$Builder;define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)Lnet/minecraft/network/syncher/SynchedEntityData$Builder;",
			ordinal = 0
	))
	public <T> SynchedEntityData.Builder onDefineData(SynchedEntityData.Builder builder, EntityDataAccessor<T> accessor, T value) {
		SynchedEntityData.Builder result = builder.define(accessor, value);

		builder.define(ROTATION_BODY, new Rotations(0, 0, 0));
		builder.define(ROTATION_HEAD, new Rotations(0, 0, 0));
		builder.define(ATTACHMENT_NORMAL, new Rotations(0, 1, 0));
		builder.define(ATTACHMENT_OFFSET, new Rotations(0, 0.075f, 0));

		return result;
	}

	@Override
	public void onWrite(ValueOutput output) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		output.putDouble("stormiespiders.AttachmentNormalX", this.attachmentNormal.x);
		output.putDouble("stormiespiders.AttachmentNormalY", this.attachmentNormal.y);
		output.putDouble("stormiespiders.AttachmentNormalZ", this.attachmentNormal.z);

		output.putInt("stormiespiders.AttachedTicks", this.attachedTicks);
	}

	@Override
	public void onRead(ValueInput input) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		this.prevAttachmentNormal = this.attachmentNormal = new Vec3(
				input.getDoubleOr("stormiespiders.AttachmentNormalX", 0.0),
				input.getDoubleOr("stormiespiders.AttachmentNormalY", 1.0),
				input.getDoubleOr("stormiespiders.AttachmentNormalZ", 0.0)
		);

		this.attachedTicks = input.getIntOr("stormiespiders.AttachedTicks", 5);

		this.orientation = this.calculateOrientation(1);

		// Sync smoothed values on load
		if (this.level().isClientSide()) {
			this.targetAttachmentNormal = this.attachmentNormal;
			this.smoothedAttachmentNormal = this.attachmentNormal;
			this.prevSmoothedAttachmentNormal = this.attachmentNormal;
		}
	}

	@Override
	public boolean canClimbInWater() {
		return this.canClimbInWater;
	}

	@Override
	public void setCanClimbInWater(boolean value) {
		this.canClimbInWater = value;
	}

	@Override
	public boolean canClimbInLava() {
		return this.canClimbInLava;
	}

	@Override
	public void setCanClimbInLava(boolean value) {
		this.canClimbInLava = value;
	}

	@Override
	public boolean isEscapingWater() {
		return this.isEscapingWater;
	}

	@Override
	public void setEscapingWater(boolean value) {
		this.isEscapingWater = value;
	}

	@Override
	public Vec3 getWaterEscapeTarget() {
		return this.waterEscapeTarget;
	}

	@Override
	public void setWaterEscapeTarget(Vec3 target) {
		this.waterEscapeTarget = target;
	}

	@Override
	public float getCollisionsInclusionRange() {
		return this.collisionsInclusionRange;
	}

	@Override
	public void setCollisionsInclusionRange(float range) {
		this.collisionsInclusionRange = range;
	}

	@Override
	public float getCollisionsSmoothingRange() {
		return this.collisionsSmoothingRange;
	}

	@Override
	public void setCollisionsSmoothingRange(float range) {
		this.collisionsSmoothingRange = range;
	}

	@Override
	public float getBridgePathingMalus(Mob entity, BlockPos pos, Node fallPathPoint) {
		return -1.0f;
	}

	@Override
	public void onPathingObstructed(Direction facing) {

	}

	@Override
	public int getMaxFallDistance() {
		return 0;
	}

	@Override
	public float getMovementSpeed() {
		AttributeInstance attribute = this.getAttribute(Attributes.MOVEMENT_SPEED); //MOVEMENT_SPEED
		return attribute != null ? (float) attribute.getValue() : 1.0f;
	}

	private static double calculateXOffset(AABB aabb, AABB other, double offsetX) {
		if(other.maxY > aabb.minY && other.minY < aabb.maxY && other.maxZ > aabb.minZ && other.minZ < aabb.maxZ) {
			if(offsetX > 0.0D && other.maxX <= aabb.minX) {
				double dx = aabb.minX - other.maxX;

				if(dx < offsetX) {
					offsetX = dx;
				}
			} else if(offsetX < 0.0D && other.minX >= aabb.maxX) {
				double dx = aabb.maxX - other.minX;

				if(dx > offsetX) {
					offsetX = dx;
				}
			}

			return offsetX;
		} else {
			return offsetX;
		}
	}

	private static double calculateYOffset(AABB aabb, AABB other, double offsetY) {
		if(other.maxX > aabb.minX && other.minX < aabb.maxX && other.maxZ > aabb.minZ && other.minZ < aabb.maxZ) {
			if(offsetY > 0.0D && other.maxY <= aabb.minY) {
				double dy = aabb.minY - other.maxY;

				if(dy < offsetY) {
					offsetY = dy;
				}
			} else if(offsetY < 0.0D && other.minY >= aabb.maxY) {
				double dy = aabb.maxY - other.minY;

				if(dy > offsetY) {
					offsetY = dy;
				}
			}

			return offsetY;
		} else {
			return offsetY;
		}
	}

	private static double calculateZOffset(AABB aabb, AABB other, double offsetZ) {
		if(other.maxX > aabb.minX && other.minX < aabb.maxX && other.maxY > aabb.minY && other.minY < aabb.maxY) {
			if(offsetZ > 0.0D && other.maxZ <= aabb.minZ) {
				double dz = aabb.minZ - other.maxZ;

				if(dz < offsetZ) {
					offsetZ = dz;
				}
			} else if(offsetZ < 0.0D && other.minZ >= aabb.maxZ) {
				double dz = aabb.maxZ - other.minZ;

				if(dz > offsetZ) {
					offsetZ = dz;
				}
			}

			return offsetZ;
		} else {
			return offsetZ;
		}
	}

	private void updateWalkingSide() {
		Direction avoidPathingFacing = null;

		AABB entityBox = this.getBoundingBox();

		double closestFacingDst = Double.MAX_VALUE;
		Direction closestFacing = null;

		Vec3 weighting = new Vec3(0, 0, 0);

		float stickingDistance = this.zza != 0 ? 1.5f : 0.1f;

		for(Direction facing : Direction.values()) {
			if(avoidPathingFacing == facing) {
				continue;
			}

			List<AABB> collisionBoxes = this.getCollisionBoxes(entityBox.inflate(0.2f).expandTowards(facing.getStepX() * stickingDistance, facing.getStepY() * stickingDistance, facing.getStepZ() * stickingDistance));

			double closestDst = Double.MAX_VALUE;

			for(AABB collisionBox : collisionBoxes) {
				switch(facing) {
				case EAST:
				case WEST:
					closestDst = Math.min(closestDst, Math.abs(calculateXOffset(entityBox, collisionBox, -facing.getStepX() * stickingDistance)));
					break;
				case UP:
				case DOWN:
					closestDst = Math.min(closestDst, Math.abs(calculateYOffset(entityBox, collisionBox, -facing.getStepY() * stickingDistance)));
					break;
				case NORTH:
				case SOUTH:
					closestDst = Math.min(closestDst, Math.abs(calculateZOffset(entityBox, collisionBox, -facing.getStepZ() * stickingDistance)));
					break;
				}
			}

			if(closestDst < closestFacingDst) {
				closestFacingDst = closestDst;
				closestFacing = facing;
			}

			if(closestDst < Double.MAX_VALUE) {
				weighting = weighting.add(new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(1 - Math.min(closestDst, stickingDistance) / stickingDistance));
			}
		}

		if(closestFacing == null) {
			this.groundDirection = Pair.of(Direction.DOWN, new Vec3(0, -1, 0));
		} else {
			this.groundDirection = Pair.of(closestFacing, weighting.normalize().add(0, -0.001f, 0).normalize());
		}
	}

	@Override
	public Pair<Direction, Vec3> getGroundDirection() {
		return this.groundDirection;
	}

	@Override
	public Direction getGroundSide() {
		return this.groundDirection.getKey();
	}

	@Override
	public Orientation getOrientation() {
		return this.orientation;
	}

	@Override
	public void setRenderOrientation(Orientation orientation) {
		this.renderOrientation = orientation;
	}

	@Override
	public Orientation getRenderOrientation() {
		return this.renderOrientation;
	}

	@Override
	public float getAttachmentOffset(Direction.Axis axis, float partialTicks) {
		if (this.level().isClientSide()) {
			// Client: interpolate between previous and current smoothed values
			switch (axis) {
				default:
				case X:
					return (float) Mth.lerp(partialTicks, this.prevSmoothedOffsetX, this.smoothedOffsetX);
				case Y:
					return (float) Mth.lerp(partialTicks, this.prevSmoothedOffsetY, this.smoothedOffsetY);
				case Z:
					return (float) Mth.lerp(partialTicks, this.prevSmoothedOffsetZ, this.smoothedOffsetZ);
			}
		} else {
			// Server: use raw values
			switch (axis) {
				default:
				case X:
					return (float) Mth.lerp(partialTicks, this.prevAttachmentOffsetX, this.attachmentOffsetX);
				case Y:
					return (float) Mth.lerp(partialTicks, this.prevAttachmentOffsetY, this.attachmentOffsetY);
				case Z:
					return (float) Mth.lerp(partialTicks, this.prevAttachmentOffsetZ, this.attachmentOffsetZ);
			}
		}
	}

	@Override
	public Vec3 onLookAt(Anchor anchor, Vec3 vec) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return vec;
		}
		Vec3 dir = vec.subtract(this.position());
		dir = this.getOrientation().getLocal(dir);
		return dir;
	}

	@Override
	public void onTick() {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		if(!this.level().isClientSide() && this.level() instanceof ServerLevel) {
			ChunkMap.TrackedEntity entityTracker = ((ServerLevel) this.level()).getChunkSource().chunkMap.entityMap.get(this.getId());

			if(entityTracker != null && !Config.COMMON.disableDataSync()) {
				// Sync attachment data only if data sync is enabled
				this.entityData.set(ATTACHMENT_NORMAL, new Rotations(
						(float) this.attachmentNormal.x,
						(float) this.attachmentNormal.y,
						(float) this.attachmentNormal.z
				));

				this.entityData.set(ATTACHMENT_OFFSET, new Rotations(
						(float) this.attachmentOffsetX,
						(float) this.attachmentOffsetY,
						(float) this.attachmentOffsetZ
				));
			}
		}
	}

	@Override
	public void onLivingTick() {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		// Client-side smoothing for interpolation and fallback generation
		if (this.level().isClientSide()) {
			// Check for vanilla server after initial connection
			if (!this.hasReceivedAttachmentData) {
				this.vanillaServerDetectionTimer++;
				if (this.vanillaServerDetectionTimer > 50) { // 2.5s
					// No server attachment data received, assume vanilla server
					this.isUsingFallbackAttachment = true;
				}
			}

			// Update fallback attachment data
			this.updateFallbackAttachmentData();

			// Store previous smoothed values for sub-tick interpolation
			this.prevSmoothedOffsetX = this.smoothedOffsetX;
			this.prevSmoothedOffsetY = this.smoothedOffsetY;
			this.prevSmoothedOffsetZ = this.smoothedOffsetZ;
			this.prevSmoothedAttachmentNormal = this.smoothedAttachmentNormal;

			// Chase the target values
			float smoothFactor = 0.5f;

			this.smoothedOffsetX = Mth.lerp(smoothFactor, this.smoothedOffsetX, this.targetAttachmentOffsetX);
			this.smoothedOffsetY = Mth.lerp(smoothFactor, this.smoothedOffsetY, this.targetAttachmentOffsetY);
			this.smoothedOffsetZ = Mth.lerp(smoothFactor, this.smoothedOffsetZ, this.targetAttachmentOffsetZ);

			this.smoothedAttachmentNormal = new Vec3(
					Mth.lerp(smoothFactor, this.smoothedAttachmentNormal.x, this.targetAttachmentNormal.x),
					Mth.lerp(smoothFactor, this.smoothedAttachmentNormal.y, this.targetAttachmentNormal.y),
					Mth.lerp(smoothFactor, this.smoothedAttachmentNormal.z, this.targetAttachmentNormal.z)
			).normalize();
		}

		this.updateWalkingSide();
	}

	@Override
	public boolean onClimbable() {
		return this.horizontalCollision;
	}

	@Override
	public float getVerticalOffset(float partialTicks) {
		return 0.075f;
	}

	private void forEachCollisonBox(AABB aabb, Shapes.DoubleLineConsumer action) {
		int minChunkX = ((Mth.floor(aabb.minX - 1.0E-7D) - 1) >> 4);
		int maxChunkX = ((Mth.floor(aabb.maxX + 1.0E-7D) + 1) >> 4);
		int minChunkZ = ((Mth.floor(aabb.minZ - 1.0E-7D) - 1) >> 4);
		int maxChunkZ = ((Mth.floor(aabb.maxZ + 1.0E-7D) + 1) >> 4);

		int width = maxChunkX - minChunkX + 1;
		int depth = maxChunkZ - minChunkZ + 1;

		BlockGetter[] blockReaderCache = new BlockGetter[width * depth];

		CollisionGetter collisionReader = this.level();

		for(int cx = minChunkX; cx <= maxChunkX; cx++) {
			for(int cz = minChunkZ; cz <= maxChunkZ; cz++) {
				blockReaderCache[(cx - minChunkX) + (cz - minChunkZ) * width] = collisionReader.getChunkForCollisions(cx, cz);
			}
		}

		CollisionGetter cachedCollisionReader = new CollisionGetter() {
			@Override
			public int getHeight() {
				return level().getHeight();
			}

			public int getMinY() {
				return level().getMinY();
			}

			@Override
			public BlockEntity getBlockEntity(BlockPos pos) {
				return collisionReader.getBlockEntity(pos);
			}

			@Override
			public BlockState getBlockState(BlockPos pos) {
				return collisionReader.getBlockState(pos);
			}

			@Override
			public FluidState getFluidState(BlockPos pos) {
				return collisionReader.getFluidState(pos);
			}

			@Override
			public WorldBorder getWorldBorder() {
				return collisionReader.getWorldBorder();
			}



			@Override
			public List<VoxelShape> getEntityCollisions(Entity entity, AABB aabb) {
				return collisionReader.getEntityCollisions(entity, aabb);
			}

			@Override
			public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
				return blockReaderCache[(chunkX - minChunkX) + (chunkZ - minChunkZ) * width];
			}
		};

		Iterable<VoxelShape> shapes =  cachedCollisionReader.getBlockCollisions(this,aabb);
		shapes.forEach(shape -> shape.forAllBoxes(action));
	}

	private List<AABB> getCollisionBoxes(AABB aabb) {
		List<AABB> boxes = new ArrayList<>();
		this.forEachCollisonBox(aabb, (minX, minY, minZ, maxX, maxY, maxZ) -> boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ)));
		return boxes;
	}

	@Override
	public boolean canClimbOnBlock(BlockState state, BlockPos pos) {
		// Check if block is in non-climbable config list
		if (NonClimbableBlocksConfig.isBlockNonClimbable(state)) {
			return false;
		}

		// Prevent climbing on blocks during rain when config is enabled
		if(Config.COMMON.preventClimbingInRain() && this.level().isRaining() && this.level().isRainingAt(pos)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canAttachToSide(Direction side) {
		if(!this.isJumping && Config.COMMON.preventClimbingInRain() &&
		   side.getAxis() != Direction.Axis.Y &&
		   this.level().isRainingAt(new BlockPos((int)this.getX(), (int)(this.getY() + this.getBbHeight() * 0.5f), (int)this.getZ()))) {
			return false;
		}
		return true;
	}

	@Override
	public float getBlockSlipperiness(BlockPos pos) {
		BlockState offsetState = this.level().getBlockState(pos);
		return offsetState.getBlock().getFriction() * 0.91f;
	}

	private void updateOffsetsAndOrientation() {
		Vec3 direction = this.getOrientation().getGlobal(this.getYRot(), this.getXRot());

		boolean isAttached = false;

		double baseStickingOffsetX = 0.0f;
		double baseStickingOffsetY = this.getVerticalOffset(1);
		double baseStickingOffsetZ = 0.0f;
		Vec3 baseOrientationNormal = new Vec3(0, 1, 0);


		// Calculate attachments on server
		if (!this.level().isClientSide()) {
			// Prevent climbing attachment during rain when config is enabled
			if (Config.COMMON.preventClimbingInRain() && this.level().isRaining() &&
					this.level().isRainingAt(new BlockPos((int) this.getX(), (int) (this.getY() + this.getBbHeight() * 0.5f), (int) this.getZ()))) {
				// Skip attachment logic entirely during rain
				isAttached = false;
			} else if (!this.isTravelingInFluid && this.onGround() && this.getVehicle() == null) {
				Vec3 p = this.position();

				Vec3 s = p.add(0, this.getBbHeight() * 0.5f, 0);
				AABB inclusionBox = new AABB(s.x, s.y, s.z, s.x, s.y, s.z).inflate(this.collisionsInclusionRange);

				Pair<Vec3, Vec3> attachmentPoint = CollisionSmoothingUtil.findClosestPoint(consumer -> this.forEachCollisonBox(inclusionBox, consumer), s, this.attachmentNormal.scale(-1), this.collisionsSmoothingRange, 1.0f, 0.001f, 20, 0.05f, s);

				AABB entityBox = this.getBoundingBox();

				if (attachmentPoint != null) {
					Vec3 attachmentPos = attachmentPoint.getLeft();

					double dx = Math.max(entityBox.minX - attachmentPos.x, attachmentPos.x - entityBox.maxX);
					double dy = Math.max(entityBox.minY - attachmentPos.y, attachmentPos.y - entityBox.maxY);
					double dz = Math.max(entityBox.minZ - attachmentPos.z, attachmentPos.z - entityBox.maxZ);

					if (Math.max(dx, Math.max(dy, dz)) < 0.5f) {
						isAttached = true;

						this.lastAttachmentOffsetX = Mth.clamp(attachmentPos.x - p.x, -this.getBbWidth() / 2, this.getBbWidth() / 2);
						this.lastAttachmentOffsetY = Mth.clamp(attachmentPos.y - p.y, 0, this.getBbHeight());
						this.lastAttachmentOffsetZ = Mth.clamp(attachmentPos.z - p.z, -this.getBbWidth() / 2, this.getBbWidth() / 2);
						this.lastAttachmentOrientationNormal = attachmentPoint.getRight();
					}
				}
			}

			this.prevAttachmentOffsetX = this.attachmentOffsetX;
			this.prevAttachmentOffsetY = this.attachmentOffsetY;
			this.prevAttachmentOffsetZ = this.attachmentOffsetZ;
			this.prevAttachmentNormal = this.attachmentNormal;

			float attachmentBlend = this.attachedTicks * 0.2f;

			this.attachmentOffsetX = baseStickingOffsetX + (this.lastAttachmentOffsetX - baseStickingOffsetX) * attachmentBlend;
			this.attachmentOffsetY = baseStickingOffsetY + (this.lastAttachmentOffsetY - baseStickingOffsetY) * attachmentBlend;
			this.attachmentOffsetZ = baseStickingOffsetZ + (this.lastAttachmentOffsetZ - baseStickingOffsetZ) * attachmentBlend;
			this.attachmentNormal = baseOrientationNormal.add(this.lastAttachmentOrientationNormal.subtract(baseOrientationNormal).scale(attachmentBlend)).normalize();

			if (!isAttached) {
				this.attachedTicks = Math.max(0, this.attachedTicks - 1);
			} else {
				this.attachedTicks = Math.min(5, this.attachedTicks + 1);
			}
		}

		this.orientation = this.calculateOrientation(1);

		// Apply rotations only on server
		if (!this.level().isClientSide()) {
			Pair<Float, Float> newRotations = this.getOrientation().getLocalRotation(direction);

			float yawDelta = newRotations.getLeft() - this.getYRot();
			float pitchDelta = newRotations.getRight() - this.getXRot();

			this.prevOrientationYawDelta = this.orientationYawDelta;
			this.orientationYawDelta = yawDelta;

			this.setYRot(Mth.wrapDegrees(this.getYRot() + yawDelta));
			this.setXRot(Mth.wrapDegrees(this.getXRot() + pitchDelta));
			this.yBodyRot = Mth.wrapDegrees(this.yBodyRot + yawDelta);
			this.yHeadRot = Mth.wrapDegrees(this.yHeadRot + yawDelta);
		}
	}

	// Client-side fallback attachment generation for vanilla servers
	private void updateFallbackAttachmentData() {
		if (!this.level().isClientSide() || !this.isUsingFallbackAttachment || !Config.COMMON.enableFallbackRotation()) {
			return;
		}

		// Update fallback attachment
		this.fallbackAttachmentUpdateTimer++;
		if (this.fallbackAttachmentUpdateTimer < Config.COMMON.fallbackUpdateInterval()) {
			return;
		}
		this.fallbackAttachmentUpdateTimer = 0;

		Vec3 p = this.position();
		Vec3 s = p.add(0, this.getBbHeight() * 0.5f, 0);
		AABB inclusionBox = new AABB(s.x, s.y, s.z, s.x, s.y, s.z).inflate(this.collisionsInclusionRange);

		Pair<Vec3, Vec3> attachmentPoint = CollisionSmoothingUtil.findClosestPoint(
				consumer -> this.forEachCollisonBox(inclusionBox, consumer),
				s,
				this.smoothedAttachmentNormal.scale(-1),
				this.collisionsSmoothingRange,
				1.0f,
				0.001f,
				20,
				0.05f,
				s
		);

		AABB entityBox = this.getBoundingBox();

		if (attachmentPoint != null) {
			Vec3 attachmentPos = attachmentPoint.getLeft();

			double dx = Math.max(entityBox.minX - attachmentPos.x, attachmentPos.x - entityBox.maxX);
			double dy = Math.max(entityBox.minY - attachmentPos.y, attachmentPos.y - entityBox.maxY);
			double dz = Math.max(entityBox.minZ - attachmentPos.z, attachmentPos.z - entityBox.maxZ);

			if (Math.max(dx, Math.max(dy, dz)) < 0.5f) {
				// Set fallback attachment data
				this.targetAttachmentOffsetX = Mth.clamp(attachmentPos.x - p.x, -this.getBbWidth() / 2, this.getBbWidth() / 2);
				this.targetAttachmentOffsetY = Mth.clamp(attachmentPos.y - p.y, 0, this.getBbHeight());
				this.targetAttachmentOffsetZ = Mth.clamp(attachmentPos.z - p.z, -this.getBbWidth() / 2, this.getBbWidth() / 2);
				this.targetAttachmentNormal = attachmentPoint.getRight();
			}
		}
	}

	private float wrapAngleInRange(float angle, float target) {
		while(target - angle < -180.0F) {
			angle -= 360.0F;
		}

		while(target - angle >= 180.0F) {
			angle += 360.0F;
		}

		return angle;
	}

	@Override
	public Orientation calculateOrientation(float partialTicks) {
		Vec3 attachmentNormal;

		if (this.level().isClientSide()) {
			// Client: use smoothed and interpolated normal
			attachmentNormal = new Vec3(
					Mth.lerp(partialTicks, this.prevSmoothedAttachmentNormal.x, this.smoothedAttachmentNormal.x),
					Mth.lerp(partialTicks, this.prevSmoothedAttachmentNormal.y, this.smoothedAttachmentNormal.y),
					Mth.lerp(partialTicks, this.prevSmoothedAttachmentNormal.z, this.smoothedAttachmentNormal.z)
			).normalize();
		} else {
			// Server: use raw interpolated values
			attachmentNormal = new Vec3(
					Mth.lerp(partialTicks, this.prevAttachmentNormal.x, this.attachmentNormal.x),
					Mth.lerp(partialTicks, this.prevAttachmentNormal.y, this.attachmentNormal.y),
					Mth.lerp(partialTicks, this.prevAttachmentNormal.z, this.attachmentNormal.z)
			).normalize();
		}

		Vec3 localZ = new Vec3(0, 0, 1);
		Vec3 localY = new Vec3(0, 1, 0);
		Vec3 localX = new Vec3(1, 0, 0);

		float componentZ = (float) localZ.dot(attachmentNormal);
		float componentY;
		float componentX = (float) localX.dot(attachmentNormal);

		float yaw = (float) Math.toDegrees(Mth.atan2(componentX, componentZ));

		localZ = new Vec3(Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
		localY = new Vec3(0, 1, 0);
		localX = new Vec3(Math.sin(Math.toRadians(yaw - 90)), 0, Math.cos(Math.toRadians(yaw - 90)));

		componentZ = (float) localZ.dot(attachmentNormal);
		componentY = (float) localY.dot(attachmentNormal);
		componentX = (float) localX.dot(attachmentNormal);

		float pitch = (float) Math.toDegrees(Mth.atan2(Mth.sqrt(componentX * componentX + componentZ * componentZ), componentY));

		Matrix4f m = new Matrix4f();
		m.multiply(new Matrix4f((float) Math.toRadians(yaw), 0, 1, 0));
		m.multiply(new Matrix4f((float) Math.toRadians(pitch), 1, 0, 0));
		m.multiply(new Matrix4f((float) Math.toRadians((float) Math.signum(0.5f - componentY - componentZ - componentX) * yaw), 0, 1, 0));

		localZ = m.multiply(new Vec3(0, 0, -1));
		localY = m.multiply(new Vec3(0, 1, 0));
		localX = m.multiply(new Vec3(1, 0, 0));

		return new Orientation(attachmentNormal, localZ, localY, localX, componentZ, componentY, componentX, yaw, pitch);
	}

	@Override
	public float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
		// Return the entity's current yaw - the interpolation system will handle smooth transitions
		return this.yRot;
	}

	@Override
	public float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
		// Return the entity's current pitch - the interpolation system will handle smooth transitions
		return this.xRot;
	}

	@Override
	public float getTargetHeadYaw(float yaw, int rotationIncrements) {
		return (float) this.lerpYHeadRot;
	}

	@Override
	public void onNotifyDataManagerChange(EntityDataAccessor<?> key) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return;
		}
		if (ATTACHMENT_NORMAL.equals(key)) {
			Rotations normal = this.entityData.get(ATTACHMENT_NORMAL);
			Vec3 newNormal = new Vec3(normal.x(), normal.y(), normal.z());

			if (this.level().isClientSide()) {
				// Mark received server attachment data
				this.hasReceivedAttachmentData = true;
				this.isUsingFallbackAttachment = false;

				// Client: set target, smoothly chase it
				this.targetAttachmentNormal = newNormal;
			} else {
				// Server: direct assignment
				this.prevAttachmentNormal = this.attachmentNormal;
				this.attachmentNormal = newNormal;
			}
		} else if (ATTACHMENT_OFFSET.equals(key)) {
			Rotations offset = this.entityData.get(ATTACHMENT_OFFSET);

			if (this.level().isClientSide()) {
				// Mark received server attachment data
				this.hasReceivedAttachmentData = true;
				this.isUsingFallbackAttachment = false;

				// Client: set target
				this.targetAttachmentOffsetX = offset.x();
				this.targetAttachmentOffsetY = offset.y();
				this.targetAttachmentOffsetZ = offset.z();
			} else {
				// Server: direct assignment
				this.prevAttachmentOffsetX = this.attachmentOffsetX;
				this.prevAttachmentOffsetY = this.attachmentOffsetY;
				this.prevAttachmentOffsetZ = this.attachmentOffsetZ;
				this.attachmentOffsetX = offset.x();
				this.attachmentOffsetY = offset.y();
				this.attachmentOffsetZ = offset.z();
			}
		}
	}

	private double getClimberGravity() {
		if(this.isNoGravity()) {
			return 0;
		}

		double gravity = 0.08d;

		boolean isFalling = this.getDeltaMovement().y <= 0.0D;

		if(isFalling && this.hasEffect(MobEffects.SLOW_FALLING)) {
			gravity = 0.1D;
		}

		return gravity;
	}

	private Vec3 getStickingForce(Pair<Direction, Vec3> walkingSide) {
		double uprightness = Math.max(this.attachmentNormal.y, 0);
		double gravity = this.getClimberGravity();
		double stickingForce = gravity * uprightness + 0.08D * (1 - uprightness);
		return walkingSide.getRight().scale(stickingForce);
	}

	@Override
	public void setJumpDirection(Vec3 dir) {
		this.jumpDir = dir != null ? dir.normalize() : null;
	}

	public void setJumping(boolean jumping) {
		this.isJumping = jumping;
	}

	@Override
	public boolean onJump() {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return false;
		}
		this.isJumping = true;

		if(this.jumpDir != null) {
			float jumpStrength = this.getJumpPower();
			if(this.hasEffect(MobEffects.JUMP_BOOST)) {
				jumpStrength += 0.1F * (float)(this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1);
			}

			Vec3 motion = this.getDeltaMovement();

			Vec3 orthogonalMotion = this.jumpDir.scale(this.jumpDir.dot(motion));
			Vec3 tangentialMotion = motion.subtract(orthogonalMotion);

			this.setDeltaMovement(tangentialMotion.x + this.jumpDir.x * jumpStrength, tangentialMotion.y + this.jumpDir.y * jumpStrength, tangentialMotion.z + this.jumpDir.z * jumpStrength);

			if(this.isSprinting()) {
				Vec3 boost = this.getOrientation().getGlobal(this.yRot, 0).scale(0.2f);
				this.setDeltaMovement(this.getDeltaMovement().add(boost));
			}

			this.needsSync = true;

			return true;
		}

		return false;
	}

	@Override
	public boolean onTravel(Vec3 relative, boolean pre) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return false;
		}
		if(pre) {
			boolean canTravel = this.isEffectiveAi() || this.isLocalClientAuthoritative();

			this.isTravelingInFluid = false;

			FluidState fluidState = this.level().getFluidState(this.blockPosition());

			if(!this.canClimbInWater && this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
				this.isTravelingInFluid = true;

				if(!Config.COMMON.canSwim()) {
					return false;
				}

				if(canTravel) {
					if(this.isEscapingWater && Config.COMMON.canSwim()) {
						this.travelWaterEscape();
						this.updateWaterEscapeOrientation();
					} else {
						Vec3 motion = this.getDeltaMovement();
						motion = motion.add(0, 0.01, 0);
						motion = motion.add(
							(this.random.nextFloat() - 0.5) * 0.01,
							0,
							(this.random.nextFloat() - 0.5) * 0.01
						);
						motion = motion.scale(0.8);
						this.setDeltaMovement(motion);
						this.move(MoverType.SELF, motion);
						this.calculateEntityAnimation(true);
					}
					return true;
				}
			} else if(!this.canClimbInLava && this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
				this.isTravelingInFluid = true;

				if(canTravel) {
					Vec3 motion = this.getDeltaMovement();
					motion = motion.add(0, 0.01, 0);
					motion = motion.scale(0.8);
					this.setDeltaMovement(motion);
					this.move(MoverType.SELF, motion);
					this.calculateEntityAnimation(true);
					return true;
				}
			} else if(canTravel) {
				this.travelOnGround(relative);
			}

			if(!canTravel) {
				this.calculateEntityAnimation( true);
			}


			this.updateOffsetsAndOrientation();
			return true;
		} else {
			this.updateOffsetsAndOrientation();
			return false;
		}
	}

	private void updateWaterEscapeOrientation() {
		this.prevAttachmentOffsetX = this.attachmentOffsetX;
		this.prevAttachmentOffsetY = this.attachmentOffsetY;
		this.prevAttachmentOffsetZ = this.attachmentOffsetZ;
		this.prevAttachmentNormal = this.attachmentNormal;

		Vec3 movement = this.getDeltaMovement();
		double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

		Vec3 waterNormal;
		if(horizontalSpeed > 0.001D) {
			Vec3 horizontalDir = new Vec3(movement.x, 0, movement.z).normalize();
			waterNormal = new Vec3(0, 1, 0).add(horizontalDir.scale(-0.35)).normalize();
		} else {
			waterNormal = new Vec3(0, 1, 0);
		}

		float blendSpeed = 0.15f;
		this.attachmentNormal = this.attachmentNormal.add(waterNormal.subtract(this.attachmentNormal).scale(blendSpeed)).normalize();

		this.attachmentOffsetX = 0;
		this.attachmentOffsetY = this.getVerticalOffset(1);
		this.attachmentOffsetZ = 0;

		this.attachedTicks = 0;

		this.orientation = this.calculateOrientation(1);
	}

	private void travelWaterEscape() {
		Vec3 movement = this.getDeltaMovement();

		if(this.isUnderWater()) {
			movement = movement.add(0, 0.025, 0);
		} else {
			if(movement.y < 0) {
				movement = new Vec3(movement.x, movement.y * 0.5, movement.z);
			}
			movement = movement.add(0, 0.01, 0);
		}

		Vec3 escapeTarget = this.getWaterEscapeTarget();
		if(escapeTarget != null) {
			Vec3 toShore = escapeTarget.subtract(this.position());
			Vec3 horizontalDir = new Vec3(toShore.x, 0, toShore.z);
			double horizontalDist = horizontalDir.length();
			if(horizontalDist > 0.1D) {
				horizontalDir = horizontalDir.normalize();
				double lateralSpeed = 0.02;
				movement = movement.add(horizontalDir.scale(lateralSpeed));
			}
		} else {
			double driftX = (this.random.nextFloat() - 0.5) * 0.02;
			double driftZ = (this.random.nextFloat() - 0.5) * 0.02;
			movement = movement.add(driftX, 0, driftZ);
		}

		movement = movement.scale(0.8);

		this.setDeltaMovement(movement);
		this.move(MoverType.SELF, movement);

		this.calculateEntityAnimation(true);
	}

	private float getRelevantMoveFactor(float slipperiness) {
		return this.onGround()? this.getSpeed() * (0.16277136F / (slipperiness * slipperiness * slipperiness)) : this.getFlyingSpeed();
	}

	private void travelOnGround(Vec3 relative) {
		Orientation orientation = this.getOrientation();

		Vec3 forwardVector = orientation.getGlobal(this.yRot, 0);
		Vec3 strafeVector = orientation.getGlobal(this.yRot + 90.0f, 0);
		Vec3 upVector = orientation.getGlobal(this.yRot, -90.0f);

		Pair<Direction, Vec3> groundDirection = this.getGroundDirection();

		Vec3 stickingForce = this.getStickingForce(groundDirection);

		boolean isFalling = this.getDeltaMovement().y <= 0.0D;

		if (isFalling && this.hasEffect(MobEffects.SLOW_FALLING)) {
			this.fallDistance = 0;
		}

		float forward = (float) relative.z;
		float strafe = (float) relative.x;

		if (forward != 0 || strafe != 0) {
			float slipperiness = 0.91f;

			if (this.onGround()) {
				BlockPos offsetPos = new BlockPos(this.blockPosition()).relative(groundDirection.getLeft());
				slipperiness = this.getBlockSlipperiness(offsetPos);
			}

			float f = forward * forward + strafe * strafe;
			if (f >= 1.0E-4F) {
				f = Math.max(Mth.sqrt(f), 1.0f);
				f = this.getRelevantMoveFactor(slipperiness) / f;
				forward *= f;
				strafe *= f;

				Vec3 movementOffset = new Vec3(forwardVector.x * forward + strafeVector.x * strafe, forwardVector.y * forward + strafeVector.y * strafe, forwardVector.z * forward + strafeVector.z * strafe);

				double px = this.getX();
				double py = this.getY();
				double pz = this.getZ();
				Vec3 motion = this.getDeltaMovement();
				AABB aabb = this.getBoundingBox();

				// Check movement vector
				this.move(MoverType.SELF, movementOffset);

				Vec3 movementDir = new Vec3(this.getX() - px, this.getY() - py, this.getZ() - pz).normalize();

				this.setBoundingBox(aabb);
				this.setLocationFromBoundingbox();
				this.setDeltaMovement(motion);

				// Check collision normal
				Vec3 probeVector = new Vec3(Math.abs(movementDir.x) < 0.001D ? -Math.signum(upVector.x) : 0, Math.abs(movementDir.y) < 0.001D ? -Math.signum(upVector.y) : 0, Math.abs(movementDir.z) < 0.001D ? -Math.signum(upVector.z) : 0).normalize().scale(0.0001D);
				this.move(MoverType.SELF, probeVector);

				Vec3 collisionNormal = new Vec3(Math.abs(this.getX() - px - probeVector.x) > 0.000001D ? Math.signum(-probeVector.x) : 0, Math.abs(this.getY() - py - probeVector.y) > 0.000001D ? Math.signum(-probeVector.y) : 0, Math.abs(this.getZ() - pz - probeVector.z) > 0.000001D ? Math.signum(-probeVector.z) : 0).normalize();

				this.setBoundingBox(aabb);
				this.setLocationFromBoundingbox();
				this.setDeltaMovement(motion);

				// Movement vector projected to surface
				Vec3 surfaceMovementDir = movementDir.subtract(collisionNormal.scale(collisionNormal.dot(movementDir))).normalize();

				boolean isInnerCorner = Math.abs(collisionNormal.x) + Math.abs(collisionNormal.y) + Math.abs(collisionNormal.z) > 1.0001f;

				// Only project movement vector to surface if not moving across inner corner (avoids getting stuck)
				if (!isInnerCorner) {
					movementDir = surfaceMovementDir;
				}

				// Remove sticking force along movement vector projected to surface
				stickingForce = stickingForce.subtract(surfaceMovementDir.scale(surfaceMovementDir.normalize().dot(stickingForce)));

				float moveSpeed = Mth.sqrt(forward * forward + strafe * strafe);
				this.setDeltaMovement(this.getDeltaMovement().add(movementDir.scale(moveSpeed)));
			}
		}

		this.setDeltaMovement(this.getDeltaMovement().add(stickingForce));

		double px = this.getX();
		double py = this.getY();
		double pz = this.getZ();
		Vec3 motion = this.getDeltaMovement();

		this.move(MoverType.SELF, motion);

		// Inline move processing (also in onMove hook) for compat when mods cancel Entity.move() early
		this.setOnGround(this.horizontalCollision || this.verticalCollision);
		if(Math.abs(this.getY() - py - motion.y) > 0.000001D) {
			this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
		}

		this.prevAttachedSides = this.attachedSides;
		this.attachedSides = new Vec3(Math.abs(this.getX() - px - motion.x) > 0.001D ? -Math.signum(motion.x) : 0, Math.abs(this.getY() - py - motion.y) > 0.001D ? -Math.signum(motion.y) : 0, Math.abs(this.getZ() - pz - motion.z) > 0.001D ? -Math.signum(motion.z) : 0);

		float slipperiness = 0.91f;

		if (this.onGround()) {
			this.fallDistance = 0;

			BlockPos offsetPos = new BlockPos(blockPosition()).relative(groundDirection.getLeft());
			slipperiness = this.getBlockSlipperiness(offsetPos);
		}

		motion = this.getDeltaMovement();
		Vec3 orthogonalMotion = upVector.scale(upVector.dot(motion));
		Vec3 tangentialMotion = motion.subtract(orthogonalMotion);

		this.setDeltaMovement(tangentialMotion.x * slipperiness + orthogonalMotion.x * 0.98f, tangentialMotion.y * slipperiness + orthogonalMotion.y * 0.98f, tangentialMotion.z * slipperiness + orthogonalMotion.z * 0.98f);

		boolean detachedX = this.attachedSides.x != this.prevAttachedSides.x && Math.abs(this.attachedSides.x) < 0.001D;
		boolean detachedY = this.attachedSides.y != this.prevAttachedSides.y && Math.abs(this.attachedSides.y) < 0.001D;
		boolean detachedZ = this.attachedSides.z != this.prevAttachedSides.z && Math.abs(this.attachedSides.z) < 0.001D;

		if (detachedX || detachedY || detachedZ) {
			float stepHeight = this.maxUpStep();

			boolean prevOnGround = this.onGround();
			boolean prevCollidedHorizontally = this.horizontalCollision;
			boolean prevCollidedVertically = this.verticalCollision;

			// Move AABB above the new surface
			this.move(MoverType.SELF, new Vec3(detachedX ? -this.prevAttachedSides.x * 0.25f : 0, detachedY ? -this.prevAttachedSides.y * 0.25f : 0, detachedZ ? -this.prevAttachedSides.z * 0.25f : 0));

			Vec3 axis = this.prevAttachedSides.normalize();
			Vec3 attachVector = upVector.scale(-1);
			attachVector = attachVector.subtract(axis.scale(axis.dot(attachVector)));

			if (Math.abs(attachVector.x) > Math.abs(attachVector.y) && Math.abs(attachVector.x) > Math.abs(attachVector.z)) {
				attachVector = new Vec3(Math.signum(attachVector.x), 0, 0);
			} else if (Math.abs(attachVector.y) > Math.abs(attachVector.z)) {
				attachVector = new Vec3(0, Math.signum(attachVector.y), 0);
			} else {
				attachVector = new Vec3(0, 0, Math.signum(attachVector.z));
			}

			double attachDst = motion.length() + 0.1f;

			AABB aabb = this.getBoundingBox();
			motion = this.getDeltaMovement();

			// Move AABB towards new surface until it touches
			for (int i = 0; i < 2 && !this.onGround(); i++) {
				this.move(MoverType.SELF, attachVector.scale(attachDst));
			}

			// Attaching failed, fall back to previous position
			if (!this.onGround()) {
				this.setBoundingBox(aabb);
				this.setLocationFromBoundingbox();
				this.setDeltaMovement(motion);
				this.setOnGround(prevOnGround);
				this.horizontalCollision = prevCollidedHorizontally;
				this.verticalCollision = prevCollidedVertically;
			} else {
				this.setDeltaMovement(Vec3.ZERO);
			}
		}

		// Stuck detection: every 10 ticks check if spider has made progress
		if(this.tickCount % 10 == 0) {
			Vec3 currentPos = this.position();
			if(this.lastStuckCheckPos != null) {
				double distanceMoved = currentPos.distanceTo(this.lastStuckCheckPos);
				if(distanceMoved < 0.2D) {
					PathNavigation nav = this.getNavigation();
					Path path = nav != null ? nav.getPath() : null;
					if(path != null && !path.isDone()) {
						Vec3 waypoint = path.getNextEntityPos(this);
						Vec3 diff = waypoint.subtract(currentPos);
						Vec3 up = orientation.getGlobal(this.yRot, -90.0f);
						Vec3 dotComponent = up.scale(up.dot(diff));
						Vec3 forwardComponent = diff.subtract(dotComponent);

						Vec3 currentMotion = this.getDeltaMovement();
						Vec3 jumpVector = Vec3.ZERO;

						if(forwardComponent.lengthSqr() > 1.0E-7D) {
							jumpVector = forwardComponent.normalize().scale(0.4D).add(currentMotion.scale(0.2D));
							jumpVector = new Vec3(jumpVector.x * (1 - Math.abs(up.x)), jumpVector.y, jumpVector.z * (1 - Math.abs(up.z)));
						}

						jumpVector = jumpVector.add(up.scale(0.4D));

						this.setDeltaMovement(jumpVector);
						this.needsSync = true;

						float rx = (float) orientation.localZ.dot(jumpVector);
						float ry = (float) orientation.localX.dot(jumpVector);
						this.setYRot(270.0f - (float) Math.toDegrees(Mth.atan2(rx, ry)));
					}
				}
			}
			this.lastStuckCheckPos = currentPos;
		}

		this.calculateEntityAnimation( true);
	}

	@Override
	public boolean onMove(MoverType type, Vec3 pos, boolean pre) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return false;
		}
		if(this.isInWater() && !this.canClimbInWater) {
			return false;
		}

		if(pre) {
			this.preWalkingPosition = this.position();
			this.preMoveY = this.getY();
		} else {
			if(Math.abs(this.getY() - this.preMoveY - pos.y) > 0.000001D) {
				this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
			}

			this.setOnGround(this.horizontalCollision || this.verticalCollision);
		}

		return false;
	}

	@Override
	public BlockPos getAdjustedOnPosition(BlockPos onPosition) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return onPosition;
		}
		float verticalOffset = this.getVerticalOffset(1);

		int x = Mth.floor(this.getX() + this.attachmentOffsetX - (float) this.attachmentNormal.x * (verticalOffset + 0.2f));
		int y = Mth.floor(this.getY() + this.attachmentOffsetY - (float) this.attachmentNormal.y * (verticalOffset + 0.2f));
		int z = Mth.floor(this.getZ() + this.attachmentOffsetZ - (float) this.attachmentNormal.z * (verticalOffset + 0.2f));
		BlockPos pos = new BlockPos(x, y, z);

		if(this.level().isEmptyBlock(pos) && this.attachmentNormal.y < 0.0f) {
			BlockPos posDown = pos.below();
			BlockState stateDown = this.level().getBlockState(posDown);

			if (stateDown.is(BlockTags.FENCES) || stateDown.is(BlockTags.WALLS) || stateDown.getBlock() instanceof FenceGateBlock) {
				return posDown;
			}
		}

		return pos;
	}

	@Override
	public boolean getAdjustedCanTriggerWalking(boolean canTriggerWalking) {
		if (RotationOverrideConfig.isClimberDisabled(this.getType())) {
			return canTriggerWalking;
		}
		if(this.preWalkingPosition != null && this.canClimberTriggerWalking() && !this.isPassenger()) {
			Vec3 moved = this.position().subtract(this.preWalkingPosition);
			this.preWalkingPosition = null;

			BlockPos pos = this.getOnPos();
			BlockState state = this.level().getBlockState(pos);

			double dx = moved.x;
			double dy = moved.y;
			double dz = moved.z;

			Vec3 tangentialMovement = moved.subtract(this.attachmentNormal.scale(this.attachmentNormal.dot(moved)));

			this.moveDist = (float) ((double) this.moveDist + Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.6D);

			if(this.moveDist > this.nextStepDistance && !state.isAir()) {
				this.nextStepDistance = this.nextStep();

				if(this.isInWater()) {
					Entity controller = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;

					float multiplier = controller == this ? 0.35F : 0.4F;

					Vec3 motion = controller.getDeltaMovement();

					float swimStrength = (float)Math.sqrt(motion.x * motion.x * (double) 0.2F + motion.y * motion.y + motion.z * motion.z * 0.2F) * multiplier;
					if(swimStrength > 1.0F) {
						swimStrength = 1.0F;
					}

					this.playSwimSound(swimStrength);
				} else {
					this.playStepSound(pos, state);
				}
			} else if(state.isAir()) {
				this.processFlappingMovement();
			}
		}

		return false;
	}

	@Override
	public boolean canClimberTriggerWalking() {
		return true;
	}

	public void setLocationFromBoundingbox() {
		AABB axisalignedbb = this.getBoundingBox();
		this.setPosRaw((axisalignedbb.minX + axisalignedbb.maxX) / 2.0D, axisalignedbb.minY, (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0D);
	}
}
