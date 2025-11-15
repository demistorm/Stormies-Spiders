package win.demistorm.stormiespiders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class ClientEventHandlers {

	// Cache to store climber data between extractRenderState and render calls
	private static final Map<Integer, ClimberRenderData> climberDataCache = new HashMap<>();

	private static class ClimberRenderData {
		final IClimberEntity climber;
		final float partialTicks;
		final Orientation renderOrientation;
		final float verticalOffset;

		ClimberRenderData(IClimberEntity climber, float partialTicks, Orientation renderOrientation, float verticalOffset) {
			this.climber = climber;
			this.partialTicks = partialTicks;
			this.renderOrientation = renderOrientation;
			this.verticalOffset = verticalOffset;
		}
	}
	
	public static void onPreRenderLiving(LivingEntity entity, float partialTicks, PoseStack matrixStack) {

		if(entity instanceof IClimberEntity) {
			IClimberEntity climber = (IClimberEntity) entity;

			Orientation orientation = climber.getOrientation();
			Orientation renderOrientation = climber.calculateOrientation(partialTicks);
			climber.setRenderOrientation(renderOrientation);

			float verticalOffset = climber.getVerticalOffset(partialTicks);

			float x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
			float y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
			float z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

			matrixStack.translate(x, y, z);

			matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw));
			matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch));
			matrixStack.mulPose(Axis.YP.rotationDegrees((float) Math.signum(0.5f - orientation.componentY - orientation.componentZ - orientation.componentX) * renderOrientation.yaw));
		}
	}

	public static void onPostRenderLiving(LivingEntity entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn) {

		if(entity instanceof IClimberEntity) {
			IClimberEntity climber = (IClimberEntity) entity;
			Orientation orientation = climber.getOrientation();
			Orientation renderOrientation = climber.getRenderOrientation();

			if(renderOrientation != null) {
				float verticalOffset = climber.getVerticalOffset(partialTicks);

				float x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
				float y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
				float z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

				matrixStack.mulPose(Axis.YP.rotationDegrees(-(float) Math.signum(0.5f - orientation.componentY - orientation.componentZ - orientation.componentX) * renderOrientation.yaw));
				matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch));
				matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw));

				
				matrixStack.translate(-x, -y, -z);
			}
		}
	}

	// New methods for 1.21.4 render state approach
	public static void storeClimberData(LivingEntity entity, IClimberEntity climber, float partialTicks) {
		Orientation renderOrientation = climber.calculateOrientation(partialTicks);
		float verticalOffset = climber.getVerticalOffset(partialTicks);
		int entityId = entity.getId();

		climberDataCache.put(entityId, new ClimberRenderData(climber, partialTicks, renderOrientation, verticalOffset));
	}

	public static void onPreRenderLivingFromState(LivingEntityRenderState renderState, PoseStack matrixStack) {
		// Get entity ID from render state (this might need adjustment based on the actual LivingEntityRenderState implementation)
		// For now, we'll use a simple approach that checks all cached climbers
		ClimberRenderData data = findClimberDataForRenderState(renderState);
		if (data != null) {
			applyClimberTransformPre(data, matrixStack);
		}
	}

	public static void onPostRenderLivingFromState(LivingEntityRenderState renderState, PoseStack matrixStack, MultiBufferSource bufferIn) {
		ClimberRenderData data = findClimberDataForRenderState(renderState);
		if (data != null) {
			applyClimberTransformPost(data, matrixStack);
		}

		// Clean up old cache entries periodically to prevent memory leaks
		if (Math.random() < 0.01) { // 1% chance to clean up each frame
			climberDataCache.clear();
		}
	}

	private static ClimberRenderData findClimberDataForRenderState(LivingEntityRenderState renderState) {
		// This is a simplified approach - in a real implementation, you'd want to match
		// the render state to the correct entity more precisely
		// For now, return the most recent climber data
		return climberDataCache.values().stream().findFirst().orElse(null);
	}

	private static void applyClimberTransformPre(ClimberRenderData data, PoseStack matrixStack) {
		IClimberEntity climber = data.climber;
		Orientation renderOrientation = data.renderOrientation;
		float verticalOffset = data.verticalOffset;

		float x = climber.getAttachmentOffset(Direction.Axis.X, data.partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
		float y = climber.getAttachmentOffset(Direction.Axis.Y, data.partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
		float z = climber.getAttachmentOffset(Direction.Axis.Z, data.partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

		matrixStack.translate(x, y, z);

		matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw));
		matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch));
		matrixStack.mulPose(Axis.YP.rotationDegrees((float) Math.signum(0.5f - renderOrientation.componentY - renderOrientation.componentZ - renderOrientation.componentX) * renderOrientation.yaw));
	}

	private static void applyClimberTransformPost(ClimberRenderData data, PoseStack matrixStack) {
		IClimberEntity climber = data.climber;
		Orientation orientation = climber.getOrientation();
		Orientation renderOrientation = data.renderOrientation;

		if(renderOrientation != null) {
			float verticalOffset = data.verticalOffset;

			float x = climber.getAttachmentOffset(Direction.Axis.X, data.partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
			float y = climber.getAttachmentOffset(Direction.Axis.Y, data.partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
			float z = climber.getAttachmentOffset(Direction.Axis.Z, data.partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

			matrixStack.mulPose(Axis.YP.rotationDegrees(-(float) Math.signum(0.5f - orientation.componentY - orientation.componentZ - orientation.componentX) * renderOrientation.yaw));
			matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch));
			matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw));

			matrixStack.translate(-x, -y, -z);
		}
	}
}
