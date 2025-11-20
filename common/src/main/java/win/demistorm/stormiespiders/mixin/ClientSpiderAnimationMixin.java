package win.demistorm.stormiespiders.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        // Simple client-side climbing animation override
        // Check if spider is moving (basic check)
        Vec3 motion = entity.getDeltaMovement();
        float movementSpeed = (float) Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);

        // If spider is moving, apply animation (this is a simple approach)
        if(movementSpeed > 0.01f) {
            float animSpeed = Math.min(movementSpeed * 5.0f, 1.0f);
            entity.walkAnimation.update(animSpeed, 0.4f, 1.0f);
            System.out.println("[CLIENT] Simple client animation override - movementSpeed = " + movementSpeed + ", animSpeed = " + animSpeed);
        }
    }
}