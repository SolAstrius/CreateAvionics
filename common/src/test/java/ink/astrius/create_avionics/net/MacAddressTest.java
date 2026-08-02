package ink.astrius.create_avionics.net;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The flag bits and the stability guarantee are the two things a peer
 * actually depends on: a bridged guest kernel will reject an address whose
 * group bit is set where a unicast one belongs, and any peer that caches an
 * address expects it to survive a reload.
 */
class MacAddressTest {

    @Test
    void synthesisedAddressesAreLocallyAdministeredUnicast() {
        final MacAddress mac = MacAddress.ofStableId(UUID.randomUUID());
        assertTrue(mac.isLocallyAdministered(), "must not squat on a real vendor OUI");
        assertFalse(mac.isGroup(), "a station address is unicast");
    }

    @Test
    void derivationIsStableForTheSameId() {
        final UUID id = UUID.fromString("6c5b2f1e-9a3d-4e77-b1c8-0f2a5d7e9b34");
        assertEquals(MacAddress.ofStableId(id), MacAddress.ofStableId(id),
                "a reload must not change an endpoint's address");
    }

    @Test
    void differentIdsGiveDifferentAddresses() {
        assertNotEquals(
                MacAddress.ofStableId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                MacAddress.ofStableId(UUID.fromString("00000000-0000-0000-0000-000000000002")));
    }

    @Test
    void bothHalvesOfTheIdInfluenceTheAddress() {
        // A fold that only used the high bits would collapse these together.
        final MacAddress a = MacAddress.ofStableId(new UUID(0x0123456789ABCDEFL, 1L));
        final MacAddress b = MacAddress.ofStableId(new UUID(0x0123456789ABCDEFL, 2L));
        assertNotEquals(a, b, "the low half of the id must not be ignored");
    }

    @Test
    void derivationSpreadsAcrossAWideIdSpace() {
        final Set<MacAddress> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(MacAddress.ofStableId(new UUID(i, ~i)));
        }
        assertEquals(2000, seen.size(), "no collisions expected at this scale");
    }

    @Test
    void broadcastIsBroadcastAndIsAGroupAddress() {
        assertTrue(MacAddress.BROADCAST.isBroadcast());
        assertTrue(MacAddress.BROADCAST.isGroup());
        assertFalse(MacAddress.ofStableId(UUID.randomUUID()).isBroadcast());
    }

    @Test
    void roundTripsThroughRawBytes() {
        final MacAddress mac = MacAddress.ofStableId(UUID.randomUUID());
        assertEquals(mac, MacAddress.of(mac.bytes()));
    }

    @Test
    void rawBytesAreCopiedNotAliased() {
        final byte[] raw = {0x02, 0x11, 0x22, 0x33, 0x44, 0x55};
        final MacAddress mac = MacAddress.of(raw);
        raw[0] = 0x7F;
        assertEquals(0x02, mac.bytes()[0], "constructing must not retain the caller's array");

        final byte[] out = mac.bytes();
        out[0] = 0x7F;
        assertEquals(0x02, mac.bytes()[0], "accessor must not expose internal state");
    }

    @Test
    void wrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MacAddress.of(new byte[5]));
        assertThrows(IllegalArgumentException.class, () -> MacAddress.of(new byte[7]));
    }

    @Test
    void printsInCanonicalColonSeparatedForm() {
        assertEquals("02:11:22:33:44:55",
                MacAddress.of(new byte[]{0x02, 0x11, 0x22, 0x33, 0x44, 0x55}).toString());
        assertEquals("ff:ff:ff:ff:ff:ff", MacAddress.BROADCAST.toString());
    }
}
