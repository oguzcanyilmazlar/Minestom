package net.minestom.server.entity.pathfinding;

import net.minestom.server.coordinate.Point;

import java.util.List;

public interface Pathfinder {
    Point nextPoint(Point currentPoint);

    void updatePath(Point target);

    List<Point> forcePath(Point target);
}
