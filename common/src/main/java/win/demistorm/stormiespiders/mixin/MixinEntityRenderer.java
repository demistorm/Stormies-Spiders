package win.demistorm.stormiespiders.mixin;

import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {

	@Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
	private void onGetPackedLightCoords(T entity, float partialTicks, CallbackInfoReturnable<Integer> ci) {
		if (entity instanceof LivingEntity passenger && passenger.isPassenger()
				&& passenger.getVehicle() instanceof IClimberEntity climber
				&& climber.hasAttachmentSync()
				&& !RotationOverrideConfig.isClimberDisabled(passenger.getVehicle().getType())) {
			Entity vehicle = passenger.getVehicle();
			BlockPos lightProbe = BlockPos.containing(vehicle.getLightProbePosition(partialTicks));
			ci.setReturnValue(LightTexture.pack(
					passenger.isOnFire() ? 15 : passenger.level().getBrightness(LightLayer.BLOCK, lightProbe),
					passenger.level().getBrightness(LightLayer.SKY, lightProbe)
			));
		}
	}
}
