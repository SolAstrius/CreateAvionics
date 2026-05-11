package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared logic for the kinetic SCADA pack. Used by
 * {@link ink.astrius.create_avionics.compat.simulated.peripherals.generic.KineticSource},
 * the {@code GenericSource} that provides the pack to every kinetic peripheral.
 */
public final class KineticReadback {

    private static final int MAX_ANCHOR_WALK = 256;

    private KineticReadback() {}

    public static String selfId(final KineticBlockEntity be) {
        return idOf(be.getBlockPos().asLong());
    }

    public static String sourceId(final KineticBlockEntity be) {
        final BlockPos src = be.source;
        return src == null ? null : idOf(src.asLong());
    }

    public static String networkId(final KineticBlockEntity be) {
        final Long n = be.network;
        return n == null ? null : idOf(n);
    }

    public static String subnetworkAnchorId(final KineticBlockEntity be) {
        final Level level = be.getLevel();
        if (level == null) return null;

        if (be instanceof GeneratingKineticBlockEntity || be instanceof SplitShaftBlockEntity) {
            return selfId(be);
        }

        BlockPos cursor = be.source;
        for (int i = 0; i < MAX_ANCHOR_WALK && cursor != null; i++) {
            final BlockEntity neighbour = level.getBlockEntity(cursor);
            if (!(neighbour instanceof final KineticBlockEntity kbe)) return null;
            if (kbe instanceof GeneratingKineticBlockEntity || kbe instanceof SplitShaftBlockEntity) {
                return idOf(cursor.asLong());
            }
            cursor = kbe.source;
        }
        return null;
    }

    /**
     * Encode any block position as the same opaque-id flavor used by the
     * kinetic SCADA pack. Useful for peripherals exposing topology pointers
     * (e.g. swivel bearing's plate position) so they match {@code getSelfId}
     * comparisons cleanly.
     */
    public static String idOf(final BlockPos pos) {
        return idOf(pos.asLong());
    }

    /**
     * Encode a Create elevator column's coordinates as an opaque token. All
     * peripherals on the same column return the same string, suitable for
     * grouping pulleys and contacts in a SCADA dashboard.
     */
    public static String columnIdOf(final ColumnCoords coords) {
        return String.format("%08x%08x%x", coords.x(), coords.z(), coords.side().ordinal());
    }

    public static String kindOf(final KineticBlockEntity be) {
        if (be instanceof GeneratingKineticBlockEntity) return "generator";
        if (be instanceof SplitShaftBlockEntity) return "split_shaft";
        if (be.calculateStressApplied() > 0f) return "consumer";
        return "passthrough";
    }

    private static String idOf(final long packed) {
        return String.format("%016x", packed);
    }
}
