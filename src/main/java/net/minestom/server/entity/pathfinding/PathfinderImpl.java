package net.minestom.server.entity.pathfinding;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

final class PathfinderImpl implements Pathfinder {
    private static final double DELTA = 0.01;
    private final Entity entity;

    volatile Point pathPosition;
    volatile List<Point> path;

    PathfinderImpl(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Point nextPoint(Point currentPoint) {
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
    public void updatePath(Point target) {
        var start = entity.getPosition();
        var result = findPath(start, target, 1);
        this.path = result.stream().toList();
        this.pathPosition = target;
    }

    @Override
    public List<Point> forcePath(Point target) {
        updatePath(target);
        return path;
    }

    @Nullable Queue<Point> findPath(Point start, Point goal, double step) {
        Comparator<Point> distanceCost = Comparator.comparingDouble(p ->
                p.distance(start) +
                        p.distance(goal) +
                        getCost(p, p)
        );
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
            PacketUtils.broadcastPacket(ParticleCreator.createParticlePacket(
                    Particle.FLAME, current.x(), current.y(), current.z(),
                    0, 0, 0, 1));

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
                if (isBlocked(neighbor)) {
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

    private static Queue<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        Deque<Point> path = new ArrayDeque<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.addFirst(current);
        }
        return path;
    }

    private static Point[] neighbors(Point point, double step) {
        return new Point[]{
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

    public double getCost(Point from, Point to) {
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
    }

    public boolean isBlocked(Point point) {
        Instance instance = entity.getInstance();
        Objects.requireNonNull(instance, "The navigator must be in an instance while pathfinding.");
        return PathfindUtils.isBlocked(point, entity.getBoundingBox(), instance, 0.1);
    }
}
