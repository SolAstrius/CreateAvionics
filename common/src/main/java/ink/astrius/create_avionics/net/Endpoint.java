package ink.astrius.create_avionics.net;

/**
 * One station attached to a {@link Medium} — the thing that can transmit
 * onto it and be delivered to from it.
 *
 * <p>Deliberately says nothing about <em>what</em> the station is. A rail
 * modem block, a machine's bridged Ethernet device, a test double in a
 * unit test and a future store-and-forward relay riding a train are all
 * endpoints; the medium only needs an address to deliver to and somewhere
 * to hand the bytes.</p>
 *
 * <p>The payload crossing this boundary is an opaque frame, not a parsed
 * one. Whatever framing rides here — {@link Frame}'s own layout, or a
 * genuine Ethernet frame lifted out of an emulated NIC — is a layer-2
 * concern; a physical medium carries bits and has no business reading
 * addresses out of them. See {@link Medium} for why that separation is
 * worth keeping strict.</p>
 */
public interface Endpoint {

    /**
     * This endpoint's link-layer address. Must be stable for as long as
     * the endpoint exists, and ideally across reloads — peers cache it.
     *
     * @return The address.
     */
    MacAddress address();

    /**
     * Accept a frame that reached this endpoint through the medium.
     *
     * <p>Called once per delivered frame, only for endpoints the medium
     * decided were actually in range. Implementations should treat this
     * as arriving on the server thread and keep it cheap.</p>
     *
     * @param frame     The raw frame, exactly as transmitted.
     * @param qualityDb Received quality in decibels — how much margin was
     *                  left after attenuation. Higher is better; the
     *                  medium only delivers above its own floor, so this
     *                  is always a value the endpoint could act on.
     *                  Usable for range estimation: invert the medium's
     *                  rolloff to recover distance.
     */
    void deliver(byte[] frame, double qualityDb);
}
