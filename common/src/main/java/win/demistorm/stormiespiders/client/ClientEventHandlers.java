package win.demistorm.stormiespiders.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

import java.util.WeakHashMap;

public class ClientEventHandlers {

	// WeakHashMap for automatic cleanup and better entity tracking
	private static final WeakHashMap<LivingEntityRenderState, ClimberRenderData> renderStateDataMap = new WeakHashMap<>();

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

	// Apply transformations before main render
	public static void onPreRenderLivingFromState(LivingEntityRenderState renderState, PoseStack matrixStack) {
		ClimberRenderData data = renderStateDataMap.get(renderState);
		if (data != null) {
			matrixStack.pushPose(); // Push to ensure clean state
			applyClimberTransformPre(data, matrixStack);
		}
	}

	// Reverse transformations after main render
	public static void onPostRenderLivingFromState(LivingEntityRenderState renderState, PoseStack matrixStack) {
		ClimberRenderData data = renderStateDataMap.get(renderState);
		if (data != null) {
			applyClimberTransformPost(data, matrixStack);
			matrixStack.popPose(); // Pop to restore state

			// Remove from map (WeakHashMap handles cleanup if missed)
			renderStateDataMap.remove(renderState);
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
}