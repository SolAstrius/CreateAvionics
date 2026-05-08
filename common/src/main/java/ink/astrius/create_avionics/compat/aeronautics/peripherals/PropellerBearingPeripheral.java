package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;
import net.minecraft.core.Direction;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Set;

public class PropellerBearingPeripheral<T extends PropellerBearingBlockEntity> extends SimPeripheral<T> {

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

    @LuaFunction
    public final String getAxis() {
        final Direction d = this.blockEntity.getBlockDirection();
        return d == null ? null : d.getSerializedName();
    }

    @LuaFunction
    public final List<Double> getThrustVector() {
        final Vector3dc v = this.blockEntity.thrustDirection;
        return List.of(v.x(), v.y(), v.z());
    }

    @LuaFunction
    public final List<Double> getFacingVector() {
        final Vector3dc v = this.blockEntity.facingDirection;
        return List.of(v.x(), v.y(), v.z());
    }

    // --- Rotation ---

    @LuaFunction
    public final double getKineticSpeed() {
        return this.blockEntity.getSpeed();
    }

    @LuaFunction
    public final double getRotationSpeed() {
        return this.blockEntity.getRotationSpeed();
    }

    @LuaFunction
    public final double getAngularSpeed() {
        return this.blockEntity.getAngularSpeed();
    }

    // Visual angle in degrees (interpolated to current tick).
    @LuaFunction
    public final double getAngle() {
        return this.blockEntity.getInterpolatedAngle(1.0f);
    }

    // --- Output ---

    @LuaFunction
    public final double getThrust() {
        return this.blockEntity.getThrust();
    }

    @LuaFunction
    public final double getAirflow() {
        return this.blockEntity.getAirflow();
    }

    @LuaFunction
    public final double getSailPower() {
        return this.blockEntity.totalSailPower;
    }

    @LuaFunction
    public final boolean isActive() {
        return this.blockEntity.isActive();
    }

    // --- Stress ---

    @LuaFunction
    public final double getStressApplied() {
        return this.blockEntity.calculateStressApplied();
    }

    // --- Thrust direction (scriptable scroll-option) ---

    // "right_handed" or "left_handed". Mirrors the in-game scroll option.
    @LuaFunction
    public final String getThrustHandedness() {
        return this.blockEntity.getThrustDirectionOption().value == 0 ? "right_handed" : "left_handed";
    }

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

    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.getMovedContraption() != null;
    }

    @LuaFunction(mainThread = true)
    public final void assemble() {
        this.blockEntity.assemble();
    }

    @LuaFunction(mainThread = true)
    public final void disassemble() {
        this.blockEntity.disassemble();
    }

    // --- Variant flags ---

    @LuaFunction
    public final boolean isWoodenTop() {
        return this.blockEntity.isWoodenTop();
    }
}
