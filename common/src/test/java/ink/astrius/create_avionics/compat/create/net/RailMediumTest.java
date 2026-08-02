package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import ink.astrius.create_avionics.net.MacAddress;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Distance here is the whole point of the class: it is measured along
 * track, and every interesting case is one where that disagrees with the
 * straight line between two points.
 */
class RailMediumTest {

    /** A test endpoint that records what the medium delivered to it. */
    private static final class Probe implements RailEndpoint {
        private final MacAddress mac = MacAddress.ofStableId(UUID.randomUUID());
        private final Couple<TrackNodeLocation> edge;
        private final double position;
        private final List<Double> heard = new ArrayList<>();

        Probe(final TrackNodeLocation a, final TrackNodeLocation b, final double position) {
            this.edge = Couple.create(a, b);
            this.position = position;
        }

        @Override
        public MacAddress address() {
            return this.mac;
        }

        @Override
        public Couple<TrackNodeLocation> edgeLocation() {
            return this.edge;
        }

        @Override
        public double edgePosition() {
            return this.position;
        }

        @Override
        public void deliver(final byte[] frame, final double qualityDb) {
            this.heard.add(qualityDb);
        }
    }

    private static TrackNodeLocation at(final double x, final double z) {
        return new TrackNodeLocation(x, 0, z).in(Level.OVERWORLD);
    }

    /** Wire two nodes together in both directions, as a real graph does. */
    private static void connect(final TrackGraph graph, final TrackNode a, final TrackNode b) {
        graph.putConnection(a, b, new TrackEdge(a, b, null, null));
        graph.putConnection(b, a, new TrackEdge(b, a, null, null));
    }

    private static TrackNode node(final TrackGraph graph, final TrackNodeLocation loc) {
        final TrackNode n = new TrackNode(loc, TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
        graph.addNode(n);
        return n;
    }

    @Test
    void distanceFollowsTheTrackNotTheStraightLine() {
        // A long way around: A --100--> B --100--> C, but A and C sit only
        // 20 blocks apart in the world.
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(100, 0);
        final TrackNodeLocation lc = at(10, 0);

        final TrackNode a = node(graph, la);
        final TrackNode b = node(graph, lb);
        final TrackNode c = node(graph, lc);
        connect(graph, a, b);
        connect(graph, b, c);

        final RailMedium medium = new RailMedium(graph);
        final Probe onAB = new Probe(la, lb, 0);      // sitting at A
        final Probe onBC = new Probe(lb, lc, 90);     // 90 along B->C, i.e. at C

        // Straight-line A to C is 10. Along the rail it is 100 + 90 = 190.
        assertEquals(190.0, medium.distance(onAB, onBC), 1e-6);
    }

    @Test
    void aDisconnectedCrossingIsUnreachable() {
        // Two edges that would cross in the world but share no node: Create
        // records a TrackEdgeIntersection and never links them.
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation p1 = at(0, 0);
        final TrackNodeLocation p2 = at(50, 0);
        final TrackNodeLocation q1 = at(25, -25);
        final TrackNodeLocation q2 = at(25, 25);

        connect(graph, node(graph, p1), node(graph, p2));
        connect(graph, node(graph, q1), node(graph, q2));

        final RailMedium medium = new RailMedium(graph);
        final Probe onP = new Probe(p1, p2, 25);
        final Probe onQ = new Probe(q1, q2, 25);

        // They occupy the same spot in the world, and still cannot hear
        // each other — the rails never join.
        assertEquals(Double.POSITIVE_INFINITY, medium.distance(onP, onQ));
        assertEquals(Double.NEGATIVE_INFINITY, medium.linkQualityDb(onP, onQ));
    }

    @Test
    void bothEndpointsOnOneEdgeMeasureAlongIt() {
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(100, 0);
        connect(graph, node(graph, la), node(graph, lb));

        final RailMedium medium = new RailMedium(graph);
        assertEquals(30.0, medium.distance(new Probe(la, lb, 10), new Probe(la, lb, 40)), 1e-6);
    }

    @Test
    void anEdgeRecordedInOppositeOrderStillMeasuresCorrectly() {
        // An edge point stores whichever orientation it was created with,
        // so two modems on one edge can disagree about which node is first.
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(100, 0);
        connect(graph, node(graph, la), node(graph, lb));

        final RailMedium medium = new RailMedium(graph);
        final Probe forward = new Probe(la, lb, 10);   // 10 from A
        final Probe backward = new Probe(lb, la, 60);  // 60 from B, i.e. 40 from A
        assertEquals(30.0, medium.distance(forward, backward), 1e-6);
    }

    @Test
    void aJunctionBranchesAndTheShorterBranchWins() {
        //        B (at 40)
        //       /
        //  A --<
        //       \
        //        C (at 10)
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(40, 0);
        final TrackNodeLocation lc = at(0, 10);

        final TrackNode a = node(graph, la);
        connect(graph, a, node(graph, lb));
        connect(graph, a, node(graph, lc));

        final RailMedium medium = new RailMedium(graph);
        final Probe onB = new Probe(la, lb, 40);   // at B
        final Probe onC = new Probe(la, lc, 10);   // at C

        // Both branches are explored; the path is B -> A -> C.
        assertEquals(50.0, medium.distance(onB, onC), 1e-6);
    }

    @Test
    void qualityFallsLinearlyAndInvertsBackToDistance() {
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(200, 0);
        connect(graph, node(graph, la), node(graph, lb));

        final RailMedium medium = new RailMedium(graph);
        final double q = medium.linkQualityDb(new Probe(la, lb, 0), new Probe(la, lb, 200));

        assertEquals(RailMedium.TX_POWER_DB - 200 * RailMedium.ATTENUATION_DB_PER_BLOCK, q, 1e-6);
        // The inversion is what makes position-fixing from received
        // strength possible at all.
        assertEquals(200.0, RailMedium.distanceForQuality(q), 1e-6);
    }

    @Test
    void deliversOnlyWithinRangeAndNeverToTheSender() {
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(2000, 0);
        connect(graph, node(graph, la), node(graph, lb));

        final RailMedium medium = new RailMedium(graph);
        final Probe sender = new Probe(la, lb, 0);
        final Probe near = new Probe(la, lb, 100);                            // 100 blocks
        final Probe far = new Probe(la, lb, RailMedium.MAX_RANGE_BLOCKS + 50); // past the floor

        medium.attach(sender);
        medium.attach(near);
        medium.attach(far);
        medium.transmit(sender, new byte[]{1, 2, 3});

        assertEquals(1, near.heard.size(), "in range, should hear it");
        assertTrue(far.heard.isEmpty(), "past the attenuation floor");
        assertTrue(sender.heard.isEmpty(), "a station does not receive its own transmission");
    }

    @Test
    void detachedEndpointsStopHearingTraffic() {
        final TrackGraph graph = new TrackGraph();
        final TrackNodeLocation la = at(0, 0);
        final TrackNodeLocation lb = at(100, 0);
        connect(graph, node(graph, la), node(graph, lb));

        final RailMedium medium = new RailMedium(graph);
        final Probe sender = new Probe(la, lb, 0);
        final Probe listener = new Probe(la, lb, 50);
        medium.attach(sender);
        medium.attach(listener);

        medium.transmit(sender, new byte[]{1});
        assertEquals(1, listener.heard.size());

        medium.detach(listener);
        medium.transmit(sender, new byte[]{1});
        assertEquals(1, listener.heard.size(), "no further delivery after detaching");
    }

    @Test
    void mediumIdentityTracksTheUnderlyingGraph() {
        final TrackGraph one = new TrackGraph();
        final TrackGraph two = new TrackGraph();
        assertEquals(new RailMedium(one).id(), new RailMedium(one).id());
        assertFalse(new RailMedium(one).id().equals(new RailMedium(two).id()),
                "two disconnected rail systems are two broadcast domains");
    }
}
