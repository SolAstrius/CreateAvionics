package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import ink.astrius.create_avionics.api.simulated.AltitudeSensorExt;

/**
 * Reports the sensor's world altitude, local air pressure, and vertical speed.
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
     * Get the sensor's current world height.
     *
     * @return The world height.
     */
    @LuaFunction
    public double getHeight() {
        return this.blockEntity.getWorldHeight();
    }

    /**
     * Get the local air pressure at the sensor.
     *
     * @return The air pressure.
     */
    @LuaFunction
    public double getAirPressure() {
        return this.blockEntity.getAirPressure();
    }

    /**
     * Get the sensor's current vertical speed.
     *
     * @return The vertical speed.
     */
    @LuaFunction
    public double getVerticalSpeed() {
        return ((AltitudeSensorExt) this.blockEntity).getVerticalSpeed();
    }
}
