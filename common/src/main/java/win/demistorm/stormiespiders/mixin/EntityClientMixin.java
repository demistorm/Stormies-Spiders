package win.demistorm.stormiespiders.mixin;

import win.demistorm.stormiespiders.common.entity.mob.IEntityRotationHook;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityClientMixin implements IEntityRotationHook {

    @ModifyVariable(method = "moveOrInterpolateTo", at = @At(value = "HEAD"), ordinal = 1)
    private float onSetPositionAndRotationDirectYaw(float yaw, Vec3 pos, float yaw2, float pitch) {
        return this.getTargetYaw(pos.x, pos.y, pos.z, yaw, pitch, 0);
    }

    @Override
    public float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        return yaw;
    }

    @ModifyVariable(method = "moveOrInterpolateTo", at = @At(value = "HEAD"), ordinal = 2)
    private float onSetPositionAndRotationDirectPitch(float pitch, Vec3 pos, float yaw, float pitch2) {
        return this.getTargetPitch(pos.x, pos.y, pos.z, yaw, pitch, 0);
    }

    @Override
    public float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        return pitch;
    }

    @ModifyVariable(method = "lerpHeadTo", at = @At("HEAD"), ordinal = 0)
    private float onSetHeadRotation(float yaw, float yaw2, int rotationIncrements) {
        return this.getTargetHeadYaw(yaw, rotationIncrements);
    }

    @Override
    public float getTargetHeadYaw(float yaw, int rotationIncrements) {
        return yaw;
    }
}