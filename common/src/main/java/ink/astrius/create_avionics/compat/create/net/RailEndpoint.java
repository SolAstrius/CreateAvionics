package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import ink.astrius.create_avionics.net.Endpoint;
import net.createmod.catnip.data.Couple;

/**
 * An {@link Endpoint} that sits somewhere on a Create track graph.
 *
 * <p>Positioned exactly the way Create positions its own signals,
 * stations and observers — a pair of node locations naming the edge, and
 * a distance along it — because that is the only frame in which "how far
 * apart are these two things, along the track" is answerable. A block
 * position could not answer it: two modems can be four blocks apart in
 * the world and sixty blocks apart along the rail, or share a diamond
 * crossing and have no rail path between them at all.</p>
 *
 * <p>Mirroring {@code TrackEdgePoint}'s own shape means a rail modem that
 * registers as a real edge point satisfies this by handing over the
 * fields it already has.</p>
 */
public interface RailEndpoint extends Endpoint {

    /**
     * The two nodes bounding the graph edge this endpoint sits on.
     *
     * @return The edge, as its endpoint node locations.
     */
    Couple<TrackNodeLocation> edgeLocation();

    /**
     * How far along {@link #edgeLocation()} this endpoint sits, measured
     * from the first of the two nodes.
     *
     * @return The offset in blocks.
     */
    double edgePosition();
}
