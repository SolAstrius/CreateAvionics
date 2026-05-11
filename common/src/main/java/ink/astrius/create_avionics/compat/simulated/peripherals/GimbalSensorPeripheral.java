package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import ink.astrius.create_avionics.api.simulated.GimbalSensorExt;
import org.joml.Vector3dc;

import java.util.List;

/**
 * Inertial measurement on the body frame: pitch/roll angles, angular rates,
 * gravity vector, and linear acceleration.
 * <p>
 * <b>Body frame.</b> "Body frame" means the host Sable sub-level's (i.e. the
 * contraption's) frame, not the block's own local frame. At identity sub-level
 * orientation, body axes equal world axes:
 * <ul>
 *   <li>body +X = world +X (Minecraft east)</li>
 *   <li>body +Y = world +Y (up)</li>
 *   <li>body +Z = world +Z (Minecraft south)</li>
 * </ul>
 * As the contraption rotates, body axes rotate with it. The block's mounting
 * orientation does not affect any reading — two gimbal sensors placed in
 * different orientations on the same contraption return identical values.
 *
 * @cc.module gimbal_sensor
 */
public class GimbalSensorPeripheral extends dev.simulated_team.simulated.compat.computercraft.peripherals.GimbalSensorPeripheral {

    public GimbalSensorPeripheral(final GimbalSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    /**
     * Get the contraption's pitch and roll angles in degrees.
     * Both are derived from where world-down points in the body frame:
     * <ul>
     *   <li>{@code xAngle} — rotation about body-X (pitch). 0° = body-Y aligned with world-up.</li>
     *   <li>{@code zAngle} — rotation about body-Z (roll). 0° = body-Y aligned with world-up.</li>
     * </ul>
     * Yaw is not measurable from gravity alone and is not reported here; use
     * {@code navigation_table.getHeading()} for yaw.
     *
     * @return A two-element list {pitch, roll} in degrees.
     */
    @Override
    @LuaFunction
    public List<Double> getAngles() {
        return super.getAngles();
    }

    /**
     * Get the contraption's pitch and roll angles in radians. See
     * {@link #getAngles} for axis conventions.
     *
     * @return A two-element list {pitch, roll} in radians.
     */
    @Override
    @LuaFunction
    public List<Double> getAnglesRad() {
        return super.getAnglesRad();
    }

    /**
     * Get the contraption's angular velocity in body frame.
     * Components are rotation rates about each body axis:
     * {@code wx} = pitch rate (about body-X, matches the sign of {@link #getAngles}'s
     * pitch derivative); {@code wy} = yaw rate (about body-Y); {@code wz} =
     * roll rate (about body-Z, matches the sign of {@link #getAngles}'s roll
     * derivative). From Sable's rigid-body engine.
     *
     * @return A three-element list {wx, wy, wz} in degrees/sec.
     */
    @LuaFunction
    public List<Double> getAngularRates() {
        final Vector3dc w = ((GimbalSensorExt) this.blockEntity).getAngularVelocityBody();
        return List.of(Math.toDegrees(w.x()), Math.toDegrees(w.y()), Math.toDegrees(w.z()));
    }

    /**
     * Get the contraption's angular velocity in body frame, in radians/sec.
     * See {@link #getAngularRates} for axis conventions.
     *
     * @return A three-element list {wx, wy, wz} in radians/sec.
     */
    @LuaFunction
    public List<Double> getAngularRatesRad() {
        final Vector3dc w = ((GimbalSensorExt) this.blockEntity).getAngularVelocityBody();
        return List.of(w.x(), w.y(), w.z());
    }

    /**
     * Get the local gravity vector expressed in body frame.
     * The dimension's gravity in world frame, rotated into the contraption's
     * body frame. Sable's stock default is {@code (0, -11.0, 0)} m/s² applied
     * uniformly to every dimension (Overworld, Nether, End, modded — all the
     * same out of the box). Other mods, modpacks, or datapacks can override
     * per-dimension by shipping {@code data/<ns>/dimension_physics/<file>.json}
     * with a {@code base_gravity} entry — e.g. a Moon dimension mod would set
     * {@code [0, -1.6, 0]} and this sensor would faithfully report it without
     * any Avionics-side change.
     * <p>
     * Useful for attitude estimation: {@code atan2(g.x, -g.y)} ≈ roll,
     * {@code atan2(g.z, -g.y)} ≈ pitch — the same derivation {@link #getAngles}
     * performs internally.
     *
     * @return A three-element list {gx, gy, gz} in m/s².
     */
    @LuaFunction
    public List<Double> getGravity() {
        final Vector3dc g = ((GimbalSensorExt) this.blockEntity).getGravityBody();
        return List.of(g.x(), g.y(), g.z());
    }

    /**
     * Get the contraption's proper acceleration in body frame — what an
     * onboard accelerometer would read.
     * <p>
     * Computed as {@code (Δv × 20) - gravity_body}: finite-differenced
     * inertial velocity at the server tick rate, with body-frame gravity
     * subtracted out. Has one tick of lag.
     * <p>
     * Conventions:
     * <ul>
     *   <li>Stationary or constant-velocity contraption reads
     *       {@code -getGravity()} (the normal force "felt" by the body).</li>
     *   <li>Free-falling contraption reads zero.</li>
     *   <li>To recover inertial acceleration (rate-of-change of body
     *       velocity), add {@code getGravity()} component-wise.</li>
     * </ul>
     *
     * @return A three-element list {ax, ay, az} in m/s².
     */
    @LuaFunction
    public List<Double> getLinearAcceleration() {
        final Vector3dc a = ((GimbalSensorExt) this.blockEntity).getLinearAccelerationBody();
        return List.of(a.x(), a.y(), a.z());
    }
}
