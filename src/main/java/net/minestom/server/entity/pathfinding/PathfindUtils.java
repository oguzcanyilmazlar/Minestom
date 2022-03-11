package net.minestom.server.entity.pathfinding;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

final class PathfindUtils {
    public static boolean isBlocked(@NotNull Point point, @NotNull BoundingBox box,
                                    @NotNull Block.Getter getter, double entityPadding) {
        Point relStart = box.relativeStart();
        Point relEnd = box.relativeEnd();
        relStart = relStart.mul(2, 0, 2).sub(entityPadding, 0, entityPadding);
        relEnd = relEnd.mul(2, 0, 2).add(entityPadding, 0, entityPadding);
        return getter.getBlock(point.add(relStart.x(), relStart.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relStart.x(), relStart.y(), relEnd.z())).isSolid() ||
                getter.getBlock(point.add(relStart.x(), relEnd.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relStart.x(), relEnd.y(), relEnd.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relStart.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relStart.y(), relEnd.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relEnd.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relEnd.y(), relEnd.z())).isSolid();
    }
}
