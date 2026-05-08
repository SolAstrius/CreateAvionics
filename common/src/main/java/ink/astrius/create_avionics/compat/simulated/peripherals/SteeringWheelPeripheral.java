package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;

/**
 * A pilot steering wheel. Reports whether it is held, the current and target
 * angles, and the configured maximum deflection.
 *
 * @cc.module steering_wheel
 */
public class SteeringWheelPeripheral extends SimPeripheral<SteeringWheelBlockEntity> {

    public SteeringWheelPeripheral(final SteeringWheelBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "steering_wheel";
    }

    /**
     * Check whether a player is currently holding the wheel.
     *
     * @return True if held.
     */
    @LuaFunction
    public boolean isHeld() {
        return this.blockEntity.held;
    }

    /**
     * Get the current visible wheel angle.
     *
     * @return Current visible wheel angle, degrees. Bounded by ±getMaxAngle().
     */
    @LuaFunction
    public double getAngle() {
        return this.blockEntity.getAngle();
    }

    /**
     * Get the current visible wheel angle, in radians.
     *
     * @return The current angle in radians.
     */
    @LuaFunction
    public double getAngleRad() {
        return Math.toRadians(this.blockEntity.getAngle());
    }

    /**
     * Get the angle the pilot is commanding.
     * Angle the pilot is currently commanding (what they're steering toward).
     *
     * @return The target angle in degrees.
     */
    @LuaFunction
    public double getTargetAngle() {
        return this.blockEntity.targetAngle;
    }

    /**
     * Get the angle the pilot is commanding, in radians.
     *
     * @return The target angle in radians.
     */
    @LuaFunction
    public double getTargetAngleRad() {
        return Math.toRadians(this.blockEntity.targetAngle);
    }

    /**
     * Get the wheel's maximum deflection.
     *
     * @return Maximum deflection in each direction, degrees. Set by the block's scroll value (1..360).
     */
    @LuaFunction
    public int getMaxAngle() {
        return this.blockEntity.angleInput.getValue();
    }

    /**
     * Get the current angle as a normalized fraction of max deflection.
     * Normalized to [-1, +1]. Convenient for mixing pilot input into autopilot commands.
     *
     * @return The normalized angle in [-1, +1].
     */
    @LuaFunction
    public double getNormalizedAngle() {
        final int max = this.blockEntity.angleInput.getValue();
        return max == 0 ? 0 : this.blockEntity.getAngle() / (float) max;
    }
}
