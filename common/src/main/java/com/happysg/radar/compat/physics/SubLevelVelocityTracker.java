package com.happysg.radar.compat.physics;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-sublevel velocity caching with optional exponential-moving-average
 * smoothing. Replaces the VS2 ship-velocity tracker used for cannon
 * lead computation; semantics and memory model are the same, the API
 * just keys on a Sable {@link SubLevelAccess}'s UUID rather than a
 * VS2 ship's long id.
 *
 * Sable's {@code RigidBodyHandle.getLinearVelocity} returns velocity in
 * blocks/tick already, so unlike VS2 there is no per-second→per-tick
 * conversion needed.
 */
public final class SubLevelVelocityTracker {

    private static final Map<UUID, Vec3> LAST_VEL_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> SMOOTHED_VEL_TICK = new ConcurrentHashMap<>();

    private SubLevelVelocityTracker() {}

    public static Vec3 getVelocityPerTick(final SubLevelAccess subLevel) {
        if (!(subLevel instanceof final ServerSubLevel server)) return Vec3.ZERO;

        final RigidBodyHandle body = RigidBodyHandle.of(server);
        final Vector3d v = body.getLinearVelocity(new Vector3d());
        final Vec3 vel = new Vec3(v.x, v.y, v.z);

        LAST_VEL_TICK.put(subLevel.getUniqueId(), vel);
        return vel;
    }

    public static Vec3 getVelocityPerTickSmoothed(final SubLevelAccess subLevel, final double alpha) {
        if (subLevel == null) return Vec3.ZERO;

        final Vec3 raw = getVelocityPerTick(subLevel);
        final UUID id = subLevel.getUniqueId();

        final Vec3 prev = SMOOTHED_VEL_TICK.get(id);
        if (prev == null) {
            SMOOTHED_VEL_TICK.put(id, raw);
            return raw;
        }
        final Vec3 smoothed = prev.scale(alpha).add(raw.scale(1.0 - alpha));
        SMOOTHED_VEL_TICK.put(id, smoothed);
        return smoothed;
    }

    public static Vec3 getLastVelocityPerTick(final UUID subLevelId) {
        return LAST_VEL_TICK.getOrDefault(subLevelId, Vec3.ZERO);
    }

    public static void clear(final UUID subLevelId) {
        LAST_VEL_TICK.remove(subLevelId);
        SMOOTHED_VEL_TICK.remove(subLevelId);
    }
}
