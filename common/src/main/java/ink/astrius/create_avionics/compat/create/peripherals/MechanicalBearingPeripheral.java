package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IControlContraption.RotationMode;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.api.create.MechanicalBearingExt;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A mechanical bearing — Create's contraption assembler. Exposes the same
 * surface the player has via right-click empty hand (assemble/disassemble),
 * the rotation mode they can scroll on the bearing, and read-only state
 * visible through goggles.
 *
 * <p>Like its Avionics counterparts {@code propeller_bearing} (Aero) and
 * {@code swivel_bearing} (Sim), the bearing's rotation is integrated from
 * kinetic input — neither the player nor scripts can set it directly.</p>
 *
 * @cc.module Create_MechanicalBearing
 */
public class MechanicalBearingPeripheral extends KineticPeripheral<MechanicalBearingBlockEntity> {

    public MechanicalBearingPeripheral(final MechanicalBearingBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_MechanicalBearing";
    }

    // --- Assembly ---

    /**
     * Check whether the bearing has assembled a contraption.
     *
     * @return True if running.
     */
    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.isRunning();
    }

    /**
     * Assemble the bearing's contraption. Equivalent to a player right-clicking
     * the bearing with an empty hand while it's not running. No-op if already
     * assembled.
     */
    @LuaFunction(mainThread = true)
    public final void assemble() {
        if (!this.blockEntity.isRunning()) {
            this.blockEntity.assemble();
        }
    }

    /**
     * Disassemble the bearing's contraption.
     */
    @LuaFunction(mainThread = true)
    public final void disassemble() {
        if (this.blockEntity.isRunning()) {
            this.blockEntity.disassemble();
        }
    }

    // --- Angle ---

    /**
     * Get the bearing's current visual angle, in degrees.
     *
     * @return The angle in degrees (interpolated to current tick).
     */
    @LuaFunction
    public final double getAngle() {
        return this.blockEntity.getInterpolatedAngle(1.0f);
    }

    /**
     * Get the bearing's current visual angle, in radians.
     *
     * @return The angle in radians.
     */
    @LuaFunction
    public final double getAngleRad() {
        return Math.toRadians(this.blockEntity.getInterpolatedAngle(1.0f));
    }

    /**
     * Get the bearing's angular speed (degrees per tick × 20 ≈ deg/s).
     * Matches the value goggles show for any kinetic block.
     *
     * @return The angular speed.
     */
    @LuaFunction
    public final double getAngularSpeed() {
        return this.blockEntity.getAngularSpeed();
    }

    /**
     * Check whether the bearing is near its initial (start) angle.
     * Used by the bearing internally to decide when to place blocks back on
     * disassembly under {@code rotate_place_returned}; useful for scripts
     * that want to wait for the contraption to return home before stopping.
     *
     * @return True if within the bearing's "near initial" tolerance.
     */
    @LuaFunction
    public final boolean isNearInitialAngle() {
        return this.blockEntity.isNearInitialAngle();
    }

    // --- Rotation mode (scriptable scroll-option) ---

    /**
     * Get the bearing's rotation mode — what happens to the contraption when
     * it stops rotating.
     * <p>
     * One of:
     * <ul>
     *   <li>{@code rotate_place} — rotate freely, place blocks back on stop</li>
     *   <li>{@code rotate_place_returned} — rotate freely, place blocks back
     *       only when stopped near the initial angle</li>
     *   <li>{@code rotate_never_place} — rotate freely, never place blocks
     *       back (contraption persists as an entity)</li>
     * </ul>
     *
     * @return The mode string.
     */
    @LuaFunction
    public final String getRotationMode() {
        return this.movementMode().get().name().toLowerCase(Locale.ROOT);
    }

    /**
     * Set the bearing's rotation mode. Matches the in-game scroll option on
     * the bearing.
     *
     * @param mode {@code rotate_place}, {@code rotate_place_returned}, or
     *             {@code rotate_never_place}.
     */
    @LuaFunction(mainThread = true)
    public final void setRotationMode(final String mode) throws LuaException {
        final RotationMode parsed;
        try {
            parsed = RotationMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new LuaException("expected one of 'rotate_place', 'rotate_place_returned', 'rotate_never_place'");
        }
        this.movementMode().setValue(parsed.ordinal());
    }

    // --- Variant flags ---

    /**
     * Check whether the bearing has a wooden top variant.
     *
     * @return True if wooden-top (e.g. windmill bearing).
     */
    @LuaFunction
    public final boolean isWoodenTop() {
        return this.blockEntity.isWoodenTop();
    }

    // --- Errors ---

    /**
     * Get the last assembly error message, or nil if the last assembly
     * attempt succeeded or no attempt has been made.
     * Same text the goggles show on a bearing whose last assembly failed.
     *
     * @return The error message, or nil.
     */
    @LuaFunction
    public final String getLastAssemblyError() {
        final AssemblyException e = this.blockEntity.getLastAssemblyException();
        if (e == null || e.component == null) return null;
        return e.component.getString();
    }

    private ScrollOptionBehaviour<RotationMode> movementMode() {
        return ((MechanicalBearingExt) this.blockEntity).createAvionics$movementMode();
    }
}
