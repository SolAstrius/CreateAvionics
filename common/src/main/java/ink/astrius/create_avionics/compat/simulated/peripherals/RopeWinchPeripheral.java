package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;

/**
 * A kinetic rope winch. Reports current/min/max extension and the per-tick
 * movement speed driven by its kinetic input.
 *
 * @cc.module rope_winch
 */
public class RopeWinchPeripheral extends SimKineticPeripheral<RopeWinchBlockEntity> {

    public RopeWinchPeripheral(final RopeWinchBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "rope_winch";
    }

    /**
     * Get the current rope extension.
     *
     * @return Current rope extension in blocks.
     */
    @LuaFunction
    public final int getLength() {
        return this.blockEntity.getCurrentValue();
    }

    /**
     * Get the maximum rope extension.
     *
     * @return Maximum rope extension in blocks.
     */
    @LuaFunction
    public final int getMaxLength() {
        return this.blockEntity.getMaxValue();
    }

    /**
     * Get the minimum rope extension.
     *
     * @return Minimum rope extension in blocks.
     */
    @LuaFunction
    public final int getMinLength() {
        return this.blockEntity.getMinValue();
    }

    /**
     * Get the winch's per-tick extension delta.
     * Per-tick extension delta. Positive extends, negative retracts. Driven
     * by kinetic input speed; the winch has no external setpoint, so to
     * command rope length a script must drive the kinetics (e.g., via an
     * analog_transmission or steering_wheel-coupled gearbox).
     *
     * @return The signed movement speed.
     */
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getMovementSpeed();
    }

}
