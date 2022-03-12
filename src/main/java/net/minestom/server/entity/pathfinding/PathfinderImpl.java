package net.minestom.server.entity.pathfinding;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;
import net.minestom.server.utils.block.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A simplified, synchronized A* pathfinder.
 * This pathfinder is focussed on stability, not performance.
 * <p>
 *     If you would like to limit the entity to a
 * </p>
 */
final class PathfinderImpl implements Pathfinder {
    // The delta is used as a failsafe to avoid incorrect pathfinding, generally 0.01 is enough
    private static final double DELTA = 0.01;

    private final @NotNull Entity entity;
    private final @NotNull BlockedPredicate blocked;
    private final @NotNull CostFunction cost;

    volatile Point pathPosition;
    volatile List<Point> path;

    /**
     * Creates an instance of this pathfinder.
     * @param entity the entity to find a path for
     * @param blocked the predicate to check if a block is blocked, {@link BlockedPredicate#BLOCK_SOLID_BLOCKS} if null
     * @param cost the cost function to calculate the cost of a block, {@link CostFunction#BLOCK_SPEED_FACTOR} if null
     */
    PathfinderImpl(@NotNull Entity entity, @Nullable BlockedPredicate blocked, @Nullable CostFunction cost) {
        this.entity = entity;
        this.blocked = blocked == null ? BlockedPredicate.BLOCK_SOLID_BLOCKS : blocked;
        this.cost = cost == null ? CostFunction.BLOCK_SPEED_FACTOR : cost;
    }

    @Override
    public Point nextPoint(@NotNull Point currentPoint) {
        var path = this.path;
        if (path == null || path.isEmpty()) {
            return null;
        }

        // Find the closest point to `pathPosition` in the path that is also closer to `currentPoint`
        var closestPoint = path.get(0);
        var closestDistance = currentPoint.distance(closestPoint);
        int index = 0;
        for (var point : path) {
            var distance = currentPoint.distance(point);
            if (distance < closestDistance) {
                closestDistance = distance;
                index = path.indexOf(point);
            }
        }
        if (index < path.size() - 1) {
            return path.get(index + 1);
        } else return null;
    }

    @Override
    public void updatePath(@Nullable Point target) {
        var start = entity.getPosition();
        this.path = findPath(start, target);
        this.pathPosition = target;
    }

    @Override
    public @Nullable List<Point> forcePath(Point target) {
        updatePath(target);
        return path;
    }

    @Nullable List<Point> findPath(Point start, Point goal) {
        // The step is half of the lowest dimension of the entity's bounding box
        // This is used so that the entity will never be able to skip over any point in the path
        BoundingBox box = entity.getBoundingBox();
        double step = Math.min(box.width(), Math.min(box.height(), box.depth())) / 2;

        // The distance cost is the distance between the current point and the goal, plus the cost of the current point
        Comparator<Point> distanceCost = Comparator.comparingDouble(p ->
                p.distance(start) + p.distance(goal) + cost.getCost(entity, p, p));

        // The queue of nodes to be evaluated next
        Queue<Point> next = new PriorityQueue<>(distanceCost);
        Set<Point> nextSet = new HashSet<>();
        next.add(start);

        // The set of nodes already evaluated
        Set<Point> closedSet = new HashSet<>();

        // The map from each node to its parent node
        Map<Point, Point> cameFrom = new HashMap<>();

        while (!next.isEmpty()) {
            Point current = next.remove();
            nextSet.remove(current);

            // TODO: Remove this debug
            PathfindUtils.debugParticle(current, Particle.SMOKE);

            // Return if the current node is the goal
            if (current.distance(goal) - DELTA <= step) {
                return reconstructPath(cameFrom, current);
            }

            // Else, look at the neighbors
            for (Point neighbor : neighbors(current, step)) {
                // If the neighbor is already evaluated, or scheduled for evaluation, skip it
                if (closedSet.contains(neighbor) || nextSet.contains(neighbor)) {
                    continue;
                }

                // If the neighbor is not walkable, skip it
                if (blocked.test(entity, current, neighbor)) {
                    continue;
                }

                // Else, add it to the queue
                next.add(neighbor);
                nextSet.add(neighbor);
                cameFrom.put(neighbor, current);
            }

            // Mark the current node as visited
            closedSet.add(current);
        }
        // No path found
        return null;
    }

    private static List<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        Deque<Point> path = new ArrayDeque<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.addFirst(current);
        }
        for (Point point : path) {
            PathfindUtils.debugParticle(point, Particle.FLAME);
        }
        return List.copyOf(path);
    }

    private static Point[] neighbors(Point point, double step) {
        return new Point[] {
                // Direct neighbors
                point.add(step, 0, 0),
                point.add(-step, 0, 0),
                point.add(0, step, 0),
                point.add(0, -step, 0),
                point.add(0, 0, step),
                point.add(0, 0, -step),

                // Diagonal neighbors
                point.add(step, step, 0),
                point.add(-step, -step, 0),
                point.add(step, -step, 0),
                point.add(-step, step, 0),
                point.add(0, step, step),
                point.add(0, -step, -step),
                point.add(step, 0, step),
                point.add(-step, 0, -step),

                // Diagonal Diagonal neighbors
                point.add(step, step, step),
                point.add(-step, -step, -step),
                point.add(step, -step, -step),
                point.add(-step, step, -step),
                point.add(step, step, -step),
                point.add(-step, -step, step),
                point.add(step, -step, step),
                point.add(-step, step, step)
        };
    }

    public interface BlockedPredicate {
        /**
         * A predicate used as default that blocks movement if any of the blocks are solid.
         */
        BlockedPredicate BLOCK_SOLID_BLOCKS = (entity, from, to) -> {
            Instance instance = entity.getInstance();
            Objects.requireNonNull(instance, "The navigator must be in an instance while pathfinding.");
            return PathfindUtils.isBlocked(to, entity.getBoundingBox(), instance);
        };

        /**
         * Returns true if the given entity cannot move between the two points, false otherwise.
         * @param entity The entity to check.
         * @param from The starting point.
         * @param to The ending point.
         * @return True if the entity cannot move between the two points, false otherwise.
         */
        boolean test(@NotNull Entity entity, @NotNull Point from, @NotNull Point to);

        /**
         * Combines this predicate with another one.
         * @param other The other predicate.
         * @return A new predicate that returns true if either this or the other predicate returns true.
         */
        default @NotNull BlockedPredicate combine(@NotNull BlockedPredicate other) {
            return (entity, from, to) -> test(entity, from, to) || other.test(entity, from, to);
        }
    }

    public interface CostFunction {
        /**
         * A cost function used as default that returns the block speed factor
         */
        CostFunction BLOCK_SPEED_FACTOR = (entity, from, to) -> {
            // TODO: Implement line intersection algorithm to determine the cost
            // The current algorithm is flawed and may tell the navigator to move through very
            // specific corners that are not actually possible
            Instance instance = entity.getInstance();
            Objects.requireNonNull(instance, "The navigator must be in an instance while pathfinding.");
            Block block = instance.getBlock(to);
            if (block.isSolid()) {
                return Double.POSITIVE_INFINITY;
            }
            return block.registry().speedFactor();
        };

        /**
         * Returns the cost of moving from one point to another.
         * @param from The starting point.
         * @param to The ending point.
         * @return The cost of moving from one point to another.
         */
        double getCost(@NotNull Entity entity, @NotNull Point from, @NotNull Point to);

        /**
         * Combines this cost function with another cost function.
         * @param other The other cost function.
         * @return A new cost function that combines this cost function with the other cost function.
         */
        default @NotNull CostFunction combine(@NotNull CostFunction other) {
            return (entity, from, to) -> getCost(entity, from, to) + other.getCost(entity, from, to);
        }
    }
}
