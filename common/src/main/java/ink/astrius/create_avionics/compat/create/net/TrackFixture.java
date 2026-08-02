package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import ink.astrius.create_avionics.net.MacAddress;
import net.createmod.catnip.data.Couple;

/**
 * Anything already fixed to the track that is not one of our modems — a
 * signal, a station, an observer — presented so the medium can measure
 * the distance to it.
 *
 * <p>Every {@code TrackEdgePoint} already carries the two fields the
 * distance walk needs: which edge it sits on and how far along. Wrapping
 * one therefore costs nothing and means the rail-distance logic is
 * written once, rather than once for modems and again for everything
 * else.</p>
 *
 * <p>It is emphatically not a station on the medium: {@link #deliver} is
 * a no-op and it is never returned by {@code endpoints()}. A signal
 * cannot receive a frame. This exists only so "how far is that signal,
 * along the rail" is answerable with the machinery that already
 * exists.</p>
 */
public record TrackFixture(TrackEdgePoint point) implements RailEndpoint {

    @Override
    public MacAddress address() {
        return MacAddress.ofStableId(this.point.getId());
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
        // Not a station; nothing here listens.
    }
}
