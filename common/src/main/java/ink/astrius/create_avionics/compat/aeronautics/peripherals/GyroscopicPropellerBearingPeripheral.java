package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import org.joml.Vector3d;

import java.util.List;

public class GyroscopicPropellerBearingPeripheral extends PropellerBearingPeripheral<GyroscopicPropellerBearingBlockEntity> {

    public GyroscopicPropellerBearingPeripheral(final GyroscopicPropellerBearingBlockEntity blockEntity) {
        super(blockEntity, "gyroscopic_propeller_bearing");
    }

    private static Vector3d toVec(final List<Double> list) throws LuaException {
        if (list == null || list.size() != 3) {
            throw new LuaException("expected a 3-element list of numbers");
        }
        return new Vector3d(list.get(0), list.get(1), list.get(2));
    }

    // Smoothly tilt the bearing axis from the current direction to a target
    // over `time` seconds. Both vectors must be unit-length world-frame.
    @LuaFunction(mainThread = true)
    public final void setTilt(final List<Double> targetDirection, final List<Double> currentDirection, final double time) throws LuaException {
        this.blockEntity.setTilt(toVec(targetDirection), toVec(currentDirection), time);
    }

    // Strict-axis tilt: rotate around `axis` by `angle` radians over `time`
    // seconds. The axis is in world frame.
    @LuaFunction(mainThread = true)
    public final void setStrictTilt(final List<Double> axis, final double angle, final double time) throws LuaException {
        this.blockEntity.setStrictTilt(toVec(axis), angle, time);
    }
}
