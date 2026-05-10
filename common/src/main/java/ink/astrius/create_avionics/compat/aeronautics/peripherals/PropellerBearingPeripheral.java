package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimKineticPeripheral;
import net.minecraft.core.Direction;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Set;

/**
 * A propeller bearing. Reports orientation, kinetic state, thrust output,
 * stress, and assembly state, and lets scripts toggle thrust handedness and
 * (dis)assembly. Also exposes "propeller_bearing" as an additional type so
 * scripts can target every variant uniformly.
 *
 * @cc.module propeller_bearing
 */
public class PropellerBearingPeripheral<T extends PropellerBearingBlockEntity> extends SimKineticPeripheral<T> {

    private final String typeName;

    public PropellerBearingPeripheral(final T blockEntity, final String typeName) {
        super(blockEntity);
        this.typeName = typeName;
    }

    public PropellerBearingPeripheral(final T blockEntity) {
        this(blockEntity, "propeller_bearing");
    }

    @Override
    public String getType() {
        return this.typeName;
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("propeller_bearing");
    }

    // --- Orientation ---

    /**
     * Get the bearing's mounted direction.
     *
     * @return The serialized direction name, or nil.
     */
    @LuaFunction
    public final String getAxis() {
        final Direction d = this.blockEntity.getBlockDirection();
        return d == null ? null : d.getSerializedName();
    }

    /**
     * Get the bearing's thrust direction vector.
     *
     * @return A three-element list {x, y, z}.
     */
    @LuaFunction
    public final List<Double> getThrustVector() {
        final Vector3dc v = this.blockEntity.thrustDirection;
        return List.of(v.x(), v.y(), v.z());
    }

    /**
     * Get the bearing's facing direction vector.
     *
     * @return A three-element list {x, y, z}.
     */
    @LuaFunction
    public final List<Double> getFacingVector() {
        final Vector3dc v = this.blockEntity.facingDirection;
        return List.of(v.x(), v.y(), v.z());
    }

    // --- Rotation ---

    /**
     * Get the bearing's rotation speed.
     *
     * @return The rotation speed.
     */
    @LuaFunction
    public final double getRotationSpeed() {
        return this.blockEntity.getRotationSpeed();
    }

    /**
     * Get the bearing's angular speed.
     *
     * @return The angular speed.
     */
    @LuaFunction
    public final double getAngularSpeed() {
        return this.blockEntity.getAngularSpeed();
    }

    /**
     * Get the bearing's current visual angle.
     *
     * @return Visual angle in degrees (interpolated to current tick).
     */
    @LuaFunction
    public final double getAngle() {
        return this.blockEntity.getInterpolatedAngle(1.0f);
    }

    // --- Output ---

    /**
     * Get the bearing's current thrust output.
     * Computed as {@code totalSailPower^1.5 × directionIndependentSpeed × configThrust}.
     * This is the raw thrust; the goggle tooltip shows it scaled by airflow
     * scaling and air pressure.
     *
     * @return The thrust in pixel-Newtons (pN), Sable's force unit.
     */
    @LuaFunction
    public final double getThrust() {
        return this.blockEntity.getThrust();
    }

    /**
     * Get the bearing's current airflow.
     * Computed as {@code sqrt(totalSailPower) × directionIndependentSpeed × configAirflowMult}.
     *
     * @return The airflow in m/s.
     */
    @LuaFunction
    public final double getAirflow() {
        return this.blockEntity.getAirflow();
    }

    /**
     * Get the bearing's total sail power.
     *
     * @return Dimensionless: count of windmill-sail blocks in the assembled
     *         contraption (1 per block tagged {@code create:windmill_sails}).
     */
    @LuaFunction
    public final double getSailPower() {
        return this.blockEntity.totalSailPower;
    }

    /**
     * Check whether the bearing is currently active.
     *
     * @return True if active.
     */
    @LuaFunction
    public final boolean isActive() {
        return this.blockEntity.isActive();
    }

    // --- Thrust direction (scriptable scroll-option) ---

    /**
     * Get the bearing's thrust handedness.
     *
     * @return "right_handed" or "left_handed". Mirrors the in-game scroll option.
     */
    @LuaFunction
    public final String getThrustHandedness() {
        return this.blockEntity.getThrustDirectionOption().value == 0 ? "right_handed" : "left_handed";
    }

    /**
     * Set the bearing's thrust handedness.
     *
     * @param handedness "right_handed" or "left_handed".
     */
    @LuaFunction(mainThread = true)
    public final void setThrustHandedness(final String handedness) throws LuaException {
        final int value = switch (handedness) {
            case "right_handed" -> 0;
            case "left_handed" -> 1;
            default -> throw new LuaException("expected 'right_handed' or 'left_handed'");
        };
        this.blockEntity.getThrustDirectionOption().setValue(value);
    }

    // --- Assembly ---

    /**
     * Check whether the bearing has assembled a contraption.
     *
     * @return True if assembled.
     */
    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.getMovedContraption() != null;
    }

    /**
     * Assemble the bearing's contraption.
     */
    @LuaFunction(mainThread = true)
    public final void assemble() {
        this.blockEntity.assemble();
    }

    /**
     * Disassemble the bearing's contraption.
     */
    @LuaFunction(mainThread = true)
    public final void disassemble() {
        this.blockEntity.disassemble();
    }

    // --- Variant flags ---

    /**
     * Check whether the bearing has a wooden top variant.
     *
     * @return True if wooden-top.
     */
    @LuaFunction
    public final boolean isWoodenTop() {
        return this.blockEntity.isWoodenTop();
    }
}
