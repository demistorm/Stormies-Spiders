package win.demistorm.stormiespiders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import win.demistorm.stormiespiders.client.RotationOverrideManager;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.WeakHashMap;

public class ClientEventHandlers {

	// WeakHashMap for automatic cleanup and better entity tracking
	private static final WeakHashMap<LivingEntityRenderState, ClimberRenderData> renderStateDataMap = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntityRenderState, RotationOverrideRenderData> rotationOverrideRenderStateDataMap = new WeakHashMap<>();
	private static final WeakHashMap<LivingEntityRenderState, ClimberPassengerRenderData> climberPassengerRenderStateDataMap = new WeakHashMap<>();

	private static class ClimberRenderData {
		final IClimberEntity climber;
		final float partialTicks;
		final Orientation renderOrientation;
		final float verticalOffset;
		final Orientation currentOrientation;

		ClimberRenderData(IClimberEntity climber, float partialTicks,
						  Orientation renderOrientation, float verticalOffset,
						  Orientation currentOrientation) {
			this.climber = climber;
			this.partialTicks = partialTicks;
			this.renderOrientation = renderOrientation;
			this.verticalOffset = verticalOffset;
			this.currentOrientation = currentOrientation;
		}
	}

	private static class RotationOverrideRenderData {
		final LivingEntity entity;
		final float partialTicks;
		final Orientation renderOrientation;
		final float verticalOffset;

		RotationOverrideRenderData(LivingEntity entity, float partialTicks,
						 Orientation renderOrientation, float verticalOffset) {
			this.entity = entity;
			this.partialTicks = partialTicks;
			this.renderOrientation = renderOrientation;
			this.verticalOffset = verticalOffset;
		}
	}

	private static class ClimberPassengerRenderData {
		final Orientation renderOrientation;
		final float rollDegrees;
		final float translateX;
		final float translateY;
		final float translateZ;

		ClimberPassengerRenderData(Orientation renderOrientation, float rollDegrees,
								   float translateX, float translateY, float translateZ) {
			this.renderOrientation = renderOrientation;
			this.rollDegrees = rollDegrees;
			this.translateX = translateX;
			this.translateY = translateY;
			this.translateZ = translateZ;
		}
	}

	// Store climber data using renderState as key (survives batched rendering)
	public static void storeClimberDataForRenderState(LivingEntityRenderState renderState,
													  LivingEntity entity,
													  IClimberEntity climber,
													  float partialTicks) {
		// Calculate render orientation with interpolation
		Orientation renderOrientation = climber.calculateOrientation(partialTicks);
		climber.setRenderOrientation(renderOrientation);

		float verticalOffset = climber.getVerticalOffset(partialTicks);

		// Store with renderState as key (persists through rendering pipeline)
		renderStateDataMap.put(renderState, new ClimberRenderData(
				climber,
				partialTicks,
				renderOrientation,
				verticalOffset,
				climber.getOrientation()
		));
	}

	public static void storeRotationOverrideDataForRenderState(LivingEntityRenderState renderState,
														 LivingEntity entity,
														 float partialTicks) {
		Orientation renderOrientation = RotationOverrideManager.getOrientation(entity, partialTicks);
		float verticalOffset = RotationOverrideManager.getVerticalOffset();

		rotationOverrideRenderStateDataMap.put(renderState, new RotationOverrideRenderData(
				entity,
				partialTicks,
				renderOrientation,
				verticalOffset
		));
	}

	public static void storeClimberPassengerDataForRenderState(LivingEntityRenderState renderState,
															   LivingEntity passenger,
															   IClimberEntity vehicle,
															   float partialTicks) {
		Orientation renderOrientation = vehicle.calculateOrientation(partialTicks);

		Vec3 attach = passenger.getVehicleAttachmentPoint((Entity) vehicle);
		Vec3 anchorOffset = attach.subtract(renderOrientation.getGlobal(attach));

		Entity vehicleEntity = (Entity) vehicle;
		BlockPos lightProbe = BlockPos.containing(vehicleEntity.getLightProbePosition(partialTicks));
		renderState.lightCoords = LightTexture.pack(
				passenger.isOnFire() ? 15 : passenger.level().getBrightness(LightLayer.BLOCK, lightProbe),
				passenger.level().getBrightness(LightLayer.SKY, lightProbe)
		);

		float rollDegrees = Math.signum(0.5f - renderOrientation.componentY
				- renderOrientation.componentZ
				- renderOrientation.componentX) * renderOrientation.yaw;

		climberPassengerRenderStateDataMap.put(renderState, new ClimberPassengerRenderData(
				renderOrientation,
				rollDegrees,
				(float) anchorOffset.x,
				(float) anchorOffset.y,
				(float) anchorOffset.z
		));
	}

	// Apply transformations before main render
	public static void onPreRenderLivingFromState(LivingEntityRenderState renderState, PoseStack matrixStack) {
		ClimberRenderData data = renderStateDataMap.get(renderState);
		if (data != null) {
			matrixStack.pushPose();
			applyClimberTransformPre(data, matrixStack);
			return;
		}
		ClimberPassengerRenderData passengerData = climberPassengerRenderStateDataMap.get(renderState);
		if (passengerData != null) {
			matrixStack.pushPose();
			applyClimberPassengerTransformPre(passengerData, matrixStack);
			return;
		}
		RotationOverrideRenderData vData = rotationOverrideRenderStateDataMap.get(renderState);
		if (vData != null) {
			matrixStack.pushPose();
			applyRotationOverrideTransformPre(vData, matrixStack);
		}
	}

	// Reverse transformations after main render
	public static void onPostRenderLivingFromState(LivingEntityRenderState renderState, PoseStack matrixStack) {
		ClimberRenderData data = renderStateDataMap.get(renderState);
		if (data != null) {
			applyClimberTransformPost(data, matrixStack);
			matrixStack.popPose();
			renderStateDataMap.remove(renderState);
			return;
		}
		ClimberPassengerRenderData passengerData = climberPassengerRenderStateDataMap.get(renderState);
		if (passengerData != null) {
			applyClimberPassengerTransformPost(passengerData, matrixStack);
			matrixStack.popPose();
			climberPassengerRenderStateDataMap.remove(renderState);
			return;
		}
		RotationOverrideRenderData vData = rotationOverrideRenderStateDataMap.get(renderState);
		if (vData != null) {
			applyRotationOverrideTransformPost(vData, matrixStack);
			matrixStack.popPose();
			rotationOverrideRenderStateDataMap.remove(renderState);
		}
	}

	private static void applyClimberTransformPre(ClimberRenderData data, PoseStack matrixStack) {
		IClimberEntity climber = data.climber;
		Orientation renderOrientation = data.renderOrientation;
		float verticalOffset = data.verticalOffset;
		float partialTicks = data.partialTicks;

		// Use interpolated attachment offsets
		float x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks)
				- (float) renderOrientation.normal.x * verticalOffset;
		float y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks)
				- (float) renderOrientation.normal.y * verticalOffset;
		float z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks)
				- (float) renderOrientation.normal.z * verticalOffset;

		matrixStack.translate(x, y, z);

		matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw));
		matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch));
		matrixStack.mulPose(Axis.YP.rotationDegrees(
				Math.signum(0.5f - renderOrientation.componentY
						- renderOrientation.componentZ
						- renderOrientation.componentX) * renderOrientation.yaw));
	}

	private static void applyClimberTransformPost(ClimberRenderData data, PoseStack matrixStack) {
		Orientation orientation = data.currentOrientation;
		Orientation renderOrientation = data.renderOrientation;

		if (renderOrientation != null) {
			float verticalOffset = data.verticalOffset;
			float partialTicks = data.partialTicks;
			IClimberEntity climber = data.climber;

			float x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks)
					- (float) renderOrientation.normal.x * verticalOffset;
			float y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks)
					- (float) renderOrientation.normal.y * verticalOffset;
			float z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks)
					- (float) renderOrientation.normal.z * verticalOffset;

			// Reverse transformations in opposite order
			matrixStack.mulPose(Axis.YP.rotationDegrees(
					-(float) Math.signum(0.5f - orientation.componentY
							- orientation.componentZ
							- orientation.componentX) * renderOrientation.yaw));
			matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch));
			matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw));

			matrixStack.translate(-x, -y, -z);
		}
	}

	private static void applyClimberPassengerTransformPre(ClimberPassengerRenderData data, PoseStack matrixStack) {
		Orientation renderOrientation = data.renderOrientation;

		matrixStack.translate(data.translateX, data.translateY, data.translateZ);

		matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw));
		matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch));
		matrixStack.mulPose(Axis.YP.rotationDegrees(data.rollDegrees));
	}

	private static void applyClimberPassengerTransformPost(ClimberPassengerRenderData data, PoseStack matrixStack) {
		Orientation renderOrientation = data.renderOrientation;

		matrixStack.mulPose(Axis.YP.rotationDegrees(-data.rollDegrees));
		matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch));
		matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw));

		matrixStack.translate(-data.translateX, -data.translateY, -data.translateZ);
	}

	private static void applyRotationOverrideTransformPre(RotationOverrideRenderData data, PoseStack matrixStack) {
		Orientation renderOrientation = data.renderOrientation;
		float verticalOffset = data.verticalOffset;
		float partialTicks = data.partialTicks;
		LivingEntity entity = data.entity;

		float x = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.X, partialTicks)
				- (float) renderOrientation.normal.x * verticalOffset;
		float y = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Y, partialTicks)
				- (float) renderOrientation.normal.y * verticalOffset;
		float z = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Z, partialTicks)
				- (float) renderOrientation.normal.z * verticalOffset;

		matrixStack.translate(x, y, z);

		matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw));
		matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch));
		matrixStack.mulPose(Axis.YP.rotationDegrees(
				Math.signum(0.5f - renderOrientation.componentY
						- renderOrientation.componentZ
						- renderOrientation.componentX) * renderOrientation.yaw));
	}

	private static void applyRotationOverrideTransformPost(RotationOverrideRenderData data, PoseStack matrixStack) {
		Orientation renderOrientation = data.renderOrientation;
		float verticalOffset = data.verticalOffset;
		float partialTicks = data.partialTicks;
		LivingEntity entity = data.entity;

		float x = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.X, partialTicks)
				- (float) renderOrientation.normal.x * verticalOffset;
		float y = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Y, partialTicks)
				- (float) renderOrientation.normal.y * verticalOffset;
		float z = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Z, partialTicks)
				- (float) renderOrientation.normal.z * verticalOffset;

		// Reverse transformations in opposite order
		matrixStack.mulPose(Axis.YP.rotationDegrees(
				-(float) Math.signum(0.5f - renderOrientation.componentY
						- renderOrientation.componentZ
						- renderOrientation.componentX) * renderOrientation.yaw));
		matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch));
		matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw));

		matrixStack.translate(-x, -y, -z);
	}
}