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

    private final ShipBridge bridge = new ShipBridge(this);
    /** Scan on the first tick after loading. */
    private boolean rescanSoon = true;
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

    public ShipBridge bridge() {
        return this.bridge;
    }

    /** Rescan on the next tick instead of waiting, e.g. after a neighbour or peer changed. */
    public void rescanSoon() {
        this.rescanSoon = true;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final AvionicsControllerBlockEntity be) {
        if (be.rescanSoon || (level.getGameTime() + pos.hashCode()) % SCAN_INTERVAL == 0) {
            be.rescanSoon = false;
            be.rescan(level);
        }
    }

    private void rescan(final Level level) {
        final SubLevel subLevel = ShipScan.shipOf(this);
        final UUID now = subLevel == null || subLevel.isRemoved() ? null : subLevel.getUniqueId();
        if (this.ship != null && !this.ship.equals(now)) {
            this.leaveShip();
        }
        this.ship = now;
        this.found = now == null ? List.of() : ShipScan.scan(level, subLevel);
        this.channels = new ChannelMap(this.found, this.forward());

        this.bridge.connectNeighbours();
        if (now == null) {
            this.bridge.linkPeers(List.of());
            this.bridge.publish(List.of(), false);
            return;
        }
        ShipControllers.join(now, this);
        final List<AvionicsControllerBlockEntity> peers = ShipControllers.on(now);
        this.bridge.linkPeers(peers);
        this.bridge.publish(this.found, !peers.isEmpty() && peers.get(0) == this);
    }

    private void leaveShip() {
        if (this.ship == null) {
            return;
        }
        ShipControllers.leave(this.ship, this);
        // The next controller in line takes over publishing straight away
        for (final AvionicsControllerBlockEntity other : ShipControllers.on(this.ship)) {
            other.rescanSoon();
        }
    }

    /** Also called when the chunk unloads. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            this.leaveShip();
            this.bridge.remove();
        }
    }

}
