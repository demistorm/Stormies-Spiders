package win.demistorm.stormiespiders.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import win.demistorm.stormiespiders.Constants;

import java.util.ArrayList;
import java.util.List;

// Compat between the pathfinding and Sable sublevels
public final class SubLevelPathing {

    private static final boolean ACTIVE;
    private static final SableCompanion COMPANION;
    private static boolean loggedEngagement;

    static {
        SableCompanion companion;
        try {
            companion = SableCompanion.INSTANCE;
        } catch (Throwable t) {
            // Sable isn't installed, fall back to plain worldspace pathfinding
            companion = null;
        }
        COMPANION = companion;
        ACTIVE = companion != null;
    }

    private SubLevelPathing() {
    }

    public static boolean isActive() {
        return ACTIVE;
    }

    public static SubLevelAccess trackingSubLevel(Mob mob) {
        return ACTIVE ? COMPANION.getTrackingSubLevel(mob) : null;
    }

    public static boolean onSubLevel(Mob mob) {
        return ACTIVE && COMPANION.getTrackingSubLevel(mob) != null;
    }

    // Mob position in the sublevel's own space
    public static Vec3 localMobPos(Mob mob) {
        if (!ACTIVE) {
            return mob.position();
        }
        SubLevelAccess subLevel = COMPANION.getTrackingSubLevel(mob);
        return subLevel != null ? subLevel.logicalPose().transformPositionInverse(mob.position()) : mob.position();
    }

    // Worldspace position to sublevel space
    public static BlockPos toLocal(Mob mob, BlockPos worldPos) {
        if (!ACTIVE) {
            return worldPos;
        }
        SubLevelAccess subLevel = COMPANION.getTrackingSubLevel(mob);
        if (subLevel == null) {
            return worldPos;
        }
        Vec3 local = subLevel.logicalPose().transformPositionInverse(Vec3.atCenterOf(worldPos));
        return BlockPos.containing(local);
    }

    public static BlockState getBlockState(Level level, Mob mob, BlockPos worldPos) {
        return level.getBlockState(toLocal(mob, worldPos));
    }

    public static boolean isLoaded(Level level, Mob mob, BlockPos worldPos) {
        return level.isLoaded(toLocal(mob, worldPos));
    }

    // Project a path out of sublevel space into worldspace so following lines up with the mob
    public static Path projectPathToWorld(Path path, Mob mob) {
        if (path == null || !ACTIVE) {
            return path;
        }
        SubLevelAccess subLevel = COMPANION.getTrackingSubLevel(mob);
        if (subLevel == null) {
            return path;
        }
        Pose3dc pose = subLevel.logicalPose();

        if (!loggedEngagement) {
            loggedEngagement = true;
            Constants.LOG.info("Sable sub level pathfinding engaged (a mob is pathfinding on a sub level)");
        }

        List<Node> worldNodes = new ArrayList<>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            BlockPos world = BlockPos.containing(pose.transformPosition(Vec3.atCenterOf(node.asBlockPos())));
            worldNodes.add(node.cloneAndMove(world.getX(), world.getY(), world.getZ()));
        }

        BlockPos worldTarget = path.getTarget();
        if (worldTarget != null) {
            worldTarget = BlockPos.containing(pose.transformPosition(Vec3.atCenterOf(worldTarget)));
        }

        return new Path(worldNodes, worldTarget, path.canReach());
    }
}
