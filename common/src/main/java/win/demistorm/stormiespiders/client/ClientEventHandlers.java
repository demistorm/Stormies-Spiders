package win.demistorm.stormiespiders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import win.demistorm.stormiespiders.client.RotationOverrideManager;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;


public class ClientEventHandlers {

	public static void onPreRenderLiving(LivingEntity entity, float partialTicks, PoseStack matrixStack) {

		if(entity instanceof IClimberEntity climber && !RotationOverrideConfig.isClimberDisabled(entity.getType())) {
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
		} else if(RotationOverrideManager.isRotationOverrideEntity(entity)) {
			RotationOverrideManager.updateIfNeeded(entity);

			Orientation renderOrientation = RotationOverrideManager.getOrientation(entity, partialTicks);
			float verticalOffset = RotationOverrideManager.getVerticalOffset();

			float x = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.X, partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
			float y = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
			float z = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

			matrixStack.translate(x, y, z);

			matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw));
			matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch));
			matrixStack.mulPose(Axis.YP.rotationDegrees((float) Math.signum(0.5f - renderOrientation.componentY - renderOrientation.componentZ - renderOrientation.componentX) * renderOrientation.yaw));
		}
	}

	public static void onPostRenderLiving(LivingEntity entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn) {

		if(entity instanceof IClimberEntity climber && !RotationOverrideConfig.isClimberDisabled(entity.getType())) {
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
		} else if(RotationOverrideManager.isRotationOverrideEntity(entity)) {
			Orientation renderOrientation = RotationOverrideManager.getOrientation(entity, partialTicks);
			float verticalOffset = RotationOverrideManager.getVerticalOffset();

			float x = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.X, partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
			float y = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
			float z = RotationOverrideManager.getAttachmentOffset(entity, Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

			matrixStack.mulPose(Axis.YP.rotationDegrees(-(float) Math.signum(0.5f - renderOrientation.componentY - renderOrientation.componentZ - renderOrientation.componentX) * renderOrientation.yaw));
			matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch));
			matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw));

			matrixStack.translate(-x, -y, -z);
		}
	}
}
