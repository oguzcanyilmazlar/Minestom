package net.minestom.server.entity.pathfinding;

import net.minestom.server.attribute.Attribute;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO all pathfinding requests could be processed in another thread

/**
 * Necessary object for all {@link NavigableEntity}.
 */
public final class Navigator {
    private final Entity entity;
    private final Pathfinder pathfinder;

    public Navigator(@NotNull Entity entity) {
        this.entity = entity;
        this.pathfinder = new PathfinderImpl(entity);
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
        final double dx = direction.x() - position.x();
        final double dy = direction.y() - position.y();
        final double dz = direction.z() - position.z();
        // the purpose of these few lines is to slow down entities when they reach their destination
        final double distSquared = dx * dx + dy * dy + dz * dz;
        if (speed > distSquared) {
            speed = distSquared;
        }
        final double radians = Math.atan2(dz, dx);
        final double speedX = Math.cos(radians) * speed;
        final double speedY = dy * speed;
        final double speedZ = Math.sin(radians) * speed;
        final float yaw = PositionUtils.getLookYaw(dx, dz);
        final float pitch = PositionUtils.getLookPitch(dx, dy, dz);
        // Prevent ghosting
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
        return null; // TODO
        //return pathfinder.pathPosition;
    }

    public @NotNull Entity getEntity() {
        return entity;
    }

    private float getAttributeValue(@NotNull Attribute attribute) {
        if (entity instanceof LivingEntity) {
            return ((LivingEntity) entity).getAttributeValue(attribute);
        }
        return 0f;
    }
}
