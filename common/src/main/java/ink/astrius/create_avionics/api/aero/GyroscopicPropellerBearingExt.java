package ink.astrius.create_avionics.api.aero;

import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

/**
 * Override-mode accessors for the gyroscopic propeller bearing. Lets scripts
 * substitute a fixed target direction for the bearing's automatic gravity
 * tracking. The bearing's 12° cone clamp, redstone power gate, and
 * stabilization-strength gate all still apply on top of the override.
 */
public interface GyroscopicPropellerBearingExt {

    /**
     * Install a manual target direction (world frame). Replaces the
     * gravity-derived target the bearing would otherwise compute each tick.
     * The vector is normalized internally; callers may pass any non-zero
     * finite direction.
     *
     * @throws IllegalArgumentException if any component is non-finite or
     *         the vector is zero-length.
     */
    void setManualTarget(Vector3dc target);

    /**
     * Remove any manual target, returning the bearing to its default
     * gravity-tracking behavior.
     */
    void clearManualTarget();

    /**
     * @return The current manual target direction, or {@code null} if the
     *         bearing is in default gravity-tracking mode.
     */
    @Nullable
    Vector3dc getManualTarget();
}
