package ink.astrius.create_avionics.content.radio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The parts of the radio that don't need a world: identities, ranges, the track table. */
class RadioTest {

    private static ShipReport report(final String callsign) {
        return new ShipReport("ABC123", callsign, "1200", 0, 100, 0, 0, 0, 0);
    }

    @Test
    void addressIsSixHexDigitsAndStable() {
        final java.util.UUID ship = java.util.UUID.fromString("7df82d1c-4b2e-4a77-9c11-0d3e5f6a7b8c");
        final String a = Identity.address(ship);
        org.junit.jupiter.api.Assertions.assertTrue(a.matches("[0-9A-F]{6}"), a);
        assertEquals(a, Identity.address(java.util.UUID.fromString(ship.toString())));
        assertEquals(a, Identity.defaultCallsign(ship, null));
        assertEquals("Heron", Identity.defaultCallsign(ship, "Heron"));
    }

    @Test
    void callsignsAreTrimmedAndChecked() {
        assertEquals("ALPHA-1", Identity.callsign("  ALPHA-1 "));
        assertThrows(IllegalArgumentException.class, () -> Identity.callsign(""));
        assertThrows(IllegalArgumentException.class, () -> Identity.callsign("THIRTEEN-CHAR"));
        assertThrows(IllegalArgumentException.class, () -> Identity.callsign("A B"));
    }

    @Test
    void squawksAreFourOctalDigits() {
        assertEquals("7700", Identity.squawk("7700"));
        assertEquals("7700", Identity.squawk(7700.0));
        assertEquals("0012", Identity.squawk(12.0));
        assertThrows(IllegalArgumentException.class, () -> Identity.squawk("1280"));
        assertThrows(IllegalArgumentException.class, () -> Identity.squawk("120"));
        assertThrows(IllegalArgumentException.class, () -> Identity.squawk(1200.5));
        assertThrows(IllegalArgumentException.class, () -> Identity.squawk(true));
    }

    @Test
    void rangesMatchTheTuning() {
        final SimpleSignalModel m = new SimpleSignalModel();
        assertEquals(3162, m.broadcastRange(30), 1);
        assertEquals(100, m.broadcastRange(0), 1);
        assertEquals(251, m.echoRange(100), 1);
    }

    @Test
    void fullTableGivesUpTheStalestThenTheWeakest() {
        final List<String> events = new ArrayList<>();
        final Receiver r = new Receiver((event, report) -> events.add(event + " " + report.callsign()));
        final UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        r.hear(List.of(new Receiver.Heard(a, report("A"), -50)), 0, 2, 8);
        r.hear(List.of(new Receiver.Heard(b, report("B"), -60)), 10, 2, 8);
        // Full: C replaces A, the stalest
        r.hear(List.of(new Receiver.Heard(c, report("C"), -90)), 20, 2, 8);
        assertEquals(List.of("traffic_new A", "traffic_new B", "traffic_lost A", "traffic_new C"), events);
        // B and C heard together: D is weaker than both, so it doesn't push either out
        events.clear();
        r.hear(List.of(new Receiver.Heard(b, report("B"), -60), new Receiver.Heard(c, report("C"), -90),
                new Receiver.Heard(d, report("D"), -95)), 30, 2, 8);
        assertEquals(List.of(), events);
        assertEquals(List.of("B", "C"), r.tracks().stream().map(t -> t.report().callsign()).toList());
        assertEquals(6, r.sequence());
        assertEquals(3, r.since(3).size());
    }

    @Test
    void tracksExpireAndTheBufferIsBounded() {
        final List<String> events = new ArrayList<>();
        final Receiver r = new Receiver((event, report) -> events.add(event + " " + report.callsign()));
        final UUID a = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            r.hear(List.of(new Receiver.Heard(a, report("A"), -50)), i * 10L, 64, 4);
        }
        assertEquals(4, r.since(0).size());
        assertEquals(7, r.since(0).get(0).sequence());
        r.expire(90 + 600, 600, 64);
        assertEquals(1, r.tracks().size());
        r.expire(90 + 601, 600, 64);
        assertTrue(r.tracks().isEmpty());
        assertEquals(List.of("traffic_new A", "traffic_lost A"), events);
    }
}
