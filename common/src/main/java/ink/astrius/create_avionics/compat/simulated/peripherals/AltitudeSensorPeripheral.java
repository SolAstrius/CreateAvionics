package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import ink.astrius.create_avionics.api.simulated.AltitudeSensorExt;

/**
 * Reports the sensor's world altitude, local air pressure, and vertical speed.
 * Altitude is the world-frame Y coordinate of the block (projected out of any
 * sub-level), so a sensor on a flying contraption reads the same value as a
 * stationary block at the same global position.
 *
 * @cc.module altitude_sensor
 */
public class AltitudeSensorPeripheral extends SimPeripheral<AltitudeSensorBlockEntity> {

    public AltitudeSensorPeripheral(final AltitudeSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "altitude_sensor";
    }

    /**
     * Get the sensor's current world altitude.
     *
     * @return The block's world-frame Y coordinate, in blocks (= meters in
     *         Minecraft units).
     */
    @LuaFunction
    public double getHeight() {
        return this.blockEntity.getWorldHeight();
    }

    /**
     * Get the local air pressure at the sensor's altitude.
     * Computed as {@code base_pressure × pressure_curve(y)} from the
     * dimension's Sable physics config; the pressure curve is a Bezier function
     * of altitude, so values fall off with height in a dimension-configurable
     * way. Units are dimension-config-defined; in the Overworld defaults the
     * raw value is roughly an atmosphere fraction (the goggle tooltip displays
     * it ×100, suggesting "atm × 100" as the natural reading scale).
     *
     * @return The air pressure in dimension-defined units.
     */
    @LuaFunction
    public double getAirPressure() {
        return this.blockEntity.getAirPressure();
    }

    /**
     * Get the sensor's current vertical speed.
     * Finite-differenced from {@link #getHeight} at the server tick rate
     * (Δheight × 20). Has one tick of lag. Server-side only — returns 0 on
     * the client side or before the second tick after placement.
     *
     * @return The vertical speed in m/s (positive = ascending).
     */
    @LuaFunction
    public double getVerticalSpeed() {
        return ((AltitudeSensorExt) this.blockEntity).getVerticalSpeed();
    }
}
