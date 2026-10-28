package ink.astrius.create_avionics.content.radar;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.NotAttachedException;
import ink.astrius.create_avionics.content.radio.SignalModel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The weather radar: a dish that turns once every 12 seconds and hears rain, snow and cloud out to
 * 2048 blocks. Each turn is a sweep at one elevation angle: 64 radials (5.6° each, radial 1 on the
 * block's front, clockwise as seen from above) of 64 range bins, 32 blocks each.
 * <p>
 * Each bin carries the reflectivity in dBZ and the radial velocity of what is in it. Like a real
 * radar, it reports apparent reflectivity. It works that out from the received power alone, so
 * heavy rain weakens everything behind it (one way, 25 mm/h costs about 0.07 dB per block).
 * Typical values: drizzle 5 to 15 dBZ, rain 25 to 40, a downpour 45 and up, snow 20 to 35. Cloud
 * without precipitation is -25 to -10 dBZ, which only the near bins hear. The weakest echo the
 * radar hears is -20 dBZ at 100 blocks, -6 at 512 and +6 at 2048.
 * <p>
 * Terrain in the beam returns strong ground clutter (55 dBZ, 25 off water), and blocks everything
 * after it. Site the radar high, or tilt the beam up. Clutter moves only with the radar, so on the
 * ground the clutter filter can remove it. The filter also removes weather moving across the beam.
 * Velocity needs a weather mod that reports wind. In vanilla, precipitation reaches up to the
 * cloud layer, and the radar only knows the weather over loaded chunks.
 * <p>
 * Sweeps come back as binary strings, one per radial, one byte per bin:
 * <ul>
 * <li>{@code 0} no echo, {@code 1} beam blocked, {@code 2} no data.</li>
 * <li>Reflectivity {@code 3..255}: {@code dbz = (b - 3) / 2 - 32}.</li>
 * <li>Velocity {@code 3..255}: {@code v = (b - 129) / 4} blocks per second, positive away from the
 * radar.</li>
 * </ul>
 * <p>
 * Read a bin with {@code sweep.reflectivity[radial]:byte(bin)}. The bin's height above the dish is
 * {@code distance * sin(elevation)}, where {@code distance = (bin - 0.5) * 32}.
 * <p>
 * Event: {@code weather_radar_sweep} when a turn completes, with the radar's name, the sweep's
 * elevation index and the elevation in degrees.
 *
 * @cc.module weather_radar
 */
public class WeatherRadarPeripheral implements IPeripheral {

    private final WeatherRadarBlockEntity blockEntity;
    private final Set<IComputerAccess> computers = ConcurrentHashMap.newKeySet();

    public WeatherRadarPeripheral(final WeatherRadarBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "weather_radar";
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other instanceof final WeatherRadarPeripheral o && o.blockEntity == this.blockEntity;
    }

    @Override
    public void attach(final IComputerAccess computer) {
        this.computers.add(computer);
    }

    @Override
    public void detach(final IComputerAccess computer) {
        this.computers.remove(computer);
    }

    void sweepDone(final WeatherSweep sweep) {
        for (final IComputerAccess computer : this.computers) {
            try {
                computer.queueEvent("weather_radar_sweep", computer.getAttachmentName(), sweep.index() + 1, sweep.elevation());
            } catch (final NotAttachedException e) {
                this.computers.remove(computer);
            }
        }
    }

    /**
     * Get the latest complete sweep.
     *
     * @param index Optional. Which of the elevations set with {@link #setElevations} (1-based) to
     *              get the latest sweep at. Without it, the latest sweep at any elevation.
     * @return Nil before the first sweep completes. Otherwise a table:
     * <ul>
     * <li>{@code elevation}: degrees above the horizon.</li>
     * <li>{@code index}: which elevation (1-based).</li>
     * <li>{@code radials}, {@code bins}: 64 and 64.</li>
     * <li>{@code binLength}: 32 blocks.</li>
     * <li>{@code time}: the game tick it completed.</li>
     * <li>{@code age}: seconds since then.</li>
     * <li>{@code reflectivity}, {@code velocity}: tables of 64 binary strings of 64 bytes each,
     * indexed by radial.</li>
     * </ul>
     * @throws LuaException If the index is out of range.
     */
    @LuaFunction
    public final @Nullable Map<String, Object> getSweep(final Optional<Integer> index) throws LuaException {
        final int i = index.map(n -> n - 1).orElse(-1);
        if (index.isPresent() && (i < 0 || i >= WeatherRadarBlockEntity.MAX_ELEVATIONS)) {
            throw new LuaException("index out of range (1-" + WeatherRadarBlockEntity.MAX_ELEVATIONS + ")");
        }
        final WeatherSweep sweep = this.blockEntity.sweep(i);
        if (sweep == null) {
            return null;
        }
        final Map<String, Object> out = new HashMap<>();
        out.put("elevation", sweep.elevation());
        out.put("index", sweep.index() + 1);
        out.put("radials", WeatherRadarBlockEntity.RADIALS);
        out.put("bins", WeatherRadarBlockEntity.BINS);
        out.put("binLength", WeatherRadarBlockEntity.BIN_LENGTH);
        out.put("time", sweep.time());
        out.put("age", this.age(sweep));
        out.put("reflectivity", strings(sweep.reflectivity()));
        out.put("velocity", strings(sweep.velocity()));
        return out;
    }

    private double age(final WeatherSweep sweep) {
        final Level level = this.blockEntity.getLevel();
        return level == null ? 0 : Math.max(0, level.getGameTime() - sweep.time()) / 20.0;
    }

    private static Map<Integer, ByteBuffer> strings(final byte[][] rows) {
        final Map<Integer, ByteBuffer> out = new HashMap<>();
        for (int r = 0; r < rows.length; r++) {
            out.put(r + 1, ByteBuffer.wrap(rows[r]).asReadOnlyBuffer());
        }
        return out;
    }

    /**
     * Get one range bin of the latest sweep, decoded.
     *
     * @param radial The radial, 1 to 64 (1 is the block's front, clockwise from above).
     * @param bin    The range bin, 1 to 64 (1 is nearest).
     * @return Nil before the first sweep. Otherwise a table:
     * <ul>
     * <li>{@code distance}: to the bin's centre, in blocks.</li>
     * <li>{@code height}: above the dish, in blocks.</li>
     * <li>{@code dbz}: nil if there is no echo.</li>
     * <li>{@code velocity}: blocks per second, positive away; nil if unknown.</li>
     * <li>{@code blocked}: true if the beam was stopped before this bin.</li>
     * <li>{@code noData}: true if the weather there isn't known.</li>
     * </ul>
     * @throws LuaException If the radial or bin is out of range.
     */
    @LuaFunction
    public final @Nullable Map<String, Object> getBin(final int radial, final int bin) throws LuaException {
        if (radial < 1 || radial > WeatherRadarBlockEntity.RADIALS || bin < 1 || bin > WeatherRadarBlockEntity.BINS) {
            throw new LuaException("radial and bin go from 1 to 64");
        }
        final WeatherSweep sweep = this.blockEntity.sweep(-1);
        if (sweep == null) {
            return null;
        }
        final int z = sweep.reflectivity()[radial - 1][bin - 1] & 0xff;
        final int v = sweep.velocity()[radial - 1][bin - 1] & 0xff;
        final double distance = (bin - 0.5) * WeatherRadarBlockEntity.BIN_LENGTH;
        final Map<String, Object> out = new HashMap<>();
        out.put("distance", distance);
        out.put("height", distance * Math.sin(Math.toRadians(sweep.elevation())));
        out.put("blocked", z == WeatherSweep.BLOCKED);
        out.put("noData", z == WeatherSweep.NO_DATA);
        if (z >= WeatherSweep.FIRST) {
            out.put("dbz", WeatherSweep.decodeDbz(z));
        }
        if (v >= WeatherSweep.FIRST) {
            out.put("velocity", WeatherSweep.decodeVelocity(v));
        }
        return out;
    }

    /**
     * Set the elevations to scan, one per turn, cycling: a volume scan.
     *
     * @param elevations A list of 1 to 8 angles in degrees above the horizon, each 0 to 30.
     * @throws LuaException If the list is empty, too long, or holds anything else.
     */
    @LuaFunction(mainThread = true)
    public final void setElevations(final Map<?, ?> elevations) throws LuaException {
        final int n = elevations.size();
        if (n < 1 || n > WeatherRadarBlockEntity.MAX_ELEVATIONS) {
            throw new LuaException("between 1 and " + WeatherRadarBlockEntity.MAX_ELEVATIONS + " elevations");
        }
        final double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            if (!(elevations.get((double) (i + 1)) instanceof final Number e)
                    || !(e.doubleValue() >= 0 && e.doubleValue() <= WeatherRadarBlockEntity.MAX_ELEVATION)) {
                throw new LuaException("elevations must be a list of numbers from 0 to "
                        + (int) WeatherRadarBlockEntity.MAX_ELEVATION);
            }
            out[i] = e.doubleValue();
        }
        this.blockEntity.setElevations(out);
    }

    /**
     * Get the elevations the radar cycles through.
     *
     * @return A list of angles in degrees above the horizon (default {@code {0.5}}).
     */
    @LuaFunction
    public final Map<Integer, Double> getElevations() {
        final double[] e = this.blockEntity.elevations();
        final Map<Integer, Double> out = new HashMap<>();
        for (int i = 0; i < e.length; i++) {
            out.put(i + 1, e[i]);
        }
        return out;
    }

    /**
     * Turn the clutter filter on or off. It removes echoes moving slower than 0.5 blocks per
     * second towards or away from the radar: ground clutter, as long as the radar stands still, but
     * also weather moving across the beam.
     *
     * @param on Whether to filter.
     */
    @LuaFunction(mainThread = true)
    public final void setClutterFilter(final boolean on) {
        this.blockEntity.setClutterFilter(on);
    }

    /**
     * Get whether the clutter filter is on.
     *
     * @return True if it is on (default off).
     */
    @LuaFunction
    public final boolean getClutterFilter() {
        return this.blockEntity.clutterFilter();
    }

    /**
     * Get where the dish points now.
     *
     * @return Degrees clockwise from the block's front, as seen from above.
     */
    @LuaFunction
    public final double getAzimuth() {
        final Level level = this.blockEntity.getLevel();
        return level == null ? 0 : 360 * this.blockEntity.turnFraction(level.getGameTime());
    }

    /**
     * Get the weakest echo the radar hears at a distance.
     *
     * @param distance The distance in blocks.
     * @return The reflectivity in dBZ.
     */
    @LuaFunction
    public final double getMinimumDetectable(final double distance) {
        final SignalModel model = SignalModel.current;
        return model.noiseFloor() - model.volumeEcho(distance, 0);
    }

    /**
     * Get how far the radar sees.
     *
     * @return 2048 blocks: 64 range bins of 32.
     */
    @LuaFunction
    public final double getRange() {
        return WeatherRadarBlockEntity.RANGE;
    }
}
