package win.demistorm.stormiespiders;

import win.demistorm.stormiespiders.config.ModConfig;
import win.demistorm.stormiespiders.config.NonClimbableBlocksConfig;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class commonInit {

    public static void init() {
        // Initialize config system
        ModConfig.load();
        // Initialize non-climbable blocks config
        NonClimbableBlocksConfig.init();
        RotationOverrideConfig.init();
    }
    public static BlockPos blockPos(double pX, double pY, double pZ) {
        return new BlockPos(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
    }
    public static BlockPos blockPos(Vec3 pVec3) {
        return blockPos(pVec3.x, pVec3.y, pVec3.z);
    }
}