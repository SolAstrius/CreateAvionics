package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;

public class VelocitySensorPeripheral extends SimPeripheral<VelocitySensorBlockEntity> {

    public VelocitySensorPeripheral(final VelocitySensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "velocity_sensor";
    }

    @LuaFunction
    public double getVelocity() {
        return this.blockEntity.getAdjustedVelocity();
    }

    // Body-frame axis the sensor measures along: "x", "y", or "z". Lets Lua
    // distinguish three orthogonally-mounted sensors to build a body-frame
    // velocity vector.
    @LuaFunction
    public String getAxis() {
        return AbstractDirectionalAxisBlock.getAxis(this.blockEntity.getBlockState()).getSerializedName();
    }
}
