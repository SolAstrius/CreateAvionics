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
     * <p>
     * Unit is dimensionless "atmosphere fraction": 1.0 = sea-level pressure,
     * 0.0 = vacuum (top of build height). The goggle tooltip displays this
     * value as a percentage ({@code 1.00 → "100.00%"}).
     * <p>
     * Computed as {@code base_pressure × pressure_curve(y)} where on the
     * Overworld defaults {@code base_pressure = 1.0} and the curve follows
     * {@code exp(-0.004 × (y - seaLevel))} above sea level (so a 1/e pressure
     * drop every ~250 blocks of altitude), clamped to a maximum of 1.5 below
     * sea level, and tapering to 0 at {@code logicalHeight}. Per-dimension
     * curves can override this via Sable's physics config.
     *
     * @return The air pressure as a fraction of sea-level pressure.
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
