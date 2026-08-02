package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.net.MacAddress;
import net.createmod.catnip.data.Couple;
import net.minecraft.resources.ResourceLocation;

/**
 * A rail modem's presence on a track graph, registered as a first-class
 * {@link EdgePointType} alongside Create's own signal, station and
 * observer.
 *
 * <p>Registering rather than merely reading the graph buys three things
 * that would otherwise all need hand-rolling: Create persists a stable
 * {@code id} and re-attaches by it across a reload, it maintains our
 * position through track edits, and {@code TrackGraph#getPoints} then
 * answers "every modem on this network" in one call — which is exactly
 * the discovery primitive the mesh is built on.</p>
 *
 * <p>The persisted {@code id} doubles as the modem's link-layer identity:
 * {@link #address()} derives a MAC from it, so an endpoint keeps the same
 * address across a world reload without serialising it separately. See
 * {@link MacAddress#ofStableId}.</p>
 */
public class RailModemPoint extends SingleBlockEntityEdgePoint implements RailEndpoint {

    /** Registry id for this edge point type. */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateAvionics.MOD_ID, "rail_modem");

    /**
     * The registered type. Create keys {@code EdgePointType.TYPES} by id
     * and serialises points through it, so this must be created exactly
     * once, before any point is read back from disk.
     */
    public static final EdgePointType<RailModemPoint> TYPE =
            EdgePointType.register(ID, RailModemPoint::new);

    /** Set by the block entity each tick it is loaded; null when unloaded. */
    private RailModemBlockEntity blockEntity;

    @Override
    public MacAddress address() {
        return MacAddress.ofStableId(this.getId());
    }

    @Override
    public Couple<TrackNodeLocation> edgeLocation() {
        return this.edgeLocation;
    }

    @Override
    public double edgePosition() {
        return this.position;
    }

    @Override
    public void deliver(final byte[] frame, final double qualityDb) {
        final RailModemBlockEntity be = this.blockEntity;
        if (be != null) be.onFrameReceived(frame, qualityDb);
    }

    /**
     * Bind the loaded block entity that backs this point, so delivered
     * frames have somewhere to go.
     *
     * @param be The block entity, or null when it unloads.
     */
    public void bind(final RailModemBlockEntity be) {
        this.blockEntity = be;
    }

    /** @return Whether a loaded block entity is currently backing this point. */
    public boolean isLoaded() {
        return this.blockEntity != null;
    }
}
