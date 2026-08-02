package ink.astrius.create_avionics.net;

import java.util.Set;

/**
 * A shared physical medium — layer 1, and nothing above it.
 *
 * <p>Models one broadcast domain in the sense a length of 10BASE5 coax
 * was one: every endpoint attached to it hears every transmission, and
 * whether a given endpoint <em>successfully</em> hears any particular one
 * is decided purely by attenuation over the distance between them. A rail
 * network is a remarkably good fit for that shape — the track is the
 * cable, and the graph's own topology decides who is electrically near
 * whom.</p>
 *
 * <h2>What this interface deliberately does not do</h2>
 * <ul>
 *   <li><b>No addressing.</b> {@link #transmit} takes raw bytes. Source
 *       and destination addresses live inside the frame, which is layer
 *       2's business — the medium carries bits.</li>
 *   <li><b>No routing.</b> There is no next-hop selection here, ever.
 *       Consumers that need real layer 3 already have it: a Scalar
 *       Evolution guest runs a real Linux kernel with a real IP stack,
 *       and re-implementing that above this interface would be strictly
 *       worse than the thing it duplicates.</li>
 *   <li><b>No repeating.</b> A repeater is a <em>device on</em> a medium
 *       that re-transmits what it hears, not a property of the medium
 *       itself. An endpoint may choose to do that; the cable does not do
 *       it for them.</li>
 * </ul>
 *
 * <p>Keeping the contract this narrow is what lets unrelated stacks share
 * one physical layer as peers: an encapsulated ComputerCraft packet, a
 * rail-control protocol, and (later) real Ethernet frames from a bridged
 * NIC can all ride the same medium, demultiplexed by the protocol field
 * that layer 2 writes into the frame.</p>
 *
 * <p>Implementations carry no Minecraft types in their signatures on
 * purpose, so the whole layer is exercisable from plain unit tests.</p>
 */
public interface Medium {

    /**
     * Stable identifier for this medium — two endpoints sharing one are
     * on the same broadcast domain, and two that do not can never reach
     * each other no matter how physically close they look.
     *
     * <p>For a rail medium this tracks the underlying track network's own
     * identity: two disconnected rail systems are two media, and joining
     * them with a single piece of track really does merge two broadcast
     * domains into one.</p>
     *
     * @return The identifier.
     */
    String id();

    /**
     * Every endpoint currently attached to this medium, in range or not.
     *
     * @return The attached endpoints.
     */
    Set<Endpoint> endpoints();

    /**
     * Link quality from one endpoint to another, in decibels of margin
     * above the medium's own floor.
     *
     * <p>Positive means deliverable, and larger means more headroom.
     * {@link Double#NEGATIVE_INFINITY} means there is no physical path at
     * all — on a rail medium that is the honest answer for two modems
     * separated only by a diamond crossing, which is a place where two
     * routes physically intersect without their rails ever connecting.</p>
     *
     * <p>Expected to be symmetric, and callers may rely on that.</p>
     *
     * @param from The transmitting endpoint.
     * @param to   The receiving endpoint.
     * @return Margin in dB, or {@link Double#NEGATIVE_INFINITY} if unreachable.
     */
    double linkQualityDb(Endpoint from, Endpoint to);

    /**
     * The medium's native distance between two endpoints — for a rail
     * medium, metres of track along the shortest path, which is emphatically
     * not the straight-line distance between them.
     *
     * <p>Exposed alongside {@link #linkQualityDb} because the two answer
     * different questions: quality decides delivery, distance is what a
     * consumer needs to invert a received signal strength back into a
     * position estimate.</p>
     *
     * @param from One endpoint.
     * @param to   The other.
     * @return Distance in the medium's own units, or
     * {@link Double#POSITIVE_INFINITY} if there is no path.
     */
    double distance(Endpoint from, Endpoint to);

    /**
     * Transmit a frame onto the medium.
     *
     * <p>Delivers to every attached endpoint whose
     * {@link #linkQualityDb} from the sender clears the medium's floor,
     * excluding the sender itself. Frames that reach nobody are simply
     * lost, exactly as they would be on real wire — there is no
     * acknowledgement, no retry and no error at this layer.</p>
     *
     * @param from  The transmitting endpoint; must be attached.
     * @param frame The raw frame. Not inspected, not modified.
     */
    void transmit(Endpoint from, byte[] frame);
}
