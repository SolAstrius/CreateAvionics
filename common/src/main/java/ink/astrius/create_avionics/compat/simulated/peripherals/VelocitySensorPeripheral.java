package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;

/**
 * A directional velocity sensor. Reports the body-frame velocity component
 * along its mounted axis.
 *
 * @cc.module velocity_sensor
 */
public class VelocitySensorPeripheral extends SimPeripheral<VelocitySensorBlockEntity> {

    public VelocitySensorPeripheral(final VelocitySensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "velocity_sensor";
    }

    /**
     * Get the velocity component along the sensor's axis.
     *
     * @return The signed velocity.
     */
    @LuaFunction
    public double getVelocity() {
        return this.blockEntity.getAdjustedVelocity();
    }

    /**
     * Get the sensor's mounted axis.
     * Body-frame axis the sensor measures along: "x", "y", or "z". Lets Lua
     * distinguish three orthogonally-mounted sensors to build a body-frame
     * velocity vector.
     *
     * @return The axis as "x", "y", or "z".
     */
    @LuaFunction
    public String getAxis() {
        return AbstractDirectionalAxisBlock.getAxis(this.blockEntity.getBlockState()).getSerializedName();
    }
}
