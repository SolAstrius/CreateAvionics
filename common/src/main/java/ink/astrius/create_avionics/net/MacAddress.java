package ink.astrius.create_avionics.net;

import java.util.Arrays;
import java.util.UUID;

/**
 * A 48-bit link-layer address, in the same shape and with the same
 * conventions as an Ethernet MAC.
 *
 * <p>Deliberately 48 bits rather than something roomier like a raw
 * {@link UUID}: an endpoint on this stack has to be able to <em>be</em> a
 * real NIC. A Scalar Evolution machine bridging its emulated Ethernet
 * device onto a {@link Medium} hands over genuine Ethernet frames whose
 * address fields are six bytes wide, and a guest Linux kernel doing real
 * ARP over them will not tolerate anything else. Sizing this to the
 * narrowest consumer up front costs nothing and avoids a translation
 * layer that could never be lossless.</p>
 *
 * <h2>Synthesised addresses</h2>
 * <p>Addresses minted by {@link #ofStableId} set the
 * <em>locally administered</em> bit and clear the <em>group</em> bit of
 * the first octet, which is the standard way to say "this address was
 * made up locally, it does not belong to any registered vendor". Squatting
 * on a real OUI would risk colliding with actual hardware on a bridged
 * network, and is exactly what QEMU, Docker and every other virtual NIC
 * avoid the same way.</p>
 *
 * <pre>
 *   first octet:  x x x x x x U I
 *                             │ └── I/G  0 = unicast, 1 = group/multicast
 *                             └──── U/L  0 = globally unique (has an OUI)
 *                                        1 = locally administered
 * </pre>
 */
public final class MacAddress {

    /** Address length in bytes, as on the wire. */
    public static final int LENGTH = 6;

    /** The all-ones address; every endpoint on a medium accepts it. */
    public static final MacAddress BROADCAST =
            new MacAddress(new byte[]{-1, -1, -1, -1, -1, -1});

    private final byte[] bytes;

    private MacAddress(final byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Wrap six raw bytes, e.g. the address field lifted straight out of a
     * received Ethernet frame.
     *
     * @param bytes Exactly {@link #LENGTH} bytes; copied, not retained.
     * @return The address.
     */
    public static MacAddress of(final byte[] bytes) {
        if (bytes.length != LENGTH) {
            throw new IllegalArgumentException("a MAC address is " + LENGTH + " bytes, got " + bytes.length);
        }
        return new MacAddress(bytes.clone());
    }

    /**
     * Derive a stable synthetic address from a persistent identity.
     *
     * <p>The same id always yields the same address, so an endpoint keeps
     * its identity across a world reload without having to serialise the
     * address separately — feed it whatever id the endpoint already
     * persists. Callers are responsible for that id actually being stable;
     * a freshly random one per session would produce an address that
     * changes out from under any peer that had learned it.</p>
     *
     * @param id The endpoint's persistent identity.
     * @return A locally administered unicast address derived from it.
     */
    public static MacAddress ofStableId(final UUID id) {
        // Fold all 128 bits down so both halves influence the result; using
        // only the high half would collide across UUIDs that share it.
        long mixed = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);

        final byte[] out = new byte[LENGTH];
        for (int i = LENGTH - 1; i >= 0; i--) {
            out[i] = (byte) mixed;
            mixed >>>= 8;
        }

        // Clear both flag bits, then set U/L: locally administered, unicast.
        out[0] = (byte) ((out[0] & 0xFC) | 0x02);
        return new MacAddress(out);
    }

    /** @return This address as six raw bytes, ready to write to a frame. */
    public byte[] bytes() {
        return this.bytes.clone();
    }

    /** @return Whether this is the broadcast address. */
    public boolean isBroadcast() {
        return this.equals(BROADCAST);
    }

    /**
     * @return Whether the group bit is set — a multicast or broadcast
     * address rather than a single endpoint.
     */
    public boolean isGroup() {
        return (this.bytes[0] & 0x01) != 0;
    }

    /**
     * @return Whether the locally-administered bit is set, i.e. this
     * address was synthesised rather than assigned from a vendor's OUI.
     */
    public boolean isLocallyAdministered() {
        return (this.bytes[0] & 0x02) != 0;
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof final MacAddress other && Arrays.equals(this.bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    /** @return The canonical colon-separated lower-case form, e.g. {@code 02:1f:3a:9c:04:b7}. */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(LENGTH * 3 - 1);
        for (int i = 0; i < LENGTH; i++) {
            if (i > 0) sb.append(':');
            sb.append(Character.forDigit((this.bytes[i] >> 4) & 0xF, 16));
            sb.append(Character.forDigit(this.bytes[i] & 0xF, 16));
        }
        return sb.toString();
    }
}
