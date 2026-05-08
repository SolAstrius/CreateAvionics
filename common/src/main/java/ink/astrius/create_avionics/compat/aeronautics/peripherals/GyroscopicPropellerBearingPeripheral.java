package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import org.joml.Vector3d;

import java.util.List;

/**
 * A propeller bearing whose thrust axis can be tilted at runtime. Adds smooth
 * and strict-axis tilt commands on top of the standard propeller bearing API.
 *
 * @cc.module gyroscopic_propeller_bearing
 */
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

    /**
     * Smoothly tilt the bearing axis from the current direction to a target
     * over `time` seconds. Both vectors must be unit-length world-frame.
     * <p>Yields until the next server tick (the tilt animation itself runs
     * on the server thread for `time` seconds — this method only blocks for
     * the dispatch tick, not the duration).
     *
     * @param targetDirection World-frame target direction (unit-length, 3 elements).
     * @param currentDirection World-frame current direction (unit-length, 3 elements).
     * @param time Tilt duration in seconds.
     */
    @LuaFunction(mainThread = true)
    public final void setTilt(final List<Double> targetDirection, final List<Double> currentDirection, final double time) throws LuaException {
        this.blockEntity.setTilt(toVec(targetDirection), toVec(currentDirection), time);
    }

    /**
     * Strict-axis tilt: rotate around `axis` by `angle` radians over `time`
     * seconds. The axis is in world frame.
     * <p>Yields until the next server tick. As with {@link #setTilt}, the
     * animation duration runs on the server thread; this method only blocks
     * for the dispatch tick.
     *
     * @param axis World-frame axis to rotate around (3 elements).
     * @param angle Rotation angle in radians.
     * @param time Tilt duration in seconds.
     */
    @LuaFunction(mainThread = true)
    public final void setStrictTilt(final List<Double> axis, final double angle, final double time) throws LuaException {
        this.blockEntity.setStrictTilt(toVec(axis), angle, time);
    }
}
