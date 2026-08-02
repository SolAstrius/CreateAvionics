package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.net.Frame;
import ink.astrius.create_avionics.net.MacAddress;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A rail modem's Lua surface.
 *
 * <p>Diagnostic for now, not the finished article: the eventual block
 * presents as a real CC modem — {@code open}/{@code close}/{@code
 * transmit}, so {@code rednet} works unmodified — but that only makes
 * sense once frames actually carry ComputerCraft packets. What is here
 * instead exposes the physical layer directly, which is what you want
 * while working out whether the medium itself behaves: who else is on
 * this rail network, how far away along the track, and at what signal
 * margin.</p>
 *
 * @cc.module Create_RailModem
 */
public class RailModemPeripheral extends SyncedPeripheral<RailModemBlockEntity> {

    public RailModemPeripheral(final RailModemBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_RailModem";
    }

    /**
     * Check whether this modem has resolved its place on a track graph.
     *
     * <p>False right after placement, and for a short while after a chunk
     * loads: attachment is deliberately lazy, so a modem sits inert until
     * the graph is ready to accept it. Nothing else here returns anything
     * useful until this is true.</p>
     *
     * @return True once attached.
     */
    @LuaFunction(mainThread = true)
    public final boolean isAttached() {
        return this.blockEntity.point() != null;
    }

    /**
     * Get this modem's link-layer address.
     *
     * <p>Derived from the identity Create persists for its point on the
     * graph, so it is stable across a world reload. Formatted like a MAC
     * because that is exactly what it is — six bytes, locally
     * administered.</p>
     *
     * @return The address, e.g. {@code 02:1f:3a:9c:04:b7}, or nil if not
     * attached yet.
     */
    @LuaFunction(mainThread = true)
    public final String getAddress() {
        final MacAddress mac = this.blockEntity.address();
        return mac == null ? null : mac.toString();
    }

    /**
     * Get the id of the medium — the track network — this modem is on.
     *
     * <p>Two modems sharing this are on one broadcast domain and may be
     * able to hear each other; two that do not never can, however close
     * together they look. Joining two separate rail systems with a single
     * piece of track really does merge two broadcast domains.</p>
     *
     * @return The medium id, or nil if not attached.
     */
    @LuaFunction(mainThread = true)
    public final String getMediumId() {
        final RailMedium medium = this.blockEntity.medium();
        return medium == null ? null : medium.id();
    }

    /**
     * List every other modem on this rail network, with how far away it
     * is along the track and how strong the link to it is.
     *
     * <p>Each entry is a table of {@code address}, {@code distance} (in
     * blocks of track, along the shortest rail path — not straight-line
     * distance) and {@code quality} (signal margin in dB). Modems with no
     * rail path at all are omitted, as are those past the attenuation
     * floor.</p>
     *
     * @return A list of peer tables.
     */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getPeers() throws LuaException {
        final RailMedium medium = this.blockEntity.medium();
        final RailModemPoint self = this.blockEntity.point();
        if (medium == null || self == null) throw new LuaException("not attached to a track network");

        final List<Map<String, Object>> out = new ArrayList<>();
        for (final var peer : medium.endpoints()) {
            if (peer == self) continue;
            final double quality = medium.linkQualityDb(self, peer);
            if (!Double.isFinite(quality) || quality < RailMedium.FLOOR_DB) continue;

            final Map<String, Object> entry = new HashMap<>(3);
            entry.put("address", peer.address().toString());
            entry.put("distance", medium.distance(self, peer));
            entry.put("quality", quality);
            out.add(entry);
        }
        return out;
    }

    /**
     * Send a string to another modem, or to all of them.
     *
     * <p>A diagnostic stand-in for the real modem surface: it puts one
     * frame on the medium and lets attenuation decide who hears it.
     * Receivers get a {@code rail_modem_message} event carrying the
     * sender's address, the text, and the margin it arrived with.</p>
     *
     * @param address The destination address, or nil / {@code "*"} to
     *                broadcast.
     * @param message The text to send.
     * @return True if it was transmitted.
     */
    @LuaFunction(mainThread = true)
    public final boolean send(final String address, final String message) throws LuaException {
        final MacAddress self = this.blockEntity.address();
        if (self == null) throw new LuaException("not attached to a track network");

        final MacAddress destination;
        if (address == null || address.isEmpty() || "*".equals(address)) {
            destination = MacAddress.BROADCAST;
        } else {
            destination = parseAddress(address);
        }

        return this.blockEntity.transmit(Frame.local(destination, self,
                Frame.SUB_RAIL_CONTROL, message.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Get the distance a signal of the given margin must have travelled.
     *
     * <p>The inverse of the medium's attenuation model, exposed because
     * it is what makes position-fixing possible: a modem that reports the
     * margin a transmission arrived with has implicitly reported how far
     * away the sender is, and two such reports constrain the sender to
     * one point on the graph.</p>
     *
     * @param quality A margin in dB, as delivered with a message event.
     * @return The implied track distance in blocks.
     */
    @LuaFunction
    public final double distanceForQuality(final double quality) {
        return RailMedium.distanceForQuality(quality);
    }

    // --- Everything else on this rail network ---

    /**
     * List every train signal within range on this rail network.
     *
     * <p>Each entry has {@code id}, {@code distance} and {@code quality}
     * as {@link #getPeers}, plus the signal's aspect on each side of the
     * boundary — {@code forward} and {@code reverse}, one of
     * {@code red}, {@code yellow}, {@code green} or {@code invalid} — and
     * {@code forcedRedForward} / {@code forcedRedReverse} for whether
     * redstone is holding that side at danger.</p>
     *
     * <p>A signal is a boundary between two block sections rather than a
     * single lamp, which is why there are two aspects: a train approaching
     * from one direction and one approaching from the other are being told
     * different things.</p>
     *
     * @return A list of signal tables.
     */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getSignals() throws LuaException {
        return this.survey(EdgePointType.SIGNAL, (signal, entry) -> {
            entry.put("forward", aspect(signal.cachedStates.getFirst()));
            entry.put("reverse", aspect(signal.cachedStates.getSecond()));
            entry.put("forcedRedForward", signal.isForcedRed(true));
            entry.put("forcedRedReverse", signal.isForcedRed(false));
        });
    }

    /**
     * List every train station within range on this rail network.
     *
     * <p>Each entry adds {@code name}, {@code assembling} (whether the
     * station is in assembly mode), and {@code train} — the name of the
     * train currently present, or nil if the platform is empty.</p>
     *
     * @return A list of station tables.
     */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getStations() throws LuaException {
        return this.survey(EdgePointType.STATION, (station, entry) -> {
            entry.put("name", station.name);
            entry.put("assembling", station.assembling);
            final Train present = station.getPresentTrain();
            if (present != null) entry.put("train", present.name.getString());
        });
    }

    /**
     * List every track observer within range on this rail network.
     *
     * <p>Each entry adds {@code activated} — whether a train is passing
     * it right now. The observer's own cargo filter decides which trains
     * count.</p>
     *
     * @return A list of observer tables.
     */
    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getObservers() throws LuaException {
        return this.survey(EdgePointType.OBSERVER,
                (observer, entry) -> entry.put("activated", observer.isActivated()));
    }

    /**
     * Walk one kind of edge point, keeping those within earshot and
     * measuring each along the rail.
     *
     * <p>Range is judged exactly as it is for a modem: same attenuation,
     * same shortest-path-along-track distance. A signal on the far side of
     * a diamond crossing is unreachable for the same reason another modem
     * there would be — the rails never join.</p>
     */
    private <T extends TrackEdgePoint> List<Map<String, Object>> survey(
            final EdgePointType<T> type, final BiConsumer<T, Map<String, Object>> describe) throws LuaException {

        final RailMedium medium = this.blockEntity.medium();
        final RailModemPoint self = this.blockEntity.point();
        final TrackGraph graph = this.blockEntity.graph();
        if (medium == null || self == null || graph == null) {
            throw new LuaException("not attached to a track network");
        }

        final List<Map<String, Object>> out = new ArrayList<>();
        for (final T point : graph.getPoints(type)) {
            final TrackFixture fixture = new TrackFixture(point);
            final double quality = medium.linkQualityDb(self, fixture);
            if (!Double.isFinite(quality) || quality < RailMedium.FLOOR_DB) continue;

            final Map<String, Object> entry = new HashMap<>();
            entry.put("id", point.getId().toString());
            entry.put("distance", medium.distance(self, fixture));
            entry.put("quality", quality);
            describe.accept(point, entry);
            out.add(entry);
        }
        return out;
    }

    private static String aspect(final SignalBlockEntity.SignalState state) {
        return state == null ? "invalid" : state.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Raise a {@code rail_modem_message} event on every attached computer.
     *
     * <p>Called by the block entity when a frame addressed to this modem
     * arrives. The margin travels with the message on purpose: a receiver
     * that knows how strongly something arrived knows how far away it came
     * from, which is what makes locating a transmitter from two or more
     * listeners possible at all.</p>
     *
     * @param source    The sender's address.
     * @param message   The text it sent.
     * @param qualityDb The margin it arrived with, in dB.
     */
    public void onMessage(final String source, final String message, final double qualityDb) {
        this.queueEvent("rail_modem_message", source, message, qualityDb);
    }

    private static MacAddress parseAddress(final String text) throws LuaException {
        final String[] parts = text.split(":");
        if (parts.length != MacAddress.LENGTH) {
            throw new LuaException("expected an address like 02:1f:3a:9c:04:b7, got '" + text + "'");
        }
        final byte[] raw = new byte[MacAddress.LENGTH];
        try {
            for (int i = 0; i < parts.length; i++) {
                raw[i] = (byte) Integer.parseInt(parts[i], 16);
            }
        } catch (final NumberFormatException e) {
            throw new LuaException("address is not hexadecimal: '" + text + "'");
        }
        return MacAddress.of(raw);
    }
}
