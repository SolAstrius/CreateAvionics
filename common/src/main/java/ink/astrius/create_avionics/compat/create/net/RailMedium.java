package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import ink.astrius.create_avionics.net.Endpoint;
import ink.astrius.create_avionics.net.Medium;
import net.createmod.catnip.data.Couple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Supplier;
import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * The rail itself as a shared physical medium — one Create track graph is
 * one broadcast domain.
 *
 * <p>The analogy that holds here is powerline networking rather than
 * radio: signal does not propagate through space, it propagates through
 * the conductor, and it weakens with the length of conductor travelled.
 * So reach is measured in metres of track along the shortest path, never
 * in straight-line distance — two modems can be a few blocks apart across
 * a gap and be effectively unreachable, while two at opposite ends of a
 * long siding hear each other fine.</p>
 *
 * <h2>What counts as connected</h2>
 * <ul>
 *   <li><b>Junctions carry signal.</b> Paths branch through them, and
 *       every branch is explored.</li>
 *   <li><b>Diamond crossings do not.</b> Where two routes physically
 *       cross without their rails joining, Create records a
 *       {@code TrackEdgeIntersection} but never links the two edges in
 *       its adjacency map — they share occupancy for collision safety and
 *       nothing else. Walking that adjacency map therefore excludes them
 *       for free, which is also what a real railway does deliberately:
 *       diamonds get insulated joints precisely so the two routes' track
 *       circuits stay electrically separate.</li>
 *   <li><b>Sharp branches carry signal.</b> Create's
 *       {@code TrackEdge#canTravelTo} rejects turns beyond roughly 29°,
 *       but that is a constraint on <em>trains</em> — a train cannot
 *       hairpin through a junction. Current has no such opinion, so this
 *       walk deliberately ignores that check. Applying it would carve
 *       dead zones around sharp junctions for no physical reason.</li>
 *   <li><b>Portals are free.</b> Create gives an inter-dimensional edge
 *       {@code getLength() == 0}, so signal crosses one at no cost.</li>
 * </ul>
 */
public final class RailMedium implements Medium {

    /**
     * Transmit power in dB. With {@link #ATTENUATION_DB_PER_BLOCK} this
     * sets the usable reach along track at {@value #TX_POWER_DB} /
     * {@value #ATTENUATION_DB_PER_BLOCK} = 500 blocks.
     */
    public static final double TX_POWER_DB = 50.0;

    /**
     * Loss per block of track travelled.
     *
     * <p>Linear in distance, not logarithmic: that is how attenuation in
     * a conductor actually behaves — real cable is specified in dB per
     * 100 m — and unlike a free-space model it inverts cleanly, so a
     * receiver can turn a measured signal strength straight back into a
     * distance. That invertibility is what makes position-fixing from two
     * or more listeners possible at all.</p>
     */
    public static final double ATTENUATION_DB_PER_BLOCK = 0.1;

    /** Received margin below which a frame is not delivered. */
    public static final double FLOOR_DB = 0.0;

    /** Beyond this the path search stops; nothing further could be heard anyway. */
    public static final double MAX_RANGE_BLOCKS = (TX_POWER_DB - FLOOR_DB) / ATTENUATION_DB_PER_BLOCK;

    private final TrackGraph graph;
    private final Set<RailEndpoint> attached = new LinkedHashSet<>();
    private final Supplier<Collection<? extends RailEndpoint>> registered;

    /**
     * A medium with no registered stations of its own; everything on it
     * must be {@link #attach}ed explicitly.
     */
    public RailMedium(final TrackGraph graph) {
        this(graph, List::of);
    }

    /**
     * @param graph      The track network this medium runs on.
     * @param registered Supplies the stations the graph itself knows
     *                   about, re-read on every use. Kept as a supplier
     *                   rather than resolved here so this class never has
     *                   to name Create's edge-point registry — touching
     *                   that drags in the whole mod registration stack,
     *                   which cannot load outside a running game and
     *                   would put the medium's own logic beyond the reach
     *                   of a unit test.
     */
    public RailMedium(final TrackGraph graph, final Supplier<Collection<? extends RailEndpoint>> registered) {
        this.graph = graph;
        this.registered = registered;
    }

    /**
     * Distance along track that a signal of the given received margin must
     * have travelled — the inverse of the attenuation model.
     *
     * <p>Exposed because it is the whole point of a linear model: a
     * listener that reports received dB has, implicitly, reported a
     * distance. Two listeners reporting distances for the same
     * transmission constrain its origin to the intersection of two
     * distance sets on the graph, which on rail — being one-dimensional
     * along any given edge — usually resolves to a single point.</p>
     *
     * @param qualityDb A received margin, as handed to
     *                  {@link Endpoint#deliver}.
     * @return The implied distance in blocks.
     */
    public static double distanceForQuality(final double qualityDb) {
        return (TX_POWER_DB - qualityDb) / ATTENUATION_DB_PER_BLOCK;
    }

    /**
     * Attach an endpoint that is not one of the graph's own registered
     * modems. Production endpoints do not need this — a {@link
     * RailModemPoint} is found through the graph itself — but it lets a
     * test drive the medium without standing up edge points.
     */
    public void attach(final RailEndpoint endpoint) {
        this.attached.add(endpoint);
    }

    /** Undo {@link #attach}. */
    public void detach(final RailEndpoint endpoint) {
        this.attached.remove(endpoint);
    }

    @Override
    public String id() {
        return "rail:" + this.graph.id;
    }

    @Override
    public Set<Endpoint> endpoints() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.stations()));
    }

    /**
     * Every station currently on this medium.
     *
     * <p>Read from the graph on each call rather than cached, because the
     * graph is the authority: Create splits and merges track networks as
     * players lay and break rail, and a cached membership list would
     * quietly diverge the first time someone connected two sidings.
     * Modems whose chunk is unloaded are skipped — they are still on the
     * graph, but nothing is there to receive.</p>
     */
    private Set<RailEndpoint> stations() {
        final Set<RailEndpoint> out = new LinkedHashSet<>(this.attached);
        out.addAll(this.registered.get());
        return out;
    }

    @Override
    public double linkQualityDb(final Endpoint from, final Endpoint to) {
        final double d = this.distance(from, to);
        if (!Double.isFinite(d)) return Double.NEGATIVE_INFINITY;
        return TX_POWER_DB - d * ATTENUATION_DB_PER_BLOCK;
    }

    @Override
    public double distance(final Endpoint from, final Endpoint to) {
        if (!(from instanceof final RailEndpoint a) || !(to instanceof final RailEndpoint b)) {
            return Double.POSITIVE_INFINITY;
        }
        if (a == b) return 0.0;

        double best = Double.POSITIVE_INFINITY;

        // Both on the same edge: the path straight along it never visits a
        // node, so the graph walk below would miss it entirely.
        final Double alongEdge = this.distanceAlongSharedEdge(a, b);
        if (alongEdge != null) best = alongEdge;

        // Otherwise leave via either end of a's edge, arrive via either end
        // of b's. Seeding the search from both of a's ends at once costs one
        // walk instead of two.
        final Map<TrackNode, Double> reach = this.walkFrom(a, best);
        for (final Ends ends : this.endsOf(b)) {
            final Double toNode = reach.get(ends.node());
            if (toNode == null) continue;
            best = Math.min(best, toNode + ends.cost());
        }
        return best;
    }

    @Override
    public void transmit(final Endpoint from, final byte[] frame) {
        for (final RailEndpoint peer : List.copyOf(this.stations())) {
            if (peer == from) continue;
            final double q = this.linkQualityDb(from, peer);
            if (q < FLOOR_DB) continue;
            peer.deliver(frame, q);
        }
    }

    // --- path finding ---

    /** One end of an endpoint's edge, and the cost to walk to it. */
    private record Ends(TrackNode node, double cost) {
    }

    /**
     * The two nodes bounding an endpoint's edge, each with the distance
     * from the endpoint to it. Either may be absent if the graph no longer
     * holds that node — a rail can be dismantled under a modem.
     */
    private List<Ends> endsOf(final RailEndpoint endpoint) {
        final Couple<TrackNodeLocation> edge = endpoint.edgeLocation();
        final TrackNode n1 = this.graph.locateNode(edge.getFirst());
        final TrackNode n2 = this.graph.locateNode(edge.getSecond());
        if (n1 == null || n2 == null) return List.of();

        final TrackEdge e = this.edgeBetween(n1, n2);
        if (e == null) return List.of();

        final double pos = endpoint.edgePosition();
        final List<Ends> out = new ArrayList<>(2);
        out.add(new Ends(n1, Math.max(0, pos)));
        out.add(new Ends(n2, Math.max(0, e.getLength() - pos)));
        return out;
    }

    private TrackEdge edgeBetween(final TrackNode a, final TrackNode b) {
        final Map<TrackNode, TrackEdge> from = this.graph.getConnectionsFrom(a);
        return from == null ? null : from.get(b);
    }

    /**
     * If both endpoints sit on the same edge, the distance straight along
     * it; otherwise null. Handles the two being recorded in opposite node
     * order, which is normal — an edge point stores whichever orientation
     * it was created with.
     */
    private Double distanceAlongSharedEdge(final RailEndpoint a, final RailEndpoint b) {
        final Couple<TrackNodeLocation> ea = a.edgeLocation();
        final Couple<TrackNodeLocation> eb = b.edgeLocation();

        final boolean sameOrder = ea.getFirst().equals(eb.getFirst()) && ea.getSecond().equals(eb.getSecond());
        final boolean flipped = ea.getFirst().equals(eb.getSecond()) && ea.getSecond().equals(eb.getFirst());
        if (!sameOrder && !flipped) return null;

        if (sameOrder) return Math.abs(a.edgePosition() - b.edgePosition());

        final TrackNode n1 = this.graph.locateNode(ea.getFirst());
        final TrackNode n2 = this.graph.locateNode(ea.getSecond());
        if (n1 == null || n2 == null) return null;
        final TrackEdge e = this.edgeBetween(n1, n2);
        if (e == null) return null;

        // b's position is measured from the other end.
        return Math.abs(a.edgePosition() - (e.getLength() - b.edgePosition()));
    }

    /**
     * Dijkstra outward from both ends of an endpoint's edge, bounded by
     * {@link #MAX_RANGE_BLOCKS} and by any better answer already known.
     *
     * <p>Walks {@code getConnectionsFrom} directly — that adjacency map is
     * exactly the set of real rail connections, so junctions branch,
     * crossings are absent and portals cost nothing, all without special
     * casing. Identity-keyed because Create keys its own adjacency the
     * same way; {@code TrackNode} does not override equality.</p>
     */
    private Map<TrackNode, Double> walkFrom(final RailEndpoint origin, final double alreadyKnownBest) {
        final Map<TrackNode, Double> dist = new IdentityHashMap<>();
        final PriorityQueue<Ends> queue = new PriorityQueue<>((x, y) -> Double.compare(x.cost(), y.cost()));

        for (final Ends seed : this.endsOf(origin)) {
            final Double prior = dist.get(seed.node());
            if (prior == null || seed.cost() < prior) {
                dist.put(seed.node(), seed.cost());
                queue.add(seed);
            }
        }

        final double limit = Math.min(MAX_RANGE_BLOCKS, alreadyKnownBest);

        while (!queue.isEmpty()) {
            final Ends current = queue.poll();
            final Double best = dist.get(current.node());
            if (best != null && current.cost() > best) continue;   // stale queue entry
            if (current.cost() > limit) break;                     // nothing further is audible

            final Map<TrackNode, TrackEdge> links = this.graph.getConnectionsFrom(current.node());
            if (links == null) continue;

            for (final Map.Entry<TrackNode, TrackEdge> link : links.entrySet()) {
                final double next = current.cost() + link.getValue().getLength();
                if (next > limit) continue;
                final Double known = dist.get(link.getKey());
                if (known != null && known <= next) continue;
                dist.put(link.getKey(), next);
                queue.add(new Ends(link.getKey(), next));
            }
        }
        return dist;
    }
}
