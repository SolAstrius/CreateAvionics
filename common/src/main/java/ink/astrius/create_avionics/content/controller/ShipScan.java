package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.peripheral.IPeripheral;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Finds everything on a ship that a computer could talk to. A ship is a Sable sub-level; its
 * blocks live in the sub-level's plot, a region of ordinary chunks in the same level.
 */
public final class ShipScan {

    /**
     * The peripheral a cable would see at a position, or null. Set by the loader: the capability
     * lookup is a NeoForge addition to {@code Level}, which {@code common} can't call.
     */
    public static BiFunction<Level, BlockPos, @Nullable IPeripheral> lookup = (level, pos) -> null;

    public record Found(BlockPos pos, BlockEntity blockEntity, IPeripheral peripheral) {
    }

    private ShipScan() {
    }

    /** The ship a block entity is on, or null on the ground. */
    public static @Nullable SubLevel shipOf(final BlockEntity blockEntity) {
        return Sable.HELPER.getContaining(blockEntity);
    }

    /** Every peripheral on the ship, in a stable order (by position). */
    public static List<Found> scan(final Level level, final SubLevel ship) {
        final List<Found> found = new ArrayList<>();
        for (final PlotChunkHolder holder : ship.getPlot().getLoadedChunks()) {
            final LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            for (final BlockEntity be : chunk.getBlockEntities().values()) {
                final IPeripheral peripheral = lookup.apply(level, be.getBlockPos());
                if (peripheral != null) {
                    found.add(new Found(be.getBlockPos().immutable(), be, peripheral));
                }
            }
        }
        found.sort((a, b) -> Long.compare(a.pos().asLong(), b.pos().asLong()));
        return found;
    }
}
