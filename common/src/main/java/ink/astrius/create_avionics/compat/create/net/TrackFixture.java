package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import ink.astrius.create_avionics.net.MacAddress;
import net.createmod.catnip.data.Couple;

/**
 * Anything already fixed to the track that is not one of our modems — a
 * signal, a station, an observer — as a station on the medium.
 *
 * <p>These are peers, not scenery. Every one of them is bolted to the
 * same conductor the modems are, sits at a known point along it, and —
 * once the command sub-protocol lands — is something a script will want
 * to address directly: hold this signal at danger, reserve that station
 * for a train, change that observer's filter. Giving them addresses in
 * the same space as the modems is what makes that possible, and it means
 * the attenuation model gates control for free: a signal you cannot hear
 * is a signal you cannot command.</p>
 *
 * <p>Addresses are as persistent as a modem's, and for the same reason.
 * {@code TrackEdgePoint.write} stores its {@code id} as a UUID in NBT, so
 * the identity survives a reload and {@link MacAddress#ofStableId}
 * reproduces the same address from it every time.</p>
 *
 * <p>{@link #deliver} accepts nothing yet — the command sub-protocol is
 * not written. Until it is, these peers are addressable and audible but
 * deaf, which is a strictly better starting point than being invisible.</p>
 */
public record TrackFixture(TrackEdgePoint point) implements RailEndpoint {

    /**
     * Whether this fixture has an identity yet, and so an address.
     *
     * <p>Create assigns a point's id before putting it on the graph, so
     * this is true for anything actually attached. It is checked anyway
     * because the alternative is a null dereference in the middle of a
     * survey, and a fixture with no identity is simply not addressable
     * yet — the honest thing is to leave it out until it is.</p>
     */
    public boolean hasIdentity() {
        return this.point.getId() != null;
    }

    @Override
    public MacAddress address() {
        return MacAddress.ofStableId(this.point.getId());
    }

    /**
     * What kind of fixture this is — {@code signal}, {@code station},
     * {@code observer}, or whatever another addon registered.
     *
     * @return The edge point type's registry path.
     */
    public String kind() {
        return this.point.getType().getId().getPath();
    }

    @Override
    public Couple<TrackNodeLocation> edgeLocation() {
        return this.point.edgeLocation;
    }

    @Override
    public double edgePosition() {
        return this.point.position;
    }

    @Override
    public void deliver(final byte[] frame, final double qualityDb) {
        // Addressable, but nothing acts on a frame yet: the command
        // sub-protocol that would let a script drive a signal or a station
        // from here is still to be written.
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof final TrackFixture other && this.point.getId().equals(other.point.getId());
    }

    @Override
    public int hashCode() {
        return this.point.getId().hashCode();
    }
}
