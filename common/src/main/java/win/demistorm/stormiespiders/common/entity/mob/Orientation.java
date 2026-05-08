package win.demistorm.stormiespiders.common.entity.mob;

import win.demistorm.stormiespiders.common.Matrix4f;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class Orientation {
	public final Vec3 normal, localZ, localY, localX;
	public final float componentZ, componentY, componentX, yaw, pitch;

	public static Orientation fromNormal(Vec3 attachmentNormal) {
		Vec3 localZ = new Vec3(0, 0, 1);
		Vec3 localY = new Vec3(0, 1, 0);
		Vec3 localX = new Vec3(1, 0, 0);

		float componentZ = (float) localZ.dot(attachmentNormal);
		float componentX = (float) localX.dot(attachmentNormal);

		float yaw = (float) Math.toDegrees(Mth.atan2(componentX, componentZ));

		localZ = new Vec3(Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
		localY = new Vec3(0, 1, 0);
		localX = new Vec3(Math.sin(Math.toRadians(yaw - 90)), 0, Math.cos(Math.toRadians(yaw - 90)));

		componentZ = (float) localZ.dot(attachmentNormal);
		float componentY = (float) localY.dot(attachmentNormal);
		componentX = (float) localX.dot(attachmentNormal);

		float pitch = (float) Math.toDegrees(Mth.atan2(Mth.sqrt(componentX * componentX + componentZ * componentZ), componentY));

		Matrix4f m = new Matrix4f();
		m.multiply(new Matrix4f((float) Math.toRadians(yaw), 0, 1, 0));
		m.multiply(new Matrix4f((float) Math.toRadians(pitch), 1, 0, 0));
		m.multiply(new Matrix4f((float) Math.toRadians((float) Math.signum(0.5f - componentY - componentZ - componentX) * yaw), 0, 1, 0));

		localZ = m.multiply(new Vec3(0, 0, -1));
		localY = m.multiply(new Vec3(0, 1, 0));
		localX = m.multiply(new Vec3(1, 0, 0));

		return new Orientation(attachmentNormal, localZ, localY, localX, componentZ, componentY, componentX, yaw, pitch);
	}

	public Orientation(Vec3 normal, Vec3 localZ, Vec3 localY, Vec3 localX, float componentZ, float componentY, float componentX, float yaw, float pitch) {
		this.normal = normal;
		this.localZ = localZ;
		this.localY = localY;
		this.localX = localX;
		this.componentZ = componentZ;
		this.componentY = componentY;
		this.componentX = componentX;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public Vec3 getGlobal(Vec3 local) {
		return this.localX.scale(local.x).add(this.localY.scale(local.y)).add(this.localZ.scale(local.z));
	}

	public Vec3 getGlobal(float yaw, float pitch) {
		float cy = Mth.cos(yaw * 0.017453292F);
		float sy = Mth.sin(yaw * 0.017453292F);
		float cp = -Mth.cos(-pitch * 0.017453292F);
		float sp = Mth.sin(-pitch * 0.017453292F);
		return this.localX.scale(sy * cp).add(this.localY.scale(sp)).add(this.localZ.scale(cy * cp));
	}

	public Vec3 getLocal(Vec3 global) {
		return new Vec3(this.localX.dot(global), this.localY.dot(global), this.localZ.dot(global));
	}

	public Pair<Float, Float> getLocalRotation(Vec3 global) {
		Vec3 local = this.getLocal(global);

		float yaw = (float) Math.toDegrees(Mth.atan2(local.x, local.z)) + 180.0f;
		float pitch = (float) -Math.toDegrees(Mth.atan2(local.y, Math.sqrt(local.x * local.x + local.z * local.z)));

		return Pair.of(yaw, pitch);
	}
}