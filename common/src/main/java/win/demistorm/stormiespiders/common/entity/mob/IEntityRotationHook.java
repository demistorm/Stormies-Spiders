package win.demistorm.stormiespiders.common.entity.mob;

public interface IEntityRotationHook {
    float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements);
    float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements);
    float getTargetHeadYaw(float yaw, int rotationIncrements);
}