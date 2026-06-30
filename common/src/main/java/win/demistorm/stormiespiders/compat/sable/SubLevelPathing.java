package win.demistorm.stormiespiders.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import win.demistorm.stormiespiders.Constants;
import win.demistorm.stormiespiders.common.entity.movement.DirectionalPathPoint;
import win.demistorm.stormiespiders.platform.Services;

import java.util.ArrayList;
import java.util.List;

// Compat between the pathfinding and Sable sublevels
public final class SubLevelPathing {

    // Minimum Sable Companion version to run compat on
    private static final String MIN_SABLE_VERSION = "1.6.0";
    private static final String SABLE_MOD_ID = "sablecompanion";

    private static volatile boolean ACTIVE;
    private static volatile String detectedSableVersion;
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
        if (companion != null) {
            String version = null;
            try {
                version = Services.PLATFORM.getModVersion(SABLE_MOD_ID);
            } catch (Throwable ignored) {
            }
            detectedSableVersion = version;
            ACTIVE = version != null && versionAtLeast(version, MIN_SABLE_VERSION);
            if (!ACTIVE) {
                Constants.LOG.warn("Sable Companion detected but sublevel pathfinding compat is disabled. Found version {}, required {} or newer. Update Sable/Sable Companion", version, MIN_SABLE_VERSION);
            }
        } else {
            ACTIVE = false;
        }
    }

    private SubLevelPathing() {
    }

    public static boolean isActive() {
        return ACTIVE;
    }

    public static SubLevelAccess trackingSubLevel(Mob mob) {
        return querySubLevel(mob);
    }

    public static boolean onSubLevel(Mob mob) {
        return querySubLevel(mob) != null;
    }

    public static AABB toSubLevelAABB(Mob mob, AABB world) {
        if (!ACTIVE) {
            return world;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        if (subLevel == null) {
            return world;
        }
        return transformCorners(subLevel.logicalPose(), world, true);
    }

    public static void emitWorldBox(Mob mob, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Shapes.DoubleLineConsumer action) {
        if (!ACTIVE) {
            action.consume(minX, minY, minZ, maxX, maxY, maxZ);
            return;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        if (subLevel == null) {
            action.consume(minX, minY, minZ, maxX, maxY, maxZ);
            return;
        }
        AABB world = transformCorners(subLevel.logicalPose(), new AABB(minX, minY, minZ, maxX, maxY, maxZ), false);
        action.consume(world.minX, world.minY, world.minZ, world.maxX, world.maxY, world.maxZ);
    }

    private static AABB transformCorners(Pose3dc pose, AABB box, boolean inverse) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 8; i++) {
            double cx = (i & 1) == 0 ? box.minX : box.maxX;
            double cy = (i & 2) == 0 ? box.minY : box.maxY;
            double cz = (i & 4) == 0 ? box.minZ : box.maxZ;
            Vec3 corner = inverse ? pose.transformPositionInverse(new Vec3(cx, cy, cz)) : pose.transformPosition(new Vec3(cx, cy, cz));
            if (corner.x < minX) minX = corner.x;
            if (corner.y < minY) minY = corner.y;
            if (corner.z < minZ) minZ = corner.z;
            if (corner.x > maxX) maxX = corner.x;
            if (corner.y > maxY) maxY = corner.y;
            if (corner.z > maxZ) maxZ = corner.z;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // Worldspace point into sublevel space
    public static Vec3 toSubLevelPoint(Mob mob, Vec3 world) {
        if (!ACTIVE) {
            return world;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        return subLevel != null ? subLevel.logicalPose().transformPositionInverse(world) : world;
    }

    // Sublevel point back to worldspace
    public static Vec3 toWorldPoint(Mob mob, Vec3 local) {
        if (!ACTIVE) {
            return local;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        return subLevel != null ? subLevel.logicalPose().transformPosition(local) : local;
    }

    // Worldspace surface normal into sublevel space (rotation only, no translation)
    public static Vec3 toSubLevelNormal(Mob mob, Vec3 worldNormal) {
        if (!ACTIVE) {
            return worldNormal;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        return subLevel != null ? subLevel.logicalPose().transformNormalInverse(worldNormal) : worldNormal;
    }

    // Sublevel surface normal back to worldspace
    public static Vec3 toWorldNormal(Mob mob, Vec3 localNormal) {
        if (!ACTIVE) {
            return localNormal;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        return subLevel != null ? subLevel.logicalPose().transformNormal(localNormal) : localNormal;
    }

    // Rotate a sublevel local direction into worldspace snapped to the nearest world axis
    public static Direction toWorldDirection(Mob mob, Direction localDir) {
        if (!ACTIVE) {
            return localDir;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        if (subLevel == null) {
            return localDir;
        }
        Vec3 worldNormal = subLevel.logicalPose().transformNormal(new Vec3(localDir.getStepX(), localDir.getStepY(), localDir.getStepZ()));
        return Direction.getNearest(worldNormal.x, worldNormal.y, worldNormal.z);
    }

    // Mob position in the sublevel's own space
    public static Vec3 localMobPos(Mob mob) {
        if (!ACTIVE) {
            return mob.position();
        }
        SubLevelAccess subLevel = querySubLevel(mob);
        return subLevel != null ? subLevel.logicalPose().transformPositionInverse(mob.position()) : mob.position();
    }

    // Worldspace position to sublevel space
    public static BlockPos toLocal(Mob mob, BlockPos worldPos) {
        if (!ACTIVE) {
            return worldPos;
        }
        SubLevelAccess subLevel = querySubLevel(mob);
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
        SubLevelAccess subLevel = querySubLevel(mob);
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
            // Path side was local during pathfinding, rotate it to match now worldspace position
            if (node instanceof DirectionalPathPoint) {
                DirectionalPathPoint dp = (DirectionalPathPoint) node;
                Direction worldSide = dp.getPathSide() != null ? toWorldDirection(mob, dp.getPathSide()) : null;
                worldNodes.add(dp.cloneAndMoveWithSide(world.getX(), world.getY(), world.getZ(), worldSide));
            } else {
                worldNodes.add(node.cloneAndMove(world.getX(), world.getY(), world.getZ()));
            }
        }

        BlockPos worldTarget = path.getTarget();
        if (worldTarget != null) {
            worldTarget = BlockPos.containing(pose.transformPosition(Vec3.atCenterOf(worldTarget)));
        }

        return new Path(worldNodes, worldTarget, path.canReach());
    }

    private static SubLevelAccess querySubLevel(Mob mob) {
        if (!ACTIVE) {
            return null;
        }
        try {
            return COMPANION.getTrackingSubLevel(mob);
        } catch (Throwable t) {
            ACTIVE = false;
            Constants.LOG.warn("Sable Companion call failed at runtime, disabling sub level pathfinding compat (found version {}). Update Sable Companion to {} or newer. Error: {}", detectedSableVersion, MIN_SABLE_VERSION, t.toString());
            return null;
        }
    }

    private static boolean versionAtLeast(String current, String minimum) {
        String[] cur = current.split("\\.");
        String[] min = minimum.split("\\.");
        int len = Math.max(cur.length, min.length);
        for (int i = 0; i < len; i++) {
            int c = i < cur.length ? parseIntSafe(cur[i]) : 0;
            int m = i < min.length ? parseIntSafe(min[i]) : 0;
            if (c != m) {
                return c > m;
            }
        }
        return true;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
