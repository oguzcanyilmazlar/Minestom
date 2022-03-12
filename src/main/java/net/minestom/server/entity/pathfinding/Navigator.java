package net.minestom.server.entity.pathfinding;

import net.minestom.server.attribute.Attribute;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Necessary object for all {@link NavigableEntity}.
 */
public final class Navigator {
    private final Entity entity;
    private final Pathfinder pathfinder;

    public Navigator(@NotNull Entity entity) {
        this.entity = entity;
        this.pathfinder = new PathfinderImpl(entity, null, null);
    }

    /**
     * Used to move the entity toward {@code direction} in the X and Z axis
     * Gravity is still applied but the entity will not attempt to jump
     * Also update the yaw/pitch of the entity to look along 'direction'
     *
     * @param direction the targeted position
     * @param speed     define how far the entity will move
     */
    public void moveTowards(@NotNull Point direction, double speed) {
        final Pos position = entity.getPosition();

        // Find the direction
        double dx = direction.x() - position.x();
        double dy = direction.y() - position.y();
        double dz = direction.z() - position.z();

        // the purpose of these few lines is to slow down entities when they reach their destination
        double distSquared = dx * dx + dy * dy + dz * dz;
        if (speed > distSquared) {
            speed = distSquared;
        }
        // Find the movement speed
        double radians = Math.atan2(dz, dx);
        double speedX = Math.cos(radians) * speed;
        double speedY = dy * speed;
        double speedZ = Math.sin(radians) * speed;

        // Now calculate the new yaw/pitch
        float oldYaw = position.yaw();
        float oldPitch = position.pitch();
        float newYaw = PositionUtils.getLookYaw(dx, dz);
        float newPitch = PositionUtils.getLookPitch(dx, dy, dz);

        // Average the pitch and yaw to avoid jittering
        float yaw = PositionUtils.averageYaw(PositionUtils.averageYaw(oldYaw, newYaw), oldYaw);
        float pitch = PositionUtils.averagePitch(PositionUtils.averagePitch(oldPitch, newPitch), oldPitch);

        // Prevent ghosting, and refresh position
        final var physicsResult = CollisionUtils.handlePhysics(entity, new Vec(speedX, speedY, speedZ));
        this.entity.refreshPosition(physicsResult.newPosition().withView(yaw, pitch));
    }

    public void jump(float height) {
        // FIXME magic value
        this.entity.setVelocity(new Vec(0, height * 2.5f, 0));
    }

    public void setPathTo(@Nullable Point point) {
        this.pathfinder.updatePath(point);
    }

    @ApiStatus.Internal
    public synchronized void tick() {
        if (entity instanceof LivingEntity && ((LivingEntity) entity).isDead())
            return; // No pathfinding tick for dead entities
        final Point next = this.pathfinder.nextPoint(entity.getPosition());
        if (next != null) {
            moveTowards(next, getAttributeValue(Attribute.MOVEMENT_SPEED));
            PathfindUtils.debugParticle(next, Particle.WHITE_ASH);
            final double entityY = entity.getPosition().y();
            if (entityY < next.y()) {
                jump(1);
            }
        }
    }

    /**
     * Gets the target pathfinder position.
     *
     * @return the target pathfinder position, null if there is no one
     */
    public @Nullable Point getPathPosition() {
        return this.pathfinder.nextPoint(entity.getPosition());
    }

    private float getAttributeValue(@NotNull Attribute attribute) {
        if (entity instanceof LivingEntity) {
            return ((LivingEntity) entity).getAttributeValue(attribute);
        }
        return attribute.defaultValue();
    }
}
