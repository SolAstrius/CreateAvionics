package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.Create;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import ink.astrius.create_avionics.net.Frame;

import java.nio.charset.StandardCharsets;
import ink.astrius.create_avionics.registry.AvionicsBlockEntities;
import ink.astrius.create_avionics.net.MacAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A station on the rail medium.
 *
 * <p>Attaches to the track graph through Create's own
 * {@link TrackTargetingBehaviour}, the same machinery a signal, station
 * or observer uses — so placement, reload re-attachment and survival
 * across track edits all behave exactly as players already expect from
 * those blocks, rather than being reimplemented slightly differently
 * here.</p>
 *
 * <p>What the block does with a received frame is deliberately not
 * decided at this layer: it demultiplexes on the frame's sub-protocol and
 * hands off. Encapsulated ComputerCraft traffic and rail control traffic
 * are peers riding one medium, and a future bridged NIC's real Ethernet
 * frames would be a third.</p>
 */
public class RailModemBlockEntity extends SmartBlockEntity {

    public TrackTargetingBehaviour<RailModemPoint> edgePoint;

    public AbstractComputerBehaviour computerBehaviour;

    /** Ticks remaining on the activity light; refreshed by traffic. */
    private int activityTicks;

    public RailModemBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /**
     * Vanilla's block entity factory hands over only a position and a
     * state, while Create's {@code SmartBlockEntity} wants its own type as
     * well; this bridges the two so the type can be built from a plain
     * constructor reference.
     */
    public RailModemBlockEntity(final BlockPos pos, final BlockState state) {
        this(AvionicsBlockEntities.RAIL_MODEM.get(), pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        behaviours.add(this.edgePoint = new TrackTargetingBehaviour<>(this, RailModemPoint.TYPE));
        behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null || this.level.isClientSide) return;

        // The behaviour resolves the point lazily -- a freshly placed or
        // freshly loaded modem sits inert until the graph is ready for it.
        final RailModemPoint point = this.point();
        if (point != null) point.bind(this);

        if (this.activityTicks > 0 && --this.activityTicks == 0) {
            this.setLit(false);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        final RailModemPoint point = this.point();
        if (point != null) point.bind(null);
    }

    /** @return This modem's point on the graph, or null before it resolves. */
    @Nullable
    public RailModemPoint point() {
        return this.edgePoint == null ? null : this.edgePoint.getEdgePoint();
    }

    /** @return The graph this modem sits on, or null if it has not resolved one. */
    @Nullable
    public TrackGraph graph() {
        final RailModemPoint point = this.point();
        if (point == null || this.level == null) return null;
        return Create.RAILWAYS.sided(this.level).getGraph(this.level, point.edgeLocation().getFirst());
    }

    /** @return The medium this modem transmits on, or null if unattached. */
    @Nullable
    public RailMedium medium() {
        final TrackGraph graph = this.graph();
        if (graph == null) return null;
        return new RailMedium(graph, () -> stationsOn(graph));
    }

    /**
     * Everything bolted to this track network, as stations on the medium.
     *
     * <p>Modems and Create's own fixtures alike: a signal or a station is
     * attached to the same conductor and addressable in the same space,
     * so the medium should see them all. Walking every registered edge
     * point type rather than naming three means an edge point some other
     * addon registers is picked up without this needing to know about
     * it.</p>
     *
     * <p>Unloaded modems are skipped -- they are still on the graph, but
     * there is no block entity there to hand a frame to. Fixtures have no
     * such requirement: their state lives on the graph itself.</p>
     */
    private static List<RailEndpoint> stationsOn(final TrackGraph graph) {
        final List<RailEndpoint> out = new ArrayList<>();
        for (final EdgePointType<?> type : EdgePointType.TYPES.values()) {
            for (final TrackEdgePoint point : graph.getPoints(type)) {
                if (point instanceof final RailModemPoint modem) {
                    if (modem.isLoaded()) out.add(modem);
                } else {
                    out.add(new TrackFixture(point));
                }
            }
        }
        return out;
    }

    /** @return This modem's link-layer address, or null before it resolves. */
    @Nullable
    public MacAddress address() {
        final RailModemPoint point = this.point();
        return point == null ? null : point.address();
    }

    /**
     * Put a frame on the medium.
     *
     * @param frame The frame to transmit.
     * @return Whether it was transmitted; false if this modem is not
     * attached to a graph yet.
     */
    public boolean transmit(final Frame frame) {
        final RailMedium medium = this.medium();
        final RailModemPoint point = this.point();
        if (medium == null || point == null) return false;
        this.noteActivity();
        medium.transmit(point, frame.toBytes());
        return true;
    }

    /**
     * Accept a frame that reached this modem. Called from
     * {@link RailModemPoint#deliver}.
     *
     * @param raw       The frame as transmitted.
     * @param qualityDb Received margin in dB.
     */
    public void onFrameReceived(final byte[] raw, final double qualityDb) {
        this.noteActivity();

        final Frame frame;
        try {
            frame = Frame.parse(raw);
        } catch (final IllegalArgumentException malformed) {
            return; // junk on the wire; resync on the next frame
        }

        final MacAddress self = this.address();
        if (self != null && !frame.addressedTo(self)) return;

        switch (frame.subProtocol()) {
            case Frame.SUB_RAIL_CONTROL -> this.queueMessageEvent(frame, qualityDb);
            case Frame.SUB_COMPUTERCRAFT -> { /* encapsulated CC packets, not wired yet */ }
            default -> { /* foreign traffic -- a bridged guest's own frames */ }
        }
    }

    /**
     * Hand a received message to any attached computer.
     *
     * <p>The signal margin travels with it deliberately: a receiver that
     * knows how strongly something arrived knows how far away it came
     * from, which is what makes locating a transmitter from two or more
     * listeners possible at all.</p>
     */
    private void queueMessageEvent(final Frame frame, final double qualityDb) {
        if (this.computerBehaviour == null || !this.computerBehaviour.hasAttachedComputer()) return;
        if (!(this.computerBehaviour.getPeripheralCapability() instanceof final RailModemPeripheral peripheral)) return;
        peripheral.onMessage(frame.source().toString(),
                new String(frame.body(), StandardCharsets.UTF_8), qualityDb);
    }

    private void noteActivity() {
        this.activityTicks = ACTIVITY_TICKS;
        this.setLit(true);
    }

    private void setLit(final boolean lit) {
        if (this.level == null) return;
        final BlockState state = this.getBlockState();
        if (!state.hasProperty(RailModemBlock.LIT) || state.getValue(RailModemBlock.LIT) == lit) return;
        this.level.setBlock(this.worldPosition, state.setValue(RailModemBlock.LIT, lit), Block.UPDATE_ALL);
    }

    /** How long the activity light stays on after traffic. */
    private static final int ACTIVITY_TICKS = 10;
}
