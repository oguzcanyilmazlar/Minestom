package net.minestom.server.entity.pathfinding;

import com.extollit.gaming.ai.path.HydrazinePathFinder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.utils.chunk.ChunkUtils;

import java.util.ArrayList;
import java.util.List;

final class HydraPathImpl implements Pathfinder {
    private final Entity entity;
    final PFPathingEntity pathingEntity;
    HydrazinePathFinder pathFinder;


    volatile Point pathPosition;
    volatile List<Point> path;

    HydraPathImpl(Navigator navigator) {
        this.entity = navigator.getEntity();
        this.pathingEntity = new PFPathingEntity(navigator);
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
    public void updatePath(Point point) {
        if (point != null && pathPosition != null && point.samePoint(pathPosition)) {
            // Tried to set path to the same target position
            return;
        }
        final Instance instance = entity.getInstance();
        if (pathFinder == null) {
            // Unexpected error
            return;
        }
        this.pathFinder.reset();
        if (point == null) {
            return;
        }
        // Can't path with a null instance.
        if (instance == null) {
            return;
        }
        // Can't path outside the world border
        final WorldBorder worldBorder = instance.getWorldBorder();
        if (!worldBorder.isInside(point)) {
            return;
        }
        // Can't path in an unloaded chunk
        final Chunk chunk = instance.getChunkAt(point);
        if (!ChunkUtils.isLoaded(chunk)) {
            return;
        }
        var path = pathFinder.computePathTo(point.x(), point.y(), point.z());
        if (path == null) {
            return;
        }

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < path.length(); i++) {
            var node = path.at(i);
            var coordinates = node.coordinates();
            points.add(new Vec(coordinates.x, coordinates.y, coordinates.z));
        }

        this.pathPosition = point;
        this.path = points;
    }

    @Override
    public List<Point> forcePath(Point target) {
        updatePath(target);
        return path;
    }

    private void reset() {
        this.pathPosition = null;
        this.pathFinder.reset();
    }
}
