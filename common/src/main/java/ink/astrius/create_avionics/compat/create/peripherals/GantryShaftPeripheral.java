package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A gantry shaft — the rail along which a gantry carriage slides. The natural
 * place to wire kinetic input and the natural primary peripheral for the
 * gantry system: control speed and direction here, observe rail topology
 * (length, this shaft's position, where the carriage is) by walking adjacent
 * shafts.
 *
 * <p>Driving programmatically goes via an upstream
 * {@code Create_SequencedGearshift}'s {@code move(distance)}, just like for
 * pistons.</p>
 *
 * @cc.module Create_GantryShaft
 */
public class GantryShaftPeripheral extends KineticPeripheral<GantryShaftBlockEntity> {

    private static final int MAX_RAIL_LENGTH = 256;

    public GantryShaftPeripheral(final GantryShaftBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_GantryShaft";
    }

    // --- Per-shaft state ---

    /**
     * Get this shaft's role in the rail.
     * One of {@code start}, {@code middle}, {@code end}, {@code single}.
     *
     * @return The part string.
     */
    @LuaFunction
    public final String getPart() {
        return this.blockEntity.getBlockState().getValue(GantryShaftBlock.PART).getSerializedName();
    }

    /**
     * Get the rail's axis.
     *
     * @return The axis as {@code "x"}, {@code "y"}, or {@code "z"}.
     */
    @LuaFunction
    public final String getAxis() {
        return this.facing().getAxis().getSerializedName();
    }

    /**
     * Check whether this shaft is currently powered (redstone). Powering any
     * shaft in the rail inverts the carriage's direction of travel.
     *
     * @return True if powered.
     */
    @LuaFunction
    public final boolean isPowered() {
        return this.blockEntity.getBlockState().getValue(GantryShaftBlock.POWERED);
    }

    /**
     * Get the linear speed at which a carriage on this rail will move.
     * Signed: positive moves the carriage along the rail's facing direction,
     * negative moves it opposite. Factors in the redstone-inverted direction
     * (so a powered shaft with positive kinetic input gives a negative
     * movement speed). 0 when stalled or unpowered.
     *
     * @return The movement speed in blocks per tick.
     */
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getPinionMovementSpeed();
    }

    /**
     * Check whether a carriage may currently move along this shaft.
     *
     * @return True if assembly conditions are met.
     */
    @LuaFunction
    public final boolean canAssembleOn() {
        return this.blockEntity.canAssembleOn();
    }

    // --- Rail topology ---

    /**
     * Get the total length of the rail this shaft is part of (number of
     * contiguous shaft blocks along the rail axis).
     *
     * @return The rail length in blocks.
     */
    @LuaFunction(mainThread = true)
    public final int getRailLength() {
        final Direction f = this.facing();
        return 1 + this.walk(f) + this.walk(f.getOpposite());
    }

    /**
     * Get this shaft's index along the rail, counted from the {@code start}
     * end. The {@code start} shaft is index 0; the {@code end} shaft is
     * {@code getRailLength() - 1}.
     *
     * @return The 0-based index.
     */
    @LuaFunction(mainThread = true)
    public final int getRailIndex() {
        return this.walk(this.facing().getOpposite());
    }

    /**
     * Get the carriage's index along the rail, or nil if no carriage is
     * attached. Same units as {@link #getRailIndex} — 0 at the {@code start}
     * end, {@code getRailLength() - 1} at the {@code end}.
     *
     * @return The carriage's rail index, or nil.
     */
    @LuaFunction(mainThread = true)
    public final Integer getCarriagePosition() {
        return this.findCarriagePosition();
    }

    /**
     * Check whether a carriage is currently attached to this rail.
     *
     * @return True if a carriage is found.
     */
    @LuaFunction(mainThread = true)
    public final boolean hasCarriage() {
        return this.findCarriagePosition() != null;
    }

    /**
     * Get the id of the carriage block attached to this rail, or nil if none
     * is found. Same opaque-token flavor as {@code getSelfId} on a peripheral
     * wrapping the carriage.
     *
     * @return The carriage's id, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getCarriageId() {
        final BlockPos p = this.findCarriageBlockPos();
        return p == null ? null : KineticReadback.idOf(p);
    }

    // --- Helpers ---

    private Direction facing() {
        return this.blockEntity.getBlockState().getValue(GantryShaftBlock.FACING);
    }

    private int walk(final Direction direction) {
        final Level level = this.blockEntity.getLevel();
        if (level == null) return 0;
        final Axis railAxis = this.facing().getAxis();
        BlockPos cursor = this.blockEntity.getBlockPos().relative(direction);
        int count = 0;
        while (count < MAX_RAIL_LENGTH) {
            final BlockState s = level.getBlockState(cursor);
            if (!(s.getBlock() instanceof GantryShaftBlock)) break;
            if (s.getValue(GantryShaftBlock.FACING).getAxis() != railAxis) break;
            count++;
            cursor = cursor.relative(direction);
        }
        return count;
    }

    private Integer findCarriagePosition() {
        final BlockPos p = this.findCarriageBlockPos();
        if (p == null) return null;
        final BlockPos start = this.railStart();
        final Axis axis = this.facing().getAxis();
        return switch (axis) {
            case X -> p.getX() - start.getX();
            case Y -> p.getY() - start.getY();
            case Z -> p.getZ() - start.getZ();
        };
    }

    private BlockPos railStart() {
        final Direction back = this.facing().getOpposite();
        final int steps = this.walk(back);
        return this.blockEntity.getBlockPos().relative(back, steps);
    }

    private BlockPos findCarriageBlockPos() {
        final Level level = this.blockEntity.getLevel();
        if (level == null) return null;
        final Direction f = this.facing();
        final Axis railAxis = f.getAxis();

        BlockPos shaftPos = this.railStart();
        final int length = 1 + this.walk(f.getOpposite()) + this.walk(f);
        for (int i = 0; i < length; i++) {
            final BlockPos found = this.checkPerpendicularForCarriage(shaftPos, railAxis);
            if (found != null) return found;
            shaftPos = shaftPos.relative(f);
        }
        return null;
    }

    private BlockPos checkPerpendicularForCarriage(final BlockPos shaftPos, final Axis railAxis) {
        final Level level = this.blockEntity.getLevel();
        if (level == null) return null;
        for (final Direction d : Direction.values()) {
            if (d.getAxis() == railAxis) continue;  // skip along-rail neighbors
            final BlockPos candidate = shaftPos.relative(d);
            final BlockState candidateState = level.getBlockState(candidate);
            if (!AllBlocks.GANTRY_CARRIAGE.has(candidateState)) continue;
            // The carriage's FACING must point back at the shaft (i.e. d.opposite()).
            if (candidateState.getValue(GantryCarriageBlock.FACING) != d.getOpposite()) continue;
            return candidate;
        }
        return null;
    }

}
