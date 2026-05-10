package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import ink.astrius.create_avionics.api.aero.GyroscopicPropellerBearingExt;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Map;

/**
 * Gyroscopic propeller bearing. On top of the standard propeller-bearing API,
 * exposes the gyro's internal state and a persistent manual-override target
 * that replaces the bearing's automatic gravity-tracking.
 * <p>The bearing always applies its hard 12° cone clamp around the block
 * normal, snaps to rest when redstone-powered, and gates the move by
 * {@link #getStabilizationStrength()} — these still apply on top of any
 * manual target.
 *
 * @cc.module gyroscopic_propeller_bearing
 */
public class GyroscopicPropellerBearingPeripheral extends PropellerBearingPeripheral<GyroscopicPropellerBearingBlockEntity> {

    public GyroscopicPropellerBearingPeripheral(final GyroscopicPropellerBearingBlockEntity blockEntity) {
        super(blockEntity, "gyroscopic_propeller_bearing");
    }

    private static Vector3d toVec(final Map<?, ?> table) throws LuaException {
        if (table == null || table.size() != 3) {
            throw new LuaException("expected a 3-element list of numbers");
        }
        return new Vector3d(num(table, 1), num(table, 2), num(table, 3));
    }

    private static Vector3d toUnitVec(final Map<?, ?> table) throws LuaException {
        final Vector3d v = toVec(table);
        if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z)) {
            throw new LuaException("direction components must be finite");
        }
        final double lenSq = v.lengthSquared();
        if (lenSq < 1.0e-12) {
            throw new LuaException("direction must be non-zero");
        }
        return v.normalize();
    }

    private static double num(final Map<?, ?> table, final int idx) throws LuaException {
        final Object v = table.get((double) idx);
        if (!(v instanceof Number n)) {
            throw new LuaException("expected number at index " + idx);
        }
        return n.doubleValue();
    }

    private Vector3d blockNormalVec() {
        final Direction facing = this.blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
        return new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
    }

    private GyroscopicPropellerBearingExt ext() {
        return (GyroscopicPropellerBearingExt) this.blockEntity;
    }

    // --- Diagnostics ---

    /**
     * The bearing's mounted-facing direction as a unit vector. This is the
     * "rest" thrust direction the gyro returns to when unpowered or when
     * stabilization is inactive.
     *
     * @return A three-element list {x, y, z}.
     */
    @LuaFunction
    public final List<Double> getBlockNormal() {
        final Vector3dc n = this.blockNormalVec();
        return List.of(n.x(), n.y(), n.z());
    }

    /**
     * Angle between the current thrust direction and the bearing's
     * mounted-facing direction, in degrees. Bounded by the bearing's hard
     * 12° cone clamp.
     *
     * @return Deflection angle in degrees, in [0, 12].
     */
    @LuaFunction
    public final double getTiltAngle() {
        final Vector3dc thrust = this.blockEntity.thrustDirection;
        final Vector3d normal = this.blockNormalVec();
        final double dot = Math.max(-1.0, Math.min(1.0, thrust.dot(normal)));
        return Math.toDegrees(Math.acos(dot));
    }

    /**
     * Effective stabilization gain in [0, 1]. Multiplies any tilt input
     * against the bearing's rest direction — at 0 the bearing snaps to
     * {@link #getBlockNormal()} and ignores any target; at 1 it tracks the
     * target fully.
     * <p>Computed as the product of three gates:
     * <ol>
     *   <li>0 if no contraption is assembled, else 1.</li>
     *   <li>Multiplied by {@code |speed|} when {@code |speed| < 1}; otherwise unchanged.</li>
     *   <li>Multiplied by the disassembly-slowdown countdown fraction when slowdown is active.</li>
     * </ol>
     * If a tilt target appears to do nothing, this is almost always why —
     * typically because the bearing is unassembled or not spinning.
     *
     * @return Stabilization gain in [0, 1].
     */
    @LuaFunction
    public final double getStabilizationStrength() {
        double lerp = this.blockEntity.getMovedContraption() == null ? 0.0 : 1.0;
        final double speed = Math.abs(this.blockEntity.getSpeed());
        if (speed < 1.0) {
            lerp *= speed;
        }
        if (this.blockEntity.disassemblySlowdown) {
            final float countdown = this.blockEntity.slowdownController.getCountdown();
            final float maxTime = this.blockEntity.slowdownController.getMaxTime();
            if (maxTime > 0) {
                lerp *= countdown / maxTime;
            }
        }
        return lerp;
    }

    // --- Manual target override ---

    /**
     * Install a persistent manual target direction. Each physics tick the
     * bearing will lerp its thrust axis toward this direction instead of the
     * gravity-derived "anti-gravity up" it would otherwise compute. The 12°
     * cone clamp, redstone power gate, and stabilization-strength gate still
     * apply on top of this value — see {@link #getStabilizationStrength()}.
     * <p>Call {@link #clearManualTarget()} to return the bearing to default
     * gravity tracking.
     *
     * @param target World-frame unit-length direction (3 elements).
     */
    @LuaFunction(mainThread = true)
    public final void setManualTarget(final Map<?, ?> target) throws LuaException {
        this.ext().setManualTarget(toUnitVec(target));
    }

    /**
     * Remove any manual target, returning the bearing to default
     * gravity-tracking behavior.
     */
    @LuaFunction(mainThread = true)
    public final void clearManualTarget() {
        this.ext().clearManualTarget();
    }

    /**
     * @return The installed manual target as {x, y, z}, or {@code nil} if the
     *         bearing is currently gravity-tracking.
     */
    @LuaFunction
    public final List<Double> getManualTarget() {
        final Vector3dc t = this.ext().getManualTarget();
        return t == null ? null : List.of(t.x(), t.y(), t.z());
    }
}
