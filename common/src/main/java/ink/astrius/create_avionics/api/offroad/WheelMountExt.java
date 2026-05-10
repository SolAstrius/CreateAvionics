package ink.astrius.create_avionics.api.offroad;

/**
 * Override accessors for a wheel mount. Lets scripts bypass the redstone
 * reads for steering and braking. Each override is independent — the user
 * can drive one axis from script and leave the other on redstone.
 */
public interface WheelMountExt {

    /** True when the steering input is currently script-driven. */
    boolean isSteeringOverridden();

    /**
     * @return The currently-applied steering signal, in [-15, 15]. When
     *         override is active, this is the script-supplied value;
     *         otherwise it is the most recent value computed from redstone.
     *         Positive turns one way, negative the other; matches the
     *         {@code signalLeft - signalRight} convention.
     */
    int getSteeringSignalValue();

    /**
     * Install a steering override. Skips the side-redstone reads and uses
     * {@code signal} (-15..15) for steering computations until cleared.
     */
    void setSteeringOverride(int signal);

    /** Release the steering override; redstone reads resume. */
    void clearSteeringOverride();

    /** True when the brake input is currently script-driven. */
    boolean isBrakeOverridden();

    /**
     * @return The currently-applied brake signal, in [0, 15]. Override value
     *         when active, otherwise the live redstone-on-top read.
     */
    int getBrakeSignalValue();

    /**
     * Install a brake override. Skips the top-of-block redstone read and
     * uses {@code signal} (0..15) for both server-side physics and the
     * client-side visual until cleared.
     */
    void setBrakeOverride(int signal);

    /** Release the brake override; redstone reads resume. */
    void clearBrakeOverride();

    /** Live suspension extension distance (0 fully compressed, larger = wheel hangs lower). */
    double getExtension();

    /** Live angular velocity of the wheel (radians per tick). */
    double getAngularVelocity();

    /** Friction coefficient of the block currently under the wheel; 1.0 when airborne. */
    double getTouchingFriction();

    /** True when the wheel is detached from terrain (max extension exceeded). */
    boolean isLiftedUp();
}
