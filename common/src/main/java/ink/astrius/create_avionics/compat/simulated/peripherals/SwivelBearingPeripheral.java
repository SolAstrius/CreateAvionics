package ink.astrius.create_avionics.compat.simulated.peripherals;

import com.simibubi.create.content.contraptions.AssemblyException;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import ink.astrius.create_avionics.api.simulated.SwivelBearingExt;
import ink.astrius.create_avionics.compat.create.peripherals.KineticReadback;
import net.minecraft.core.BlockPos;

import java.util.Locale;
import java.util.UUID;

/**
 * A swivel bearing — Simulated's contraption assembler. Exposes the same
 * surface the player has via right-click empty hand (assemble/disassemble),
 * read-only state visible through goggles or the block itself (assembled
 * flag, target angle), and topology pointers to the bearing's plate and the
 * sub-level it rotates.
 *
 * <p>The target angle is integrated from kinetic input — neither the player
 * nor scripts can set it directly; spinning the input shaft drives it.</p>
 *
 * @cc.module swivel_bearing
 */
public class SwivelBearingPeripheral extends SimKineticPeripheral<SwivelBearingBlockEntity> {

    public SwivelBearingPeripheral(final SwivelBearingBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "swivel_bearing";
    }

    // --- Assembly ---

    /**
     * Check whether the bearing has assembled a sub-level.
     *
     * @return True if assembled.
     */
    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.isAssembled();
    }

    /**
     * Assemble the bearing's contraption into a sub-level.
     * Equivalent to a player right-clicking the bearing with an empty hand
     * while it's not yet assembled. No-op if already assembled.
     */
    @LuaFunction(mainThread = true)
    public final void assemble() {
        if (!this.blockEntity.isAssembled()) {
            this.blockEntity.assembleNextTick = true;
        }
    }

    /**
     * Disassemble the bearing's sub-level back into world blocks.
     */
    @LuaFunction(mainThread = true)
    public final void disassemble() {
        this.blockEntity.disassemble();
    }

    // --- Angle ---

    /**
     * Get the bearing's target angle, in degrees.
     * The target is integrated from kinetic input each tick; not directly
     * settable from script (no player setter exists either).
     *
     * @return The target angle in degrees.
     */
    @LuaFunction
    public final double getTargetAngle() {
        return this.blockEntity.getTargetAngleDegrees();
    }

    /**
     * Get the bearing's target angle, in radians.
     *
     * @return The target angle in radians.
     */
    @LuaFunction
    public final double getTargetAngleRad() {
        return Math.toRadians(this.blockEntity.getTargetAngleDegrees());
    }

    // --- Topology ---

    /**
     * Get the id of the bearing's paired plate block, or nil if no plate is
     * linked. Same id flavor as {@code getSelfId} on a peripheral wrapped
     * around the plate; equality-comparable.
     *
     * @return The plate's id, or nil.
     */
    @LuaFunction
    public final String getPlateId() {
        final BlockPos p = this.blockEntity.getPlatePos();
        return p == null ? null : KineticReadback.idOf(p);
    }

    /**
     * Get the UUID of the sub-level this bearing's contraption rotates as,
     * or nil when the bearing isn't currently assembled.
     *
     * @return The sub-level UUID string, or nil.
     */
    @LuaFunction
    public final String getSubLevelId() {
        final UUID id = this.blockEntity.getSubLevelID();
        return id == null ? null : id.toString();
    }

    // --- Locking ---

    private SwivelBearingExt ext() {
        return (SwivelBearingExt) this.blockEntity;
    }

    /**
     * Check whether the bearing is currently asserting its rotational lock.
     * True when the block state is powered (the bearing is actively holding
     * its attached sub-level fixed against the parent).
     *
     * @return True if locked.
     */
    @LuaFunction
    public final boolean isLocked() {
        return this.ext().createAvionics$isLocking();
    }

    /**
     * Get the bearing's locking mode — how it reacts to redstone signal.
     * One of:
     * <ul>
     *   <li>{@code "locked_always"} — locked regardless of signal</li>
     *   <li>{@code "locked_default"} — locked at rest; signal unlocks</li>
     *   <li>{@code "unlocked_default"} — unlocked at rest; signal locks</li>
     *   <li>{@code "unlocked_always"} — unlocked regardless of signal</li>
     * </ul>
     * Mirrors the in-game scroll option.
     *
     * @return The current locking mode.
     */
    @LuaFunction
    public final String getLockingMode() {
        return this.ext().createAvionics$getLockingMode();
    }

    /**
     * Set the bearing's locking mode. See {@link #getLockingMode} for the
     * accepted values.
     *
     * @param mode The new locking mode.
     */
    @LuaFunction(mainThread = true)
    public final void setLockingMode(final String mode) throws LuaException {
        final int ordinal = switch (mode.toLowerCase(Locale.ROOT)) {
            case "locked_always" -> 0;
            case "locked_default" -> 1;
            case "unlocked_default" -> 2;
            case "unlocked_always" -> 3;
            default -> throw new LuaException(
                "expected 'locked_always', 'locked_default', 'unlocked_default', or 'unlocked_always'");
        };
        this.ext().createAvionics$setLockingModeOrdinal(ordinal);
    }

    // --- Assembly diagnostics ---

    /**
     * Get the message from the bearing's most recent failed assembly attempt,
     * or nil if the last {@link #assemble} call succeeded (or none has been
     * made yet).
     *
     * @return The failure message, or nil.
     */
    @LuaFunction
    public final String getLastAssemblyException() {
        final AssemblyException e = this.blockEntity.getLastAssemblyException();
        return e == null ? null : e.getMessage();
    }
}
