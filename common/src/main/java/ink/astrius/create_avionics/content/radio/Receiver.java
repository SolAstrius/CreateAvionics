package ink.astrius.create_avionics.content.radio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One controller's radio receiver: a table of the ships it hears, newest report each, and the
 * last raw messages. Written on the server thread, read from computer threads, hence the locks.
 */
public final class Receiver {

    /** Where {@code traffic_new} and {@code traffic_lost} go: the controller's attached computers. */
    @FunctionalInterface
    public interface Events {
        void queue(String event, String callsign);
    }

    /** A message as received: whom it came from, what it said, and how strong it was. */
    public record Heard(UUID ship, ShipReport report, double signal) {
    }

    /** A ship being heard. {@code heard} is the game time of its latest message. */
    public record Track(UUID ship, ShipReport report, double signal, long heard) {
    }

    public record Message(long sequence, ShipReport report, double signal, long heard) {
    }

    /** Stalest first, then weakest: the order tracks are given up in when the table is full. */
    private static final Comparator<Track> GIVE_UP = Comparator.comparingLong(Track::heard)
            .thenComparingDouble(Track::signal);

    private final Events events;
    private final Map<UUID, Track> tracks = new HashMap<>();
    private final ArrayDeque<Message> messages = new ArrayDeque<>();
    private long sequence;

    public Receiver(final Events events) {
        this.events = events;
    }

    /**
     * Take in one interval's messages, all heard at {@code now}. They are handled strongest first,
     * so when the table is full a weak newcomer can't push out a stronger ship heard just now.
     */
    public synchronized void hear(final List<Heard> heard, final long now, final int maxTracks, final int bufferSize) {
        final List<Heard> sorted = new ArrayList<>(heard);
        sorted.sort(Comparator.comparingDouble(Heard::signal).reversed());
        for (final Heard h : sorted) {
            this.messages.addLast(new Message(++this.sequence, h.report(), h.signal(), now));
            final Track track = new Track(h.ship(), h.report(), h.signal(), now);
            if (this.tracks.containsKey(h.ship())) {
                this.tracks.put(h.ship(), track);
                continue;
            }
            if (this.tracks.size() >= Math.max(1, maxTracks)) {
                final Track victim = this.tracks.values().stream().min(GIVE_UP).orElseThrow();
                if (victim.heard() >= now) {
                    continue; // everything kept was heard this interval, and more strongly
                }
                this.lose(victim);
            }
            this.tracks.put(h.ship(), track);
            this.events.queue("traffic_new", h.report().callsign());
        }
        while (this.messages.size() > Math.max(0, bufferSize)) {
            this.messages.removeFirst();
        }
    }

    /** Drop tracks not heard for {@code expiryTicks}, and any over a lowered cap. */
    public synchronized void expire(final long now, final long expiryTicks, final int maxTracks) {
        for (final Track t : List.copyOf(this.tracks.values())) {
            if (now - t.heard() > expiryTicks) {
                this.lose(t);
            }
        }
        while (this.tracks.size() > Math.max(1, maxTracks)) {
            this.lose(this.tracks.values().stream().min(GIVE_UP).orElseThrow());
        }
    }

    private void lose(final Track track) {
        this.tracks.remove(track.ship());
        this.events.queue("traffic_lost", track.report().callsign());
    }

    /** Every track, strongest first. */
    public synchronized List<Track> tracks() {
        final List<Track> out = new ArrayList<>(this.tracks.values());
        out.sort(Comparator.comparingDouble(Track::signal).reversed());
        return out;
    }

    /** The sequence number of the newest message, 0 before the first. */
    public synchronized long sequence() {
        return this.sequence;
    }

    /** Buffered messages newer than {@code since}, oldest first. */
    public synchronized List<Message> since(final long since) {
        final List<Message> out = new ArrayList<>();
        for (final Message m : this.messages) {
            if (m.sequence() > since) {
                out.add(m);
            }
        }
        return out;
    }
}
