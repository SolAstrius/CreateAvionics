package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IControlContraption.MovementMode;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.api.create.LinearActuatorExt;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * A mechanical piston (regular or sticky). Exposes the same surface a player
 * has — assemble/disassemble via right-click empty hand, the movement-mode
 * scroll option — plus read-only state visible through goggles or block
 * state.
 *
 * <p>Like the bearing, the piston is driven by kinetic input: positive
 * rotation extends, negative retracts, zero stops. There's no setpoint
 * either in vanilla or here. To program timed extensions, drive the piston
 * via a {@code Create_SequencedGearshift} upstream and use its
 * {@code move()} / {@code setInstructions()}.</p>
 *
 * <p>This peripheral covers both the regular and sticky variants — they
 * share a block-entity class. Use {@link #isSticky} to distinguish.</p>
 *
 * @cc.module Create_MechanicalPiston
 */
public class MechanicalPistonPeripheral extends KineticPeripheral<MechanicalPistonBlockEntity> {

    public MechanicalPistonPeripheral(final MechanicalPistonBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_MechanicalPiston";
    }

    // --- Assembly ---

    /**
     * Check whether the piston has assembled a contraption (i.e. is running).
     *
     * @return True if running.
     */
    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.running;
    }

    /**
     * Assemble the piston's contraption. Equivalent to a player right-clicking
     * the piston with an empty hand. No-op if already assembled.
     */
    @LuaFunction(mainThread = true)
    public final void assemble() {
        if (!this.blockEntity.running) {
            this.blockEntity.assembleNextTick = true;
        }
    }

    /**
     * Disassemble the piston's contraption.
     */
    @LuaFunction(mainThread = true)
    public final void disassemble() {
        if (this.blockEntity.running) {
            this.blockEntity.disassemble();
        }
    }

    // --- State ---

    /**
     * Get the piston's current extension distance, in blocks.
     * Continuous; visual position is interpolated to the current tick.
     *
     * @return The current offset in blocks.
     */
    @LuaFunction
    public final double getOffset() {
        return this.blockEntity.getInterpolatedOffset(1.0f);
    }

    /**
     * Get the piston's current movement speed (blocks per tick).
     * Signed: positive means extending, negative means retracting, 0 when
     * stopped (either at a limit or when kinetic input is zero).
     *
     * @return The movement speed.
     */
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getMovementSpeed();
    }

    /**
     * Get the piston's current motion vector in world frame.
     * Magnitude equals {@link #getMovementSpeed}; direction is the piston's
     * facing axis (positive direction when extending).
     *
     * @return A three-element list {x, y, z} in blocks/tick.
     */
    @LuaFunction
    public final List<Double> getMotionVector() {
        final Vec3 v = this.blockEntity.getMotionVector();
        return List.of(v.x(), v.y(), v.z());
    }

    /**
     * Get the piston's block-state machine position.
     * One of {@code retracted}, {@code moving}, {@code extended} (matches the
     * block-state {@code state} property).
     *
     * @return The state string.
     */
    @LuaFunction
    public final String getState() {
        final PistonState s = this.blockEntity.getBlockState().getValue(MechanicalPistonBlock.STATE);
        return s.getSerializedName();
    }

    /**
     * Check whether this piston is the sticky variant (slimeball-converted).
     * Sticky pistons pull their attached contraption back on retraction;
     * non-sticky leave it where it ended up. Visually distinguished by the
     * slime-coated head.
     *
     * @return True if sticky.
     */
    @LuaFunction
    public final boolean isSticky() {
        return AllBlocks.STICKY_MECHANICAL_PISTON.has(this.blockEntity.getBlockState());
    }

    // --- Movement mode (scriptable scroll-option) ---

    /**
     * Get the piston's movement mode — what happens to the contraption blocks
     * when motion stops.
     * <p>
     * One of:
     * <ul>
     *   <li>{@code move_place} — place blocks back on stop</li>
     *   <li>{@code move_place_returned} — place blocks back only when stopped
     *       at the starting offset</li>
     *   <li>{@code move_never_place} — never place blocks back (contraption
     *       stays as an entity)</li>
     * </ul>
     *
     * @return The mode string.
     */
    @LuaFunction
    public final String getMovementMode() {
        return this.movementMode().get().name().toLowerCase(Locale.ROOT);
    }

    /**
     * Set the piston's movement mode. Matches the in-game scroll option.
     *
     * @param mode {@code move_place}, {@code move_place_returned}, or
     *             {@code move_never_place}.
     */
    @LuaFunction(mainThread = true)
    public final void setMovementMode(final String mode) throws LuaException {
        final MovementMode parsed;
        try {
            parsed = MovementMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new LuaException("expected one of 'move_place', 'move_place_returned', 'move_never_place'");
        }
        this.movementMode().setValue(parsed.ordinal());
    }

    // --- Errors ---

    /**
     * Get the last assembly error message, or nil if the last attempt
     * succeeded or no attempt has been made. Same text the goggles show on a
     * piston whose last assembly failed.
     *
     * @return The error message, or nil.
     */
    @LuaFunction
    public final String getLastAssemblyError() {
        final AssemblyException e = this.blockEntity.getLastAssemblyException();
        if (e == null || e.component == null) return null;
        return e.component.getString();
    }

    private ScrollOptionBehaviour<MovementMode> movementMode() {
        return ((LinearActuatorExt) this.blockEntity).createAvionics$movementMode();
    }
}
