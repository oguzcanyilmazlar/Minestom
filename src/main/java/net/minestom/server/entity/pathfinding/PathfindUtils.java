package net.minestom.server.entity.pathfinding;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.NotNull;

final class PathfindUtils {
    public static boolean isBlocked(@NotNull Point point, @NotNull BoundingBox box,
                                    @NotNull Block.Getter getter) {
        Point relStart = box.relativeStart();
        Point relEnd = box.relativeEnd();

        // Double for some reason (Necessary, I don't know why)
        relStart = relStart.mul(2, 0, 2);
        relEnd = relEnd.mul(2, 0, 2);

        // Add a little padding so pathfinding is not against the very edge of the block
        relStart = relStart.add(-0.1, 0, -0.1);
        relEnd = relEnd.add(0.1, 0, 0.1);

        // TODO: Use BlockIterator instead
        return getter.getBlock(point.add(relStart.x(), relStart.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relStart.x(), relStart.y(), relEnd.z())).isSolid() ||
                getter.getBlock(point.add(relStart.x(), relEnd.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relStart.x(), relEnd.y(), relEnd.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relStart.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relStart.y(), relEnd.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relEnd.y(), relStart.z())).isSolid() ||
                getter.getBlock(point.add(relEnd.x(), relEnd.y(), relEnd.z())).isSolid();
    }

    public static void debugParticle(@NotNull Point point, @NotNull Particle particle) {
        // TODO: Remove this debug
        PacketUtils.broadcastPacket(ParticleCreator.createParticlePacket(
                particle, point.x(), point.y(), point.z(),
                0, 0, 0, 1));
    }
}
