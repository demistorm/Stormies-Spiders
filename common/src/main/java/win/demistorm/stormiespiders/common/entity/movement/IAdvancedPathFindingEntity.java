package win.demistorm.stormiespiders.common.entity.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Node;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface IAdvancedPathFindingEntity {
	// The side on which the entity is currently walking
	public default Direction getGroundSide() {
		return Direction.DOWN;
	}
	
	// Called when the mob tries to move along the path but is obstructed
	public void onPathingObstructed(Direction facing);

	// How many ticks the mob can be stuck before the path is considered obstructed
	public default int getMaxStuckCheckTicks() {
		return 40;
	}

	// Returns the pathing malus for building a bridge
	public default float getBridgePathingMalus(Mob entity, BlockPos pos, @Nullable Node fallPathPoint) {
		return -1.0f;
	}

	// Returns the pathing malus for the given PathType and block position
	// Negative values are avoided at all cost, 0.0 has highest priority
	// Positive values add travel cost, higher values mean less preferred
	// Additional cost increases path length and decreases max path length in blocks
	public default float getPathingMalus(BlockGetter cache, Mob entity, PathType nodeType, BlockPos pos, Vec3i direction, Predicate<Direction> sides) {
		return entity.getPathfindingMalus(nodeType);
	}

	// Called after the path finder has finished finding a path (used to clear caches)
	public default void pathFinderCleanup() {

	}
}
