package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import ink.astrius.create_avionics.compat.create.peripherals.KineticReadback;
import net.minecraft.core.BlockPos;

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
     * linked. Same id flavor as {@link #getSelfId} on a peripheral wrapped
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
}
