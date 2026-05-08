package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import ink.astrius.create_avionics.api.simulated.GimbalSensorExt;
import org.joml.Vector3dc;

import java.util.List;

/**
 * Inertial measurement on the body frame: pitch/roll angles, angular rates,
 * gravity vector, and linear acceleration.
 *
 * @cc.module gimbal_sensor
 */
public class GimbalSensorPeripheral extends SimPeripheral<GimbalSensorBlockEntity> {

    public GimbalSensorPeripheral(final GimbalSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "gimbal_sensor";
    }

    /**
     * Get the gimbal pitch and roll angles in degrees.
     *
     * @return A two-element list {xAngle, zAngle} in degrees.
     */
    @LuaFunction
    public List<Double> getAngles() {
        return List.of(Math.toDegrees(this.blockEntity.getXAngle()), Math.toDegrees(this.blockEntity.getZAngle()));
    }

    /**
     * Get the gimbal pitch and roll angles in radians.
     *
     * @return A two-element list {xAngle, zAngle} in radians.
     */
    @LuaFunction
    public List<Double> getAnglesRad() {
        return List.of(this.blockEntity.getXAngle(), this.blockEntity.getZAngle());
    }

    /**
     * Get body-frame angular velocity in degrees/sec.
     *
     * @return A three-element list {wx, wy, wz} in degrees/sec.
     */
    @LuaFunction
    public List<Double> getAngularRates() {
        final Vector3dc w = ((GimbalSensorExt) this.blockEntity).getAngularVelocityBody();
        return List.of(Math.toDegrees(w.x()), Math.toDegrees(w.y()), Math.toDegrees(w.z()));
    }

    /**
     * Get body-frame angular velocity in radians/sec.
     *
     * @return A three-element list {wx, wy, wz} in radians/sec.
     */
    @LuaFunction
    public List<Double> getAngularRatesRad() {
        final Vector3dc w = ((GimbalSensorExt) this.blockEntity).getAngularVelocityBody();
        return List.of(w.x(), w.y(), w.z());
    }

    /**
     * Get the gravity vector in the body frame.
     *
     * @return A three-element list {gx, gy, gz}.
     */
    @LuaFunction
    public List<Double> getGravity() {
        final Vector3dc g = ((GimbalSensorExt) this.blockEntity).getGravityBody();
        return List.of(g.x(), g.y(), g.z());
    }

    /**
     * Get linear acceleration in the body frame.
     * m/s^2, body-frame, gravity not subtracted; finite-differenced at 20 Hz.
     *
     * @return A three-element list {ax, ay, az}.
     */
    @LuaFunction
    public List<Double> getLinearAcceleration() {
        final Vector3dc a = ((GimbalSensorExt) this.blockEntity).getLinearAccelerationBody();
        return List.of(a.x(), a.y(), a.z());
    }
}
