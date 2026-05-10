package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IControlContraption.MovementMode;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.api.create.LinearActuatorExt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * A rope pulley. Vertical contraption movement: the platform descends as the
 * shaft turns one way, ascends as it turns the other. Like the piston, motion
 * is purely kinetic-driven — no setpoint either in vanilla or here.
 *
 * <p>Drive programmatically via an upstream {@code Create_SequencedGearshift}'s
 * {@code move(distance)} (one revolution = one block of rope extension).</p>
 *
 * <p>Wide platforms span multiple pulleys via Create's "mirror" mechanism: one
 * pulley is the parent (does the assembly), others become passive children
 * inheriting motion. {@link #getMirrorParentId} and {@link #isMirrorChild}
 * surface this for SCADA dashboards.</p>
 *
 * @cc.module Create_RopePulley
 */
public class RopePulleyPeripheral extends KineticPeripheral<PulleyBlockEntity> {

    public RopePulleyPeripheral(final PulleyBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_RopePulley";
    }

    // --- Assembly ---

    /**
     * Check whether the pulley has assembled a contraption (is running).
     *
     * @return True if running.
     */
    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.running;
    }

    /**
     * Assemble the pulley's contraption. Equivalent to a player right-clicking
     * the pulley with an empty hand. No-op if already assembled.
     */
    @LuaFunction(mainThread = true)
    public final void assemble() {
        if (!this.blockEntity.running) {
            this.blockEntity.assembleNextTick = true;
        }
    }

    /**
     * Disassemble the pulley's contraption. Restores rope blocks and the
     * magnet block beneath the rope chain.
     */
    @LuaFunction(mainThread = true)
    public final void disassemble() {
        if (this.blockEntity.running) {
            this.blockEntity.disassemble();
        }
    }

    // --- State ---

    /**
     * Get the rope's current extension distance, in blocks. 0 = retracted
     * (platform at the pulley's level); positive = extended downward.
     *
     * @return The current offset in blocks.
     */
    @LuaFunction
    public final double getOffset() {
        return this.blockEntity.getInterpolatedOffset(1.0f);
    }

    /**
     * Get the maximum extension this pulley can reach, in blocks. Bounded by
     * the {@code maxRopeLength} server config and the pulley's height above
     * the world's minimum build Y.
     *
     * @return The maximum extension in blocks.
     */
    @LuaFunction
    public final int getMaxLength() {
        if (this.blockEntity.getLevel() == null) return 0;
        final int worldY = this.blockEntity.getBlockPos().getY();
        final int minY = this.blockEntity.getLevel().getMinBuildHeight();
        return Math.max(0, Math.min(AllConfigs.server().kinetics.maxRopeLength.get(), (worldY - 1) - minY));
    }

    /**
     * Get the pulley's current movement speed (blocks per tick). Positive
     * means extending (descending), negative means retracting (ascending),
     * 0 when stopped.
     *
     * @return The movement speed.
     */
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getMovementSpeed();
    }

    /**
     * Get the pulley's current motion vector in world frame (always vertical).
     *
     * @return A three-element list {x, y, z} in blocks/tick.
     */
    @LuaFunction
    public final List<Double> getMotionVector() {
        final Vec3 v = this.blockEntity.getMotionVector();
        return List.of(v.x(), v.y(), v.z());
    }

    /**
     * Get the world Y coordinate of the rope's lowest point — i.e. the
     * platform's current Y level. Same value a Threshold Switch attached to
     * this pulley would read.
     *
     * @return The platform's world Y.
     */
    @LuaFunction
    public final int getCurrentY() {
        return this.blockEntity.getCurrentValue();
    }

    // --- Movement mode (scriptable scroll-option) ---

    /**
     * Get the pulley's movement mode — what happens to the platform's blocks
     * when motion stops.
     * <p>
     * One of:
     * <ul>
     *   <li>{@code move_place} — place blocks back on stop</li>
     *   <li>{@code move_place_returned} — place blocks back only when stopped
     *       at the starting offset</li>
     *   <li>{@code move_never_place} — never place blocks back (platform
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
     * Set the pulley's movement mode. Matches the in-game scroll option.
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

    // --- Mirroring ---

    /**
     * Check whether this pulley is a mirror child (passive follower of
     * another pulley's contraption). Wide platforms sync multiple pulleys
     * via this mechanism — one parent does the assembly, others mirror its
     * motion.
     *
     * @return True if this pulley is mirroring another.
     */
    @LuaFunction
    public final boolean isMirrorChild() {
        return this.blockEntity.getMirrorParent() != null;
    }

    /**
     * Get the id of the pulley this one mirrors, or nil if independent. Same
     * opaque-token flavor as {@code getSelfId} on a peripheral wrapping the
     * parent pulley — matchable for equality.
     *
     * @return The parent pulley's id, or nil.
     */
    @LuaFunction
    public final String getMirrorParentId() {
        final BlockPos p = this.blockEntity.getMirrorParent();
        return p == null ? null : KineticReadback.idOf(p);
    }

    // --- Errors ---

    /**
     * Get the last assembly error message, or nil if the last attempt
     * succeeded or no attempt has been made.
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
