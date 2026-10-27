package ink.astrius.create_avionics.content.controller;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Keeps track of the ship the controller is on: rescans it for sensors every couple of seconds.
 * The channels are for gauges, which can't combine sensor readings themselves; computers get
 * the sensors directly instead, through the controller's wired bridge, and do their own maths.
 */
public class AvionicsControllerBlockEntity extends BlockEntity {

    public static final int SCAN_INTERVAL = 40;

    private @Nullable UUID ship;
    private List<ShipScan.Found> found = List.of();
    private volatile ChannelMap channels = new ChannelMap(List.of(), Direction.NORTH);

    public AvionicsControllerBlockEntity(final BlockPos pos, final BlockState state) {
        super(Controllers.BLOCK_ENTITY.get(), pos, state);
    }

    /** The ship's nose, as marked by the controller's facing. */
    public Direction forward() {
        return this.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    public @Nullable UUID ship() {
        return this.ship;
    }

    public List<ShipScan.Found> found() {
        return this.found;
    }

    public List<Channel> channels() {
        return this.channels.channels();
    }

    /** A channel's current value, or empty if the ship can't measure it right now. */
    public OptionalDouble read(final Channel channel) {
        return this.channels.read(channel);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final AvionicsControllerBlockEntity be) {
        if ((level.getGameTime() + pos.hashCode()) % SCAN_INTERVAL == 0) {
            be.rescan(level);
        }
    }

    private void rescan(final Level level) {
        final SubLevel subLevel = ShipScan.shipOf(this);
        if (subLevel == null || subLevel.isRemoved()) {
            this.ship = null;
            this.found = List.of();
        } else {
            this.ship = subLevel.getUniqueId();
            this.found = ShipScan.scan(level, subLevel);
        }
        this.channels = new ChannelMap(this.found, this.forward());
    }

}
