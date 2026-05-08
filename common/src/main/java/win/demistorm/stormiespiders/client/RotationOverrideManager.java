package win.demistorm.stormiespiders.client;

import net.minecraft.util.Mth;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.tuple.Pair;
import win.demistorm.stormiespiders.common.CollisionSmoothingUtil;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RotationOverrideManager {

	private static final float COLLISIONS_INCLUSION_RANGE = 2.0f;
	private static final float COLLISIONS_SMOOTHING_RANGE = 1.25f;
	private static final float VERTICAL_OFFSET = 0.02f;
	private static final float SMOOTH_FACTOR = 0.5f;

	private static final Vec3 DEFAULT_NORMAL = new Vec3(0, 1, 0);

	private static final Map<Integer, VisualAttachmentState> entityStates = new ConcurrentHashMap<>();
	private static long lastCleanupTick = 0;

	private static class VisualAttachmentState {
		Vec3 targetNormal = new Vec3(0, 1, 0);
		Vec3 smoothedNormal = new Vec3(0, 1, 0);
		Vec3 prevSmoothedNormal = new Vec3(0, 1, 0);
		double targetOffsetX;
		double targetOffsetY;
		double targetOffsetZ;
		double smoothedOffsetX;
		double smoothedOffsetY;
		double smoothedOffsetZ;
		double prevSmoothedOffsetX;
		double prevSmoothedOffsetY;
		double prevSmoothedOffsetZ;
		long lastUpdateTick = -1;
	}

	public static boolean isRotationOverrideEntity(LivingEntity entity) {
		return RotationOverrideConfig.isRotationOverrideEnabled(entity.getType());
	}

	public static void updateIfNeeded(LivingEntity entity) {
		if (!entity.level().isClientSide()) {
			return;
		}

		long currentTick = entity.level().getGameTime();
		int entityId = entity.getId();

		VisualAttachmentState state = entityStates.get(entityId);
		if (state != null && state.lastUpdateTick == currentTick) {
			return;
		}

		if (state == null) {
			state = new VisualAttachmentState();
			entityStates.put(entityId, state);
		}

		state.prevSmoothedNormal = state.smoothedNormal;
		state.prevSmoothedOffsetX = state.smoothedOffsetX;
		state.prevSmoothedOffsetY = state.smoothedOffsetY;
		state.prevSmoothedOffsetZ = state.smoothedOffsetZ;

		computeAttachment(entity, state);

		float tiltFactor = (float) (1.0 - Math.abs(state.targetNormal.y));
		state.targetOffsetY = tiltFactor * 0.5f;

		state.smoothedNormal = new Vec3(
			Mth.lerp(SMOOTH_FACTOR, state.smoothedNormal.x, state.targetNormal.x),
			Mth.lerp(SMOOTH_FACTOR, state.smoothedNormal.y, state.targetNormal.y),
			Mth.lerp(SMOOTH_FACTOR, state.smoothedNormal.z, state.targetNormal.z)
		).normalize();
		state.smoothedOffsetX = Mth.lerp(SMOOTH_FACTOR, state.smoothedOffsetX, state.targetOffsetX);
		state.smoothedOffsetY = Mth.lerp(SMOOTH_FACTOR, state.smoothedOffsetY, state.targetOffsetY);
		state.smoothedOffsetZ = Mth.lerp(SMOOTH_FACTOR, state.smoothedOffsetZ, state.targetOffsetZ);

		state.lastUpdateTick = currentTick;

		if (currentTick - lastCleanupTick > 200) {
			lastCleanupTick = currentTick;
			cleanupStaleEntries(entity);
		}
	}

	private static void resetToDefault(VisualAttachmentState state) {
		state.targetOffsetX = 0;
		state.targetOffsetY = 0;
		state.targetOffsetZ = 0;
		state.targetNormal = DEFAULT_NORMAL;
	}

	private static void computeAttachment(LivingEntity entity, VisualAttachmentState state) {
		Vec3 p = entity.position();
		Vec3 s = p.add(0, entity.getBbHeight() * 0.5f, 0);
		AABB inclusionBox = new AABB(s.x, s.y, s.z, s.x, s.y, s.z).inflate(COLLISIONS_INCLUSION_RANGE);

		Pair<Vec3, Vec3> attachmentPoint = CollisionSmoothingUtil.findClosestPoint(
			consumer -> {
				CollisionGetter collisionGetter = entity.level();
				for (VoxelShape shape : collisionGetter.getBlockCollisions(entity, inclusionBox)) {
					shape.forAllBoxes(consumer);
				}
			},
			s,
			state.smoothedNormal.scale(-1),
			COLLISIONS_SMOOTHING_RANGE,
			1.0f,
			0.001f,
			20,
			0.05f,
			s
		);

		if (attachmentPoint != null) {
			Vec3 attachmentPos = attachmentPoint.getLeft();
			AABB entityBox = entity.getBoundingBox();

			double dx = Math.max(entityBox.minX - attachmentPos.x, attachmentPos.x - entityBox.maxX);
			double dy = Math.max(entityBox.minY - attachmentPos.y, attachmentPos.y - entityBox.maxY);
			double dz = Math.max(entityBox.minZ - attachmentPos.z, attachmentPos.z - entityBox.maxZ);

			if (Math.max(dx, Math.max(dy, dz)) < 0.5f) {
				float halfWidth = entity.getBbWidth() / 2;
				state.targetOffsetX = Mth.clamp(attachmentPos.x - p.x, -halfWidth, halfWidth);
				state.targetOffsetY = Mth.clamp(attachmentPos.y - p.y, 0, entity.getBbHeight());
				state.targetOffsetZ = Mth.clamp(attachmentPos.z - p.z, -halfWidth, halfWidth);
				state.targetNormal = attachmentPoint.getRight();
			} else {
				resetToDefault(state);
			}
		} else {
			resetToDefault(state);
		}
	}

	public static Orientation getOrientation(LivingEntity entity, float partialTicks) {
		VisualAttachmentState state = entityStates.get(entity.getId());
		if (state == null) {
			return Orientation.fromNormal(DEFAULT_NORMAL);
		}

		Vec3 normal = new Vec3(
			Mth.lerp(partialTicks, state.prevSmoothedNormal.x, state.smoothedNormal.x),
			Mth.lerp(partialTicks, state.prevSmoothedNormal.y, state.smoothedNormal.y),
			Mth.lerp(partialTicks, state.prevSmoothedNormal.z, state.smoothedNormal.z)
		).normalize();

		return Orientation.fromNormal(normal);
	}

	public static float getAttachmentOffset(LivingEntity entity, Direction.Axis axis, float partialTicks) {
		VisualAttachmentState state = entityStates.get(entity.getId());
		if (state == null) {
			return 0;
		}

		switch (axis) {
			default:
			case X:
				return (float) Mth.lerp(partialTicks, state.prevSmoothedOffsetX, state.smoothedOffsetX);
			case Y:
				return (float) Mth.lerp(partialTicks, state.prevSmoothedOffsetY, state.smoothedOffsetY);
			case Z:
				return (float) Mth.lerp(partialTicks, state.prevSmoothedOffsetZ, state.smoothedOffsetZ);
		}
	}

	public static float getVerticalOffset() {
		return VERTICAL_OFFSET;
	}

	private static void cleanupStaleEntries(LivingEntity referenceEntity) {
		entityStates.entrySet().removeIf(entry -> {
			net.minecraft.world.entity.Entity found = referenceEntity.level().getEntity(entry.getKey());
			return found == null || !(found instanceof LivingEntity);
		});
	}

	public static void clearAll() {
		entityStates.clear();
	}
}
