package ink.astrius.create_avionics.net;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wire layout is a compatibility contract, not an internal detail: a
 * bridged guest NIC emits real Ethernet frames, so anything that parses or
 * builds here has to agree with the real thing byte for byte.
 */
class FrameTest {

    private static final MacAddress A = MacAddress.ofStableId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MacAddress B = MacAddress.ofStableId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Test
    void headerIsFourteenBytesInEthernetOrder() {
        assertEquals(14, Frame.HEADER_LENGTH);

        final byte[] wire = new Frame(B, A, 0x0800, new byte[]{9, 9}).toBytes();
        assertEquals(16, wire.length);
        assertArrayEquals(B.bytes(), java.util.Arrays.copyOfRange(wire, 0, 6), "destination comes first");
        assertArrayEquals(A.bytes(), java.util.Arrays.copyOfRange(wire, 6, 12), "then source");
        assertEquals((byte) 0x08, wire[12], "then ethertype, big-endian");
        assertEquals((byte) 0x00, wire[13]);
    }

    @Test
    void roundTripsThroughTheWire() {
        final Frame original = Frame.local(B, A, Frame.SUB_RAIL_CONTROL, "signal-state".getBytes());
        assertEquals(original, Frame.parse(original.toBytes()));
    }

    @Test
    void localFramesUseTheIeeeExperimentalEtherType() {
        // 0x88B5 is reserved by IEEE for local use; picking a registered
        // value would collide the moment real hardware shares the medium.
        assertEquals(0x88B5, Frame.ETHERTYPE_LOCAL);
        assertTrue(Frame.local(B, A, Frame.SUB_COMPUTERCRAFT, new byte[0]).isLocal());
    }

    @Test
    void ourTrafficIsDistinguishableFromAGuestKernelsTraffic() {
        final Frame ours = Frame.local(B, A, Frame.SUB_COMPUTERCRAFT, new byte[]{1});
        final Frame guestIp = new Frame(B, A, 0x0800, new byte[]{1});   // real IPv4
        final Frame guestArp = new Frame(B, A, 0x0806, new byte[]{1});  // real ARP

        assertTrue(ours.isLocal());
        assertFalse(guestIp.isLocal(), "a guest's IP frame must never be mistaken for ours");
        assertFalse(guestArp.isLocal());
        assertEquals(-1, guestIp.subProtocol(), "no sub-protocol on foreign traffic");
    }

    @Test
    void subProtocolSeparatesOurOwnUsers() {
        final byte[] body = {4, 5, 6};
        final Frame cc = Frame.local(B, A, Frame.SUB_COMPUTERCRAFT, body);
        final Frame rail = Frame.local(B, A, Frame.SUB_RAIL_CONTROL, body);

        assertEquals(Frame.SUB_COMPUTERCRAFT, cc.subProtocol());
        assertEquals(Frame.SUB_RAIL_CONTROL, rail.subProtocol());
        assertArrayEquals(body, cc.body(), "the sub-protocol byte is stripped from the body");
        assertArrayEquals(body, rail.body());
    }

    @Test
    void payloadSurvivesAnEmptyBody() {
        final Frame f = Frame.local(B, A, Frame.SUB_RAIL_CONTROL, new byte[0]);
        assertEquals(Frame.SUB_RAIL_CONTROL, f.subProtocol());
        assertArrayEquals(new byte[0], f.body());
        assertEquals(f, Frame.parse(f.toBytes()));
    }

    @Test
    void broadcastIsAcceptedByEveryone() {
        final Frame f = Frame.local(MacAddress.BROADCAST, A, Frame.SUB_RAIL_CONTROL, new byte[0]);
        assertTrue(f.addressedTo(A));
        assertTrue(f.addressedTo(B));
    }

    @Test
    void unicastIsAcceptedOnlyByItsTarget() {
        final Frame f = Frame.local(B, A, Frame.SUB_RAIL_CONTROL, new byte[0]);
        assertTrue(f.addressedTo(B));
        assertFalse(f.addressedTo(A));
    }

    @Test
    void malformedInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Frame.parse(new byte[13]),
                "shorter than a header");
        assertThrows(IllegalArgumentException.class,
                () -> new Frame(B, A, 0x10000, new byte[0]), "ethertype is 16 bits");
        assertThrows(IllegalArgumentException.class,
                () -> Frame.local(B, A, 0x100, new byte[0]), "sub-protocol is 8 bits");
    }

    @Test
    void aHeaderOnlyFrameParses() {
        final Frame f = new Frame(B, A, 0x0800, new byte[0]);
        final Frame parsed = Frame.parse(f.toBytes());
        assertEquals(0, parsed.payload().length);
        assertEquals(f, parsed);
    }
}
