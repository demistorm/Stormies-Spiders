package win.demistorm.stormiespiders;

import win.demistorm.stormiespiders.compat.sable.SubLevelPathing;
import win.demistorm.stormiespiders.config.ModConfig;
import win.demistorm.stormiespiders.config.NonClimbableBlocksConfig;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;
import win.demistorm.stormiespiders.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class commonInit {

    private static final String SABLE_MOD_ID = "sablecompanion";

    public static void init() {
        // Initialize config system
        ModConfig.load();
        // Initialize non-climbable blocks config
        NonClimbableBlocksConfig.init();
        RotationOverrideConfig.init();

        // Report Sable sub level pathfinding compat at startup
        if (SubLevelPathing.isActive()) {
            Constants.LOG.info("Sable sublevel pathfinding compatibility enabled (on sublevel pathfinding is active)");
        } else if (Services.PLATFORM.isModLoaded(SABLE_MOD_ID)) {
            Constants.LOG.info("Sable sublevel pathfinding compatibility inactive (Sable Companion detected but version is too old, see warning above)");
        } else {
            Constants.LOG.info("Sable sublevel pathfinding compatibility inactive (Sable Companion not found)");
        }
    }
    public static BlockPos blockPos(double pX, double pY, double pZ) {
        return new BlockPos(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
    }
    public static BlockPos blockPos(Vec3 pVec3) {
        return blockPos(pVec3.x, pVec3.y, pVec3.z);
    }
}
