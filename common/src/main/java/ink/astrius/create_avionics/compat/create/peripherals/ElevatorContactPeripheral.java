package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlock;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * An elevator contact — one floor button + landing pad. Player workflow:
 * right-click empty hand to edit name and door mode; redstone-pulse to call
 * the cabin to this floor.
 *
 * <p>Pair this with {@code Create_ElevatorPulley} on the same column (same
 * {@link #getColumnId}) for a complete elevator dashboard.</p>
 *
 * @cc.module Create_ElevatorContact
 */
public class ElevatorContactPeripheral extends SyncedPeripheral<ElevatorContactBlockEntity> {

    public ElevatorContactPeripheral(final ElevatorContactBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_ElevatorContact";
    }

    // --- Identity ---

    /**
     * Get this contact's id.
     *
     * @return The block's id.
     */
    @LuaFunction
    public final String getSelfId() {
        return KineticReadback.idOf(this.blockEntity.getBlockPos());
    }

    /**
     * Get the opaque id of this contact's column. Same value for every
     * contact and the elevator pulley on the same column. Returns nil if the
     * contact hasn't initialized its column yet (e.g. just after placement).
     *
     * @return The column id, or nil.
     */
    @LuaFunction
    public final String getColumnId() {
        final ColumnCoords c = this.blockEntity.columnCoords;
        return c == null ? null : KineticReadback.columnIdOf(c);
    }

    /**
     * Get this contact's Y level.
     *
     * @return The Y coordinate.
     */
    @LuaFunction
    public final int getY() {
        return this.blockEntity.getBlockPos().getY();
    }

    // --- Names ---

    /**
     * Get the short floor name shown on display links and in cabin controls
     * (e.g. {@code "1F"}).
     *
     * @return The short name.
     */
    @LuaFunction
    public final String getShortName() {
        return this.blockEntity.shortName;
    }

    /**
     * Get the long floor name shown in the contact's edit screen (e.g.
     * {@code "Lobby"}).
     *
     * @return The long name.
     */
    @LuaFunction
    public final String getLongName() {
        return this.blockEntity.longName;
    }

    /**
     * Set this contact's short name. Same field a player edits in the
     * right-click screen. Propagates to display links on the column.
     *
     * @param shortName The new short name.
     */
    @LuaFunction(mainThread = true)
    public final void setShortName(final String shortName) {
        this.blockEntity.updateName(shortName, this.blockEntity.longName);
    }

    /**
     * Set this contact's long name.
     *
     * @param longName The new long name.
     */
    @LuaFunction(mainThread = true)
    public final void setLongName(final String longName) {
        this.blockEntity.updateName(this.blockEntity.shortName, longName);
    }

    // --- Door control ---

    /**
     * Get the contact's door-control mode — which side(s) of the cabin's
     * doors open when the cabin arrives at this floor.
     * <p>
     * One of: {@code all}, {@code north}, {@code east}, {@code south},
     * {@code west}, {@code none}.
     *
     * @return The door mode string.
     */
    @LuaFunction
    public final String getDoorMode() {
        return this.blockEntity.doorControls.mode.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Set the contact's door-control mode.
     *
     * @param mode One of {@code all}, {@code north}, {@code east},
     *             {@code south}, {@code west}, {@code none}.
     */
    @LuaFunction(mainThread = true)
    public final void setDoorMode(final String mode) throws LuaException {
        final DoorControl parsed;
        try {
            parsed = DoorControl.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            throw new LuaException("expected one of 'all', 'north', 'east', 'south', 'west', 'none'");
        }
        this.blockEntity.doorControls.set(parsed);
    }

    // --- State ---

    /**
     * Check whether this contact is currently powered (a redstone neighbor
     * is providing signal).
     *
     * @return True if powered.
     */
    @LuaFunction
    public final boolean isPowered() {
        return this.blockEntity.getBlockState().getValue(ElevatorContactBlock.POWERED);
    }

    /**
     * Check whether this contact is the column's currently-selected target.
     * True between request and arrival (the floor button stays "lit").
     *
     * @return True if calling.
     */
    @LuaFunction
    public final boolean isCalling() {
        return this.blockEntity.getBlockState().getValue(ElevatorContactBlock.CALLING);
    }

    /**
     * Check whether this contact is currently emitting redstone power —
     * briefly true when the cabin is at this floor.
     *
     * @return True if powering.
     */
    @LuaFunction
    public final boolean isPowering() {
        return this.blockEntity.getBlockState().getValue(ElevatorContactBlock.POWERING);
    }

    /**
     * Get the short name of whichever floor the cabin most recently visited
     * on this column. Same value the {@code current_floor} display-link
     * source reads.
     *
     * @return The last-reported floor's short name.
     */
    @LuaFunction
    public final String getLastReportedFloor() {
        return this.blockEntity.lastReportedCurrentFloor;
    }

    // --- Action ---

    /**
     * Call the elevator to this floor — programmatic equivalent of a redstone
     * pulse on the contact, or the in-cabin Contraption Controls UI selecting
     * this floor. Same code path as the floor-select packet handler.
     */
    @LuaFunction(mainThread = true)
    public final void call() throws LuaException {
        final ColumnCoords coords = this.blockEntity.columnCoords;
        if (coords == null) throw new LuaException("contact has no column");
        final Level level = this.blockEntity.getLevel();
        if (level == null) throw new LuaException("level not loaded");
        final ElevatorColumn column = ElevatorColumn.get(level, coords);
        if (column == null) throw new LuaException("column not found");

        final BlockPos pos = this.blockEntity.getBlockPos();
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof final ElevatorContactBlock ecb)) {
            throw new LuaException("block is not an elevator contact");
        }
        ecb.callToContactAndUpdate(column, state, level, pos, false);
    }
}
