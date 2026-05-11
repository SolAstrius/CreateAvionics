package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;

/**
 * A directional velocity sensor. Reports the host contraption's linear
 * velocity component along the body-frame axis the block is mounted on.
 * <p>
 * <b>Axis convention.</b> Each sensor is mounted along one body-frame axis
 * (set at place-time, not affected by sub-level rotation). At sub-level
 * identity orientation, body axes equal world axes (body +X = east, +Y = up,
 * +Z = south). Internally the sensor projects the contraption's global
 * velocity onto its mounted axis direction (transformed into world frame),
 * so three orthogonally-mounted sensors give the contraption's velocity in
 * its own body frame.
 *
 * @cc.module velocity_sensor
 */
public class VelocitySensorPeripheral extends dev.simulated_team.simulated.compat.computercraft.peripherals.VelocitySensorPeripheral {

    public VelocitySensorPeripheral(final VelocitySensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    /**
     * Get the velocity component along the sensor's mounted axis.
     * Sign: positive when the contraption is moving along the axis-positive
     * direction (e.g. east for an X-mounted sensor on an unrotated contraption).
     * Has a deadband: returns 0 if the projected component's magnitude is
     * below 0.05 m/s. When the host is not on a sub-level (sitting on
     * stationary ground), returns 0.
     *
     * @return The signed velocity in m/s.
     */
    @Override
    @LuaFunction
    public float getVelocity() {
        return super.getVelocity();
    }

    /**
     * Get the body-frame axis the sensor measures along.
     * Lets a Lua script distinguish three orthogonally-mounted sensors to
     * reconstruct the contraption's body-frame velocity vector
     * {@code (vx, vy, vz)}. The axis label is fixed at place-time; the
     * direction it represents in world frame rotates with the contraption.
     *
     * @return The axis as "x", "y", or "z".
     */
    @LuaFunction
    public String getAxis() {
        return AbstractDirectionalAxisBlock.getAxis(this.blockEntity.getBlockState()).getSerializedName();
    }
}
