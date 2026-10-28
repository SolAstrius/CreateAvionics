package ink.astrius.create_avionics.content.radio;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Every station in one level. Once per broadcast interval each transmitting ship's report goes to
 * every station that hears it above the noise floor: transmitters × receivers, with receivers
 * beyond the farthest a broadcast could carry skipped before any signal is worked out.
 * <p>
 * There is no tick hook in {@code common}, so the stations' own ticks drive it; the first to tick
 * at an interval runs it for everyone.
 */
public final class Airwaves {

    private static final Map<Level, Airwaves> LEVELS = new WeakHashMap<>();

    private final Set<Station> stations = new LinkedHashSet<>();
    private long lastRun = Long.MIN_VALUE;

    private Airwaves() {
    }

    public static synchronized Airwaves of(final Level level) {
        return LEVELS.computeIfAbsent(level, l -> new Airwaves());
    }

    public synchronized void join(final Station station) {
        this.stations.add(station);
    }

    public synchronized void leave(final Station station) {
        this.stations.remove(station);
    }

    /** Called from every station's server tick; does the work once per interval. */
    public void tick(final Level level) {
        final long now = level.getGameTime();
        final List<Station> live;
        synchronized (this) {
            if (now == this.lastRun || now % Math.max(1, RadioConfig.broadcastInterval) != 0) {
                return;
            }
            this.lastRun = now;
            this.stations.removeIf(Station::isRemoved);
            live = List.copyOf(this.stations);
        }
        final SignalModel model = SignalModel.current;
        final double noise = model.noiseFloor();
        final List<Station.Broadcast> sent = new ArrayList<>();
        final List<Double> reach = new ArrayList<>();
        for (final Station s : live) {
            final Station.Broadcast b = s.broadcast();
            if (b != null) {
                sent.add(b);
                final double range = model.broadcastRange(b.powerDbm());
                reach.add(range * range);
            }
        }
        final int maxTracks = RadioConfig.maxTracks;
        final int buffer = RadioConfig.messageBufferSize;
        final long expiry = RadioConfig.trackExpirySeconds * 20L;
        for (final Station s : live) {
            final List<Receiver.Heard> heard = new ArrayList<>();
            if (!sent.isEmpty()) {
                final Vec3 at = s.antenna();
                for (int i = 0; i < sent.size(); i++) {
                    final Station.Broadcast b = sent.get(i);
                    if (b.ship().equals(s.ship()) || at.distanceToSqr(b.from()) > reach.get(i)) {
                        continue;
                    }
                    final double signal = model.broadcast(level, b.from(), at, b.powerDbm());
                    if (signal >= noise) {
                        heard.add(new Receiver.Heard(b.ship(), b.report(), signal));
                    }
                }
            }
            if (!heard.isEmpty()) {
                s.receiver().hear(heard, now, maxTracks, buffer);
            }
            s.receiver().expire(now, expiry, maxTracks);
        }
    }

    /**
     * A ship's report as measured at its antenna: world position, the bow's compass heading, and
     * velocity from how far the antenna moved since the last physics pose.
     */
    public static ShipReport measure(final SubLevel ship, final BlockPos antenna, final Direction bow,
                                     final String callsign, final String squawk) {
        final Vec3 local = Vec3.atCenterOf(antenna);
        final Vec3 now = ship.logicalPose().transformPosition(local);
        final Vec3 before = ship.lastPose().transformPosition(local);
        final Vec3 velocity = now.subtract(before).scale(20);
        final Vec3 nose = ship.logicalPose().transformNormal(new Vec3(bow.getStepX(), 0, bow.getStepZ()));
        // Compass bearing, north -z, east +x; rounded so due north reads 0, not 359.99..
        final double heading = (Math.round(Math.toDegrees(Math.atan2(nose.x, -nose.z)) * 1e6) / 1e6 + 360) % 360;
        return new ShipReport(Identity.address(ship.getUniqueId()), callsign, squawk, now.x, now.y, now.z, heading,
                Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z), velocity.y);
    }
}
