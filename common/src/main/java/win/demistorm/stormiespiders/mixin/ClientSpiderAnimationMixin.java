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

@Mixin(Spider.class)
public class ClientSpiderAnimationMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Cast to LivingEntity to access walkAnimation
        LivingEntity entity = (LivingEntity)(Object)this;

        // Only run additional logic on client side
        if(!entity.level().isClientSide) {
            return;
        }

        // World-space climbing detection: check for air underneath spider
        Vec3 motion = entity.getDeltaMovement();
        float movementSpeed = (float) Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);

        // Check if there's air 0.2 blocks directly underneath the spider (world space)
        net.minecraft.core.BlockPos underPos = net.minecraft.core.BlockPos.containing(
            entity.getX(),
            entity.getY() - 0.2,
            entity.getZ()
        );

        boolean hasAirUnderneath = entity.level().getBlockState(underPos).isAir();
        boolean isClimbing = hasAirUnderneath && movementSpeed > 0.01f;

        if(movementSpeed > 0.01f) {
            // Use higher animation speed for climbing (air underneath), normal for ground (solid underneath)
            float multiplier = isClimbing ? 8.0f : 3.0f; // Ground gets slower speed, climbing gets enhanced speed
            float animSpeed = Math.min(movementSpeed * multiplier, 1.0f);
            entity.walkAnimation.update(animSpeed, 0.4f, 1.0f);
            System.out.println("[CLIENT] Animation - movementSpeed = " + movementSpeed + ", animSpeed = " + animSpeed +
                ", hasAirUnderneath = " + hasAirUnderneath + ", isClimbing = " + isClimbing +
                ", underBlock = " + entity.level().getBlockState(underPos).getBlock().getName().getString());
        }
    }
}