package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;

public class LaserSensorPeripheral extends SimPeripheral<LaserSensorBlockEntity> {

    public LaserSensorPeripheral(final LaserSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "laser_sensor";
    }

    // Current redstone output 0..15.
    @LuaFunction
    public final int getPower() {
        return this.blockEntity.currentPower;
    }

    // Distance to the closest pointer hit observed last tick.
    // Returns nil when no laser is currently striking the sensor.
    @LuaFunction
    public final Double getClosestHitDistance() {
        final double d = this.blockEntity.closestHitDistance;
        return d == Double.MAX_VALUE ? null : d;
    }
}
