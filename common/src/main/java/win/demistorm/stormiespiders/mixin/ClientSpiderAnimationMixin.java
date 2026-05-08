package win.demistorm.stormiespiders.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;

@Mixin(Spider.class)
public class ClientSpiderAnimationMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (RotationOverrideConfig.isClimberDisabled(entity.getType())) {
            return;
        }

        // Client side only
        if(!entity.level().isClientSide()) {
            return;
        }

        // Detect climbing by checking for air below spider
        Vec3 motion = entity.getDeltaMovement();
        float movementSpeed = (float) Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);

        // Look for air 0.2 blocks below spider
        net.minecraft.core.BlockPos underPos = net.minecraft.core.BlockPos.containing(
            entity.getX(),
            entity.getY() - 0.2,
            entity.getZ()
        );

        boolean hasAirUnderneath = entity.level().getBlockState(underPos).isAir();
        boolean isClimbing = hasAirUnderneath && movementSpeed > 0.01f;

        if(movementSpeed > 0.01f) {
            // Speed up animation when climbing, slow down when on ground
            float multiplier = isClimbing ? 8.0f : 3.0f; // First float is climbing speed, second is ground speed
            float animSpeed = Math.min(movementSpeed * multiplier, 1.0f);
            entity.walkAnimation.update(animSpeed, 0.4f, 1.0f);
        }
    }
}