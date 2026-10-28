package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.NotAttachedException;
import ink.astrius.create_avionics.content.radio.Identity;
import ink.astrius.create_avionics.content.radio.Receiver;
import ink.astrius.create_avionics.content.radio.ShipReport;
import ink.astrius.create_avionics.content.radio.SignalModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The avionics controller's radio, which works like ADS-B. While the transponder is on, the ship
 * broadcasts its callsign, squawk code, position, heading and speeds a couple of times a second;
 * every controller in range hears it, on a ship or on the ground. Callsign, squawk, power and
 * the transponder switch belong to the ship: setting them on any controller sets every
 * controller on that ship.
 * <p>
 * The receiver is always on. It keeps a track per ship it hears, and queues two events on every
 * computer attached to it:
 * <ul>
 *   <li>{@code traffic_new}: args: the peripheral's name, the new track's callsign and address.</li>
 *   <li>{@code traffic_lost}: args: the peripheral's name, the lost track's callsign and address. A track is
 *   lost when nothing has been heard from that ship for a while, or when the table is full and
 *   it is the stalest (then weakest) track.</li>
 * </ul>
 *
 * @cc.module avionics_controller
 */
public class AvionicsControllerPeripheral implements IPeripheral {

    private final AvionicsControllerBlockEntity blockEntity;
    private final Set<IComputerAccess> computers = ConcurrentHashMap.newKeySet();

    public AvionicsControllerPeripheral(final AvionicsControllerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "avionics_controller";
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other instanceof final AvionicsControllerPeripheral o && o.blockEntity == this.blockEntity;
    }

    @Override
    public void attach(final IComputerAccess computer) {
        this.computers.add(computer);
    }

    @Override
    public void detach(final IComputerAccess computer) {
        this.computers.remove(computer);
    }

    /** Queue a traffic event on every attached computer, under the name it knows this by. */
    void queue(final String event, final ShipReport report) {
        for (final IComputerAccess computer : this.computers) {
            try {
                computer.queueEvent(event, computer.getAttachmentName(), report.callsign(), report.address());
            } catch (final NotAttachedException e) {
                this.computers.remove(computer);
            }
        }
    }

    /**
     * Check whether the ship's transponder is on.
     *
     * @return True if the ship is broadcasting.
     */
    @LuaFunction
    public final boolean isTransponderOn() {
        return this.blockEntity.transponderOn();
    }

    /**
     * Switch the ship's transponder on or off. The receiver keeps listening either way.
     *
     * @param on True to broadcast, false for radio silence.
     */
    @LuaFunction(mainThread = true)
    public final void setTransponder(final boolean on) {
        this.blockEntity.setShipTransponder(on);
    }

    /**
     * Get the callsign the ship broadcasts.
     *
     * @return The callsign set with {@link #setCallsign}; until one is set, the ship's name, or its
     * address when it has none. Nil for a controller on the ground with no callsign set.
     */
    @LuaFunction
    public final @Nullable String getCallsign() {
        return this.blockEntity.callsign();
    }

    /**
     * Get the ship's address: six hex digits, like an aircraft's ICAO address. It is derived from
     * the ship itself and never changes, so it tells apart ships that share a callsign.
     *
     * @return The address, or nil for a controller on the ground.
     */
    @LuaFunction
    public final @Nullable String getAddress() {
        return this.blockEntity.address();
    }

    /**
     * Set the ship's callsign.
     *
     * @param callsign 1 to 12 letters, digits or {@code -}; surrounding spaces are trimmed.
     * @throws LuaException If the callsign is empty, too long or has other characters.
     */
    @LuaFunction(mainThread = true)
    public final void setCallsign(final String callsign) throws LuaException {
        try {
            this.blockEntity.setShipCallsign(Identity.callsign(callsign));
        } catch (final IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Get the ship's squawk code.
     *
     * @return Four octal digits as a string, {@code "1200"} until set.
     */
    @LuaFunction
    public final String getSquawk() {
        return this.blockEntity.squawk();
    }

    /**
     * Set the ship's squawk code.
     *
     * @param code Four digits 0 to 7, as a string ({@code "0012"}) or a number ({@code 12}, padded
     * to {@code "0012"}).
     * @throws LuaException If the code isn't four octal digits.
     */
    @LuaFunction(mainThread = true)
    public final void setSquawk(final Object code) throws LuaException {
        try {
            this.blockEntity.setShipSquawk(Identity.squawk(code));
        } catch (final IllegalArgumentException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Get the transponder's transmit power.
     *
     * @return The power in dBm.
     */
    @LuaFunction
    public final double getTransponderPower() {
        return this.blockEntity.power();
    }

    /**
     * Set the transponder's transmit power, which sets how far the ship is heard. At the default
     * 30 dBm it carries about 3000 blocks in clear weather; every 20 dB less divides that by ten.
     *
     * @param dBm The power in dBm, clamped to 0..40.
     * @throws LuaException If the power isn't a finite number.
     */
    @LuaFunction(mainThread = true)
    public final void setTransponderPower(final double dBm) throws LuaException {
        if (!Double.isFinite(dBm)) {
            throw new LuaException("power must be a finite number");
        }
        this.blockEntity.setShipPower(dBm);
    }

    /**
     * Get the ships this receiver hears.
     *
     * @return A list of tracks, strongest first, each a table with {@code address}, {@code callsign},
     * {@code squawk}, {@code signal} in dBm and {@code age} (seconds since the ship was last heard),
     * plus what the sending ship can measure: {@code altitude} in blocks (with an altitude sensor;
     * with only a barometer, to the nearest 10), {@code verticalRate} in blocks per second (altitude
     * sensor), {@code heading} in degrees, 0 north, clockwise (a navigation table holding a compass)
     * and {@code speed} over the ground in blocks per second (a velocity sensor on the ship's forward
     * axis). Fields the ship can't measure are nil. No positions are sent.
     */
    @LuaFunction
    public final List<Map<String, Object>> getTraffic() {
        final long now = this.now();
        final List<Map<String, Object>> out = new ArrayList<>();
        for (final Receiver.Track t : this.blockEntity.receiver().tracks()) {
            out.add(entry(t.report(), t.signal(), now - t.heard()));
        }
        return out;
    }

    /**
     * Get the raw messages received since an earlier call. The receiver keeps only the latest
     * ones (256 by default), so poll more often than they arrive if every one matters.
     *
     * @param since The {@code sequence} a previous call returned; omit or 0 for every message kept.
     * @return A table with {@code sequence}, the number of the newest message so far (pass it to
     * the next call), and {@code messages}, oldest first, each like a {@link #getTraffic} entry plus
     * its own {@code sequence}.
     */
    @LuaFunction
    public final Map<String, Object> getMessages(final Optional<Long> since) {
        final long now = this.now();
        final Receiver receiver = this.blockEntity.receiver();
        final long last = receiver.sequence();
        final List<Map<String, Object>> messages = new ArrayList<>();
        for (final Receiver.Message m : receiver.since(since.orElse(0L))) {
            if (m.sequence() > last) {
                break; // arrived after `last` was read; the next call gets it
            }
            final Map<String, Object> e = entry(m.report(), m.signal(), now - m.heard());
            e.put("sequence", m.sequence());
            messages.add(e);
        }
        final Map<String, Object> out = new HashMap<>();
        out.put("sequence", last);
        out.put("messages", messages);
        return out;
    }

    /**
     * Get the receiver's noise floor: the weakest broadcast it can hear, and the weakest radar
     * echo a radar reports.
     *
     * @return The noise floor in dBm.
     */
    @LuaFunction
    public final double getNoiseFloor() {
        return SignalModel.current.noiseFloor();
    }

    private long now() {
        return this.blockEntity.getLevel() == null ? 0 : this.blockEntity.getLevel().getGameTime();
    }

    private static Map<String, Object> entry(final ShipReport r, final double signal, final long ageTicks) {
        final Map<String, Object> e = new HashMap<>();
        e.put("address", r.address());
        e.put("callsign", r.callsign());
        e.put("squawk", r.squawk());
        // Fields the sending ship has no instrument for are left out, which reads as nil in Lua
        if (r.altitude() != null) e.put("altitude", r.altitude());
        if (r.heading() != null) e.put("heading", r.heading());
        if (r.speed() != null) e.put("speed", r.speed());
        if (r.verticalRate() != null) e.put("verticalRate", r.verticalRate());
        e.put("signal", signal);
        e.put("age", Math.max(0, ageTicks) / 20.0);
        return e;
    }
}
