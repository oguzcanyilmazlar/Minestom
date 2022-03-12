package net.minestom.server.entity.pathfinding;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A pathfinder is used to maintain a dynamic path to a target.
 * This dynamic path may change target, or be cancelled with {@link Pathfinder#updatePath(Point)}.
 * It may be forcibly generated with {@link Pathfinder#forcePath(Point)}.
 * And you may query the path with {@link Pathfinder#nextPoint(Point)}.
 * <br><br>
 * Implementations of this interface may be threaded, as long as the interface methods are thread-safe.
 */
public interface Pathfinder {
    /**
     * This query will return the next point to reach the target, from the current position.
     * @param currentPoint the current position
     * @return the next point to reach the target, or null if a path does not currently exist
     */
    @Nullable Point nextPoint(@NotNull Point currentPoint);

    /**
     * This method will update the path to the given target, starting a new path if none exists.
     * @param target the new target, or null to cancel the path
     */
    void updatePath(@Nullable Point target);

    /**
     * This method will force the path to the given target.
     * @param target the new target
     * @return the list of points to reach the target
     */
    @Nullable List<@NotNull Point> forcePath(Point target);
}
