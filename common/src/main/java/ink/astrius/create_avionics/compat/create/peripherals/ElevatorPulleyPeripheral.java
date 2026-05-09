package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An elevator pulley — Create's smart-floor lift. Drives a cabin between
 * floor contacts placed in a column; motion is target-driven (sqrt-decel
 * convergence to {@code currentTarget}), not gearshift-driven.
 *
 * <p>Lua control mirrors the player's options:</p>
 * <ul>
 *   <li>Right-click empty hand on the pulley → {@link #assemble} /
 *       {@link #disassemble}.</li>
 *   <li>Pulse a floor contact (or click an in-cabin Contraption Controls
 *       widget) → {@link #setTargetFloor}.</li>
 * </ul>
 *
 * <p>Floor identity is owned by the contact peripherals. Use
 * {@link #getFloors} to enumerate them, or pair with
 * {@code Create_ElevatorContact} peripherals on the same column.</p>
 *
 * @cc.module Create_ElevatorPulley
 */
public class ElevatorPulleyPeripheral extends KineticPeripheral<ElevatorPulleyBlockEntity> {

    public ElevatorPulleyPeripheral(final ElevatorPulleyBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_ElevatorPulley";
    }

    // --- Assembly ---

    /**
     * Check whether the pulley has assembled a cabin.
     *
     * @return True if running.
     */
    @LuaFunction
    public final boolean isAssembled() {
        return this.blockEntity.running;
    }

    /**
     * Assemble the pulley's cabin. Equivalent to a player right-clicking the
     * pulley with an empty hand while it's stopped. No-op if already assembled.
     * <p>Yields until the next server tick.
     */
    @LuaFunction(mainThread = true)
    public final void assemble() {
        if (!this.blockEntity.running) {
            this.blockEntity.assembleNextTick = true;
        }
    }

    /**
     * Disassemble the pulley's cabin.
     * <p>Yields until the next server tick.
     */
    @LuaFunction(mainThread = true)
    public final void disassemble() {
        if (this.blockEntity.running) {
            this.blockEntity.disassemble();
        }
    }

    // --- Cabin motion ---

    /**
     * Get the cable's current extension distance, in blocks. 0 = cabin at the
     * pulley's level; positive = extended downward.
     *
     * @return The current offset in blocks.
     */
    @LuaFunction
    public final double getOffset() {
        return this.blockEntity.getInterpolatedOffset(1.0f);
    }

    /**
     * Get the maximum extension this pulley can reach, in blocks.
     *
     * @return The maximum extension.
     */
    @LuaFunction
    public final int getMaxLength() {
        if (this.blockEntity.getLevel() == null) return 0;
        final int worldY = this.blockEntity.getBlockPos().getY();
        final int minY = this.blockEntity.getLevel().getMinBuildHeight();
        return Math.max(0, Math.min(AllConfigs.server().kinetics.maxRopeLength.get(), (worldY - 1) - minY));
    }

    /**
     * Get the cabin's current world Y coordinate.
     *
     * @return The platform's world Y.
     */
    @LuaFunction
    public final int getCurrentY() {
        return this.blockEntity.getCurrentValue();
    }

    /**
     * Get the cabin's current movement speed in blocks per tick. Signed:
     * positive descending, negative ascending, 0 stopped.
     *
     * @return The movement speed.
     */
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getMovementSpeed();
    }

    /**
     * Check whether the cabin has arrived at its current target floor (within
     * the elevator's tolerance, ~0.5 blocks).
     *
     * @return True if arrived.
     */
    @LuaFunction
    public final boolean isArrived() {
        final ElevatorContraption ec = this.contraption();
        return ec == null ? !this.blockEntity.running : ec.arrived;
    }

    // --- Column / floors ---

    /**
     * Get the opaque id of this cabin's column, or nil when unassembled.
     * Matches {@code Create_ElevatorContact.getColumnId()} on every contact
     * on the same column — equality-comparable for cross-peripheral grouping.
     * <p>Yields until the next server tick (column resolution touches
     * server-thread-only state).
     *
     * @return The column id, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getColumnId() {
        final ElevatorContraption ec = this.contraption();
        return ec == null ? null : KineticReadback.columnIdOf(ec.getGlobalColumn());
    }

    /**
     * Get the Y level the cabin is currently heading to, or nil when no
     * target is selected (e.g. just-assembled and idle, or unassembled).
     * <p>Yields until the next server tick.
     *
     * @return The target Y, or nil.
     */
    @LuaFunction(mainThread = true)
    public final Integer getCurrentTargetY() {
        final ElevatorContraption ec = this.contraption();
        if (ec == null) return null;
        final Level level = this.blockEntity.getLevel();
        if (level == null) return null;
        return ec.getCurrentTargetY(level);
    }

    /**
     * Get the list of floors on this cabin's column, ordered bottom-to-top.
     * Each entry is a table {@code {y, short_name, long_name}}. Returns nil
     * when unassembled.
     * <p>Yields until the next server tick.
     *
     * @return A list of floor tables, or nil.
     */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getFloors() {
        final ElevatorContraption ec = this.contraption();
        if (ec == null) return null;
        final Level level = this.blockEntity.getLevel();
        if (level == null) return null;
        final ElevatorColumn column = ElevatorColumn.get(level, ec.getGlobalColumn());
        if (column == null) return null;

        final List<IntAttached<Couple<String>>> raw = column.compileNamesList();
        final List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (final IntAttached<Couple<String>> entry : raw) {
            final Couple<String> names = entry.getSecond();
            final Map<String, Object> e = new HashMap<>();
            e.put("y", entry.getFirst());
            e.put("short_name", names.getFirst());
            e.put("long_name", names.getSecond());
            out.add(e);
        }
        return out;
    }

    /**
     * Get the floor the cabin is currently parked at, or nil if the cabin
     * isn't currently arrived at any known floor (in transit, parked
     * between floors via {@link #setTargetY}, or unassembled).
     * <p>
     * Same shape as one entry of {@link #getFloors}: {@code {y, short_name,
     * long_name}}.
     * <p>Yields until the next server tick.
     *
     * @return The current floor table, or nil.
     */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getCurrentFloor() {
        if (!this.isArrived()) return null;
        final Integer ty = this.getCurrentTargetY();
        if (ty == null) return null;
        final List<Map<String, Object>> floors = this.getFloors();
        if (floors == null) return null;
        for (final Map<String, Object> f : floors) {
            if (((Number) f.get("y")).intValue() == ty) return f;
        }
        return null;
    }

    /**
     * Get every contact on this cabin's column with its current state.
     * <p>
     * Each entry is a table:
     * <ul>
     *   <li>{@code y}: contact's Y</li>
     *   <li>{@code short_name}, {@code long_name}: floor names</li>
     *   <li>{@code calling}: this contact is the column's selected target</li>
     *   <li>{@code powering}: cabin is at this floor right now</li>
     *   <li>{@code door_mode}: contact's door mode (e.g. {@code all},
     *       {@code north}, ...)</li>
     * </ul>
     * Returns nil when unassembled or the column is unavailable.
     * <p>Yields until the next server tick.
     *
     * @return A list of contact tables, or nil.
     */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getColumnContacts() {
        final ElevatorContraption ec = this.contraption();
        if (ec == null) return null;
        final Level level = this.blockEntity.getLevel();
        if (level == null) return null;
        final ElevatorColumn column = ElevatorColumn.get(level, ec.getGlobalColumn());
        if (column == null) return null;

        final List<Map<String, Object>> out = new ArrayList<>();
        for (final BlockPos pos : column.getContacts()) {
            final BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ElevatorContactBlock)) continue;
            final BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof final ElevatorContactBlockEntity ecbe)) continue;

            final Map<String, Object> entry = new HashMap<>();
            entry.put("y", pos.getY());
            entry.put("short_name", ecbe.shortName);
            entry.put("long_name", ecbe.longName);
            entry.put("calling", state.getValue(ElevatorContactBlock.CALLING));
            entry.put("powering", state.getValue(ElevatorContactBlock.POWERING));
            entry.put("door_mode", ecbe.doorControls.mode.name().toLowerCase(Locale.ROOT));
            out.add(entry);
        }
        return out;
    }

    /**
     * Halt the cabin at its current Y (sets the target to the current
     * position). The cabin decelerates smoothly and parks. Equivalent to
     * {@link #setTargetY} with {@link #getCurrentY}; convenience.
     * <p>Yields until the next server tick.
     */
    @LuaFunction(mainThread = true)
    public final void stop() throws LuaException {
        this.setTargetY(this.blockEntity.getCurrentValue());
    }

    /**
     * Send the cabin to a free-form Y level, regardless of whether a floor
     * contact exists there.
     * <p>
     * The cabin physically moves to that Y using the same target-driven
     * motion the contact system uses. <b>Differences vs. {@link #setTargetFloor}:</b>
     * <ul>
     *   <li>No floor contact gets {@code CALLING} (no button "lights up").</li>
     *   <li>{@code POWERING} on arrival fires only if a contact happens to be
     *       at the destination Y — door logic triggers accordingly.</li>
     * </ul>
     * Bounded to the cabin's reachable range ({@code minContactY ..
     * maxContactY}); {@link LuaException} if outside.
     * <p>Yields until the next server tick.
     *
     * @param y The destination Y coordinate.
     */
    @LuaFunction(mainThread = true)
    public final void setTargetY(final int y) throws LuaException {
        final ElevatorContraption ec = this.contraption();
        if (ec == null) throw new LuaException("elevator pulley is not assembled");
        final Level level = this.blockEntity.getLevel();
        if (level == null) throw new LuaException("level not loaded");

        final ColumnCoords coords = ec.getGlobalColumn();
        final ElevatorColumn column = ElevatorColumn.get(level, coords);
        if (column == null) throw new LuaException("column not found");

        if (ec.isTargetUnreachable(y)) throw new LuaException("Y=" + y + " is out of cable reach");
        column.target(y);
        column.markDirty();
    }

    /**
     * Send the cabin to the floor at the given world Y. Equivalent to a
     * redstone pulse on that floor's contact, or a player using an in-cabin
     * Contraption Controls UI to pick the floor.
     * <p>Yields until the next server tick.
     *
     * @param y The destination floor's Y coordinate.
     */
    @LuaFunction(mainThread = true)
    public final void setTargetFloor(final int y) throws LuaException {
        final ElevatorContraption ec = this.contraption();
        if (ec == null) throw new LuaException("elevator pulley is not assembled");
        final Level level = this.blockEntity.getLevel();
        if (level == null) throw new LuaException("level not loaded");

        final ColumnCoords coords = ec.getGlobalColumn();
        final ElevatorColumn column = ElevatorColumn.get(level, coords);
        if (column == null) throw new LuaException("column not found");

        final boolean exists = column.getContacts().stream().anyMatch(p -> p.getY() == y);
        if (!exists) throw new LuaException("no floor contact at Y=" + y);
        if (ec.isTargetUnreachable(y)) throw new LuaException("Y=" + y + " is out of cable reach");

        final BlockPos contactPos = column.contactAt(y);
        final BlockState contactState = level.getBlockState(contactPos);
        if (!(contactState.getBlock() instanceof final ElevatorContactBlock ecb)) {
            throw new LuaException("expected an elevator contact at Y=" + y);
        }
        ecb.callToContactAndUpdate(column, contactState, level, contactPos, false);
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

    // --- Helpers ---

    private ElevatorContraption contraption() {
        if (this.blockEntity.movedContraption == null) return null;
        if (!(this.blockEntity.movedContraption.getContraption() instanceof final ElevatorContraption ec)) return null;
        return ec;
    }
}
