package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Rail topology and carriage location for a gantry, kept out of the peripheral
 * so it can be reasoned about (and tested) on its own.
 *
 * <p>Two upstream facts drive everything here, and both are easy to get
 * backwards:</p>
 *
 * <ul>
 *   <li>{@code GantryCarriageBlock.FACING} points <em>away</em> from the shaft
 *       the carriage is attached to — {@code GantryCarriageBlock.canSurvive}
 *       looks for its shaft at {@code pos.relative(FACING.getOpposite())}, and
 *       {@code GantryShaftBlockEntity.checkAttachedCarriageBlocks} matches on
 *       {@code FACING == d} where {@code d} points shaft → carriage.</li>
 *   <li>Shafts join into one rail only on <em>exact</em> facing equality, not
 *       shared axis ({@code GantryShaftBlock.updateShape}). Sneak-placing
 *       against a shaft deliberately yields the opposite facing, so abutting
 *       opposed rails are a normal build, not a corner case.</li>
 * </ul>
 *
 * <p>While a gantry is moving there is no carriage block at all: assembly
 * anchors the contraption at the carriage's own position and removes its
 * blocks from the world, so the carriage becomes part of a
 * {@link GantryContraptionEntity}. Locating it therefore means checking the
 * entity first and only then falling back to a block scan.</p>
 */
public final class GantryRail {

    /** Hard cap on how far a rail walk will travel, in blocks. */
    public static final int MAX_LENGTH = 256;

    /** Hoisted: {@code Direction.values()} clones its array on every call. */
    private static final Direction[] DIRECTIONS = Direction.values();

    private GantryRail() {
    }

    /** What the rail is doing, as reported to Lua. */
    public enum State {
        /** Carriage present, at rest, and its last assembly attempt did not fail. */
        PARKED,
        /** Carriage assembled and travelling. */
        MOVING,
        /** Carriage assembled but blocked; it will not move until cleared. */
        STALLED,
        /** Carriage present but its last assembly attempt failed. */
        FAILED;

        public String serialized() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A rail as seen from one of its shafts. {@code start} is the {@code start}
     * shaft — the end opposite {@code facing} — and {@code index} is the
     * observing shaft's own offset from it.
     */
    public record Rail(Direction facing, BlockPos start, int length, int index) {

        public Axis axis() {
            return this.facing.getAxis();
        }

        /**
         * This position's offset from {@link #start} along the rail axis, signed
         * so it counts up in the rail's own direction of travel ({@link #facing}).
         *
         * <p>A raw coordinate delta is not enough: on a rail whose facing points
         * in a negative axis direction (west, north, down), travelling away from
         * {@code start} <em>decreases</em> the raw coordinate, so the delta comes
         * out negative for every real position except {@code start} itself.</p>
         */
        public int coord(final BlockPos pos) {
            final int delta = this.axis().choose(pos.getX(), pos.getY(), pos.getZ())
                    - this.axis().choose(this.start.getX(), this.start.getY(), this.start.getZ());
            return delta * this.facing.getAxisDirection().getStep();
        }

        /** This vector's offset from {@link #start} along the rail axis; see {@link #coord(BlockPos)}. */
        public double coord(final Vec3 vec) {
            final double delta = this.axis().choose(vec.x, vec.y, vec.z)
                    - this.axis().choose(this.start.getX(), this.start.getY(), this.start.getZ());
            return delta * this.facing.getAxisDirection().getStep();
        }

        /** Whether a position is one of this rail's shaft positions. */
        public boolean contains(final BlockPos pos) {
            final int offset = this.coord(pos);
            if (offset < 0 || offset >= this.length) return false;
            return this.start.relative(this.facing, offset).equals(pos);
        }

        /** The far end of the rail — the {@code end} shaft. */
        public BlockPos end() {
            return this.start.relative(this.facing, this.length - 1);
        }
    }

    /**
     * A located carriage. {@code position} is a rail index measured from
     * {@link Rail#start} — fractional while moving, whole while parked.
     * {@code blockPos}, {@code error} and {@code remaining} are nil when they
     * do not apply.
     */
    public record Carriage(
            double position,
            State state,
            BlockPos blockPos,
            String error,
            Double remaining) {
    }

    /** Walk the rail this shaft belongs to. */
    public static Rail of(final GantryShaftBlockEntity shaft) {
        final Direction facing = shaft.getBlockState().getValue(GantryShaftBlock.FACING);
        final Level level = shaft.getLevel();
        final BlockPos pos = shaft.getBlockPos();
        final int behind = walk(level, pos, facing.getOpposite(), facing);
        final int ahead = walk(level, pos, facing, facing);
        return new Rail(facing, pos.relative(facing.getOpposite(), behind), 1 + behind + ahead, behind);
    }

    /** Locate the carriage on this rail, moving or parked, or nil if there is none. */
    public static Carriage carriage(final Level level, final Rail rail) {
        if (level == null) return null;

        final GantryContraptionEntity moving = movingCarriage(level, rail);
        if (moving != null) {
            return new Carriage(
                    rail.coord(moving.getAnchorVec()),
                    moving.isStalled() ? State.STALLED : State.MOVING,
                    null,
                    null,
                    moving.sequencedOffsetLimit >= 0 ? moving.sequencedOffsetLimit : null);
        }

        return parkedCarriage(level, rail);
    }

    /**
     * The contraption entity for this rail's carriage while it travels, or nil.
     *
     * <p>Membership is decided exactly as {@code GantryContraptionEntity} does
     * it in {@code checkPinionShaft}: step from the anchor back along the
     * carriage's facing to find the shaft it rides, then ask whether that shaft
     * is ours. Nothing else identifies a contraption with a rail — two parallel
     * rails a block apart both intersect the same search volume.</p>
     */
    public static GantryContraptionEntity movingCarriage(final Level level, final Rail rail) {
        if (level == null) return null;

        // A carriage sits one block off the rail, so the rail volume has to be
        // grown before it can contain one; contraption bounding boxes are large
        // and generous, and the shaft check below rejects any false positive.
        final AABB search = AABB.encapsulatingFullBlocks(rail.start(), rail.end()).inflate(1.0);

        for (final GantryContraptionEntity entity
                : level.getEntitiesOfClass(GantryContraptionEntity.class, search)) {
            if (!(entity.getContraption() instanceof final GantryContraption contraption)) continue;
            final BlockPos shaftPos = BlockPos
                    .containing(entity.getAnchorVec().add(0.5, 0.5, 0.5))
                    .relative(contraption.getFacing().getOpposite());
            if (rail.contains(shaftPos)) return entity;
        }
        return null;
    }

    private static Carriage parkedCarriage(final Level level, final Rail rail) {
        BlockPos shaftPos = rail.start();
        for (int i = 0; i < rail.length(); i++) {
            for (final Direction d : DIRECTIONS) {
                if (d.getAxis() == rail.axis()) continue;  // skip along-rail neighbours
                final BlockPos candidate = shaftPos.relative(d);
                if (!level.isLoaded(candidate)) continue;
                final BlockState state = level.getBlockState(candidate);
                if (!AllBlocks.GANTRY_CARRIAGE.has(state)) continue;
                // FACING points shaft → carriage, i.e. away from the shaft.
                if (state.getValue(GantryCarriageBlock.FACING) != d) continue;

                final String error = assemblyError(level, candidate);
                return new Carriage(i, error == null ? State.PARKED : State.FAILED, candidate, error, null);
            }
            shaftPos = shaftPos.relative(rail.facing());
        }
        return null;
    }

    private static String assemblyError(final Level level, final BlockPos carriagePos) {
        if (!(level.getBlockEntity(carriagePos) instanceof final GantryCarriageBlockEntity be)) return null;
        final AssemblyException e = be.getLastAssemblyException();
        if (e == null || e.component == null) return null;
        return e.component.getString();
    }

    /**
     * Count contiguous shafts from {@code from}, exclusive, stepping in
     * {@code step}. Bounded by {@link #MAX_LENGTH}, and stops at unloaded
     * chunks rather than through them: {@code Level.getBlockState} resolves its
     * chunk with {@code requireChunk = true}, so an unguarded walk down a rail
     * pointing at ungenerated terrain would load — and generate — it, on the
     * server thread, from a Lua call. Create's own rail walk in
     * {@code GantryShaftBlock.neighborChanged} guards the same way.
     */
    private static int walk(final Level level, final BlockPos from, final Direction step, final Direction facing) {
        if (level == null) return 0;
        BlockPos cursor = from.relative(step);
        int count = 0;
        while (count < MAX_LENGTH) {
            if (!level.isLoaded(cursor)) break;
            final BlockState state = level.getBlockState(cursor);
            if (!AllBlocks.GANTRY_SHAFT.has(state)) break;
            if (state.getValue(GantryShaftBlock.FACING) != facing) break;
            count++;
            cursor = cursor.relative(step);
        }
        return count;
    }
}
