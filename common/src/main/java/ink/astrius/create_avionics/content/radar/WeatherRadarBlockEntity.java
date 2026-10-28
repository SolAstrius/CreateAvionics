package ink.astrius.create_avionics.content.radar;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import ink.astrius.create_avionics.content.controller.ShipScan;
import ink.astrius.create_avionics.content.radio.Scatterer;
import ink.astrius.create_avionics.content.radio.SignalModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The weather radar: a dish that turns once every {@value #TURN_TICKS} ticks and, for each of its
 * {@value #RADIALS} radials, hears the echo of every {@value #BIN_LENGTH}-block range bin out to
 * 2048 blocks. What fills a bin comes from {@link WeatherField} (the weather source) and the
 * terrain under the beam; how much of it comes back is up to the {@link SignalModel}. The radar
 * reports apparent reflectivity: received power turned back into dBZ with its own calibration,
 * not corrected for the attenuation on the way, so a heavy core shadows what is behind it.
 * <p>
 * Radial 1 is the block's front; radials go clockwise as seen from above. On a ship the beam
 * stays level with the horizon (the antenna is stabilised) and turns with the hull. Where the
 * dish points follows from the game time alone, so the client renders it without syncing.
 */
public class WeatherRadarBlockEntity extends BlockEntity {

    public static final int RADIALS = 64;
    public static final int BINS = 64;
    public static final double BIN_LENGTH = 32;
    public static final double RANGE = BINS * BIN_LENGTH;
    /** One turn of the dish: 12 seconds. */
    public static final int TURN_TICKS = 240;
    public static final int MAX_ELEVATIONS = 8;
    public static final double MAX_ELEVATION = 30;
    /** The clutter filter removes echoes slower than this, blocks per second. */
    static final double FILTER_SPEED = 0.5;
    /** Fall speeds, blocks per second. */
    static final double RAIN_FALL = 6;
    static final double SNOW_FALL = 1;
    /** Reflectivity aloft thins by 1 dB per this many blocks above where it starts to. */
    static final double TAPER_BLOCKS = 10;
    /** How far out a radar on a ship checks for its own hull in the beam. */
    static final double OWN_SHIP_CHECK = 16;
    /** The dish's centre above the block's centre, in blocks. */
    static final double DISH_HEIGHT = 0.25;

    private double[] elevations = {0.5};
    private boolean clutterFilter;

    private long turn = Long.MIN_VALUE;
    private int nextRadial;
    private int turnIndex;
    private byte[][] reflectivity = new byte[RADIALS][BINS];
    private byte[][] velocity = new byte[RADIALS][BINS];
    private final WeatherSweep[] sweeps = new WeatherSweep[MAX_ELEVATIONS];
    private volatile @Nullable WeatherSweep latest;

    private @Nullable WeatherRadarPeripheral peripheral;

    public WeatherRadarBlockEntity(final BlockPos pos, final BlockState state) {
        super(Radars.WEATHER_BLOCK_ENTITY.get(), pos, state);
    }

    public WeatherRadarPeripheral peripheral() {
        if (this.peripheral == null) {
            this.peripheral = new WeatherRadarPeripheral(this);
        }
        return this.peripheral;
    }

    public Direction facing() {
        return this.getBlockState().getValue(WeatherRadarBlock.FACING);
    }

    public double[] elevations() {
        return this.elevations.clone();
    }

    public boolean clutterFilter() {
        return this.clutterFilter;
    }

    /** Set the elevations the radar cycles through, one per turn. Server thread. */
    public void setElevations(final double[] elevations) {
        this.elevations = elevations.clone();
        this.changed();
    }

    public void setClutterFilter(final boolean on) {
        this.clutterFilter = on;
        this.changed();
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The latest complete sweep, or the latest at one elevation index; null before there is one. */
    public @Nullable WeatherSweep sweep(final int index) {
        if (index < 0) {
            return this.latest;
        }
        synchronized (this.sweeps) {
            return index < MAX_ELEVATIONS ? this.sweeps[index] : null;
        }
    }

    /** The dish's progress through its turn at a (fractional) game time, 0..1. */
    public double turnFraction(final double gameTime) {
        final double t = gameTime + this.phase();
        return (t % TURN_TICKS + TURN_TICKS) % TURN_TICKS / TURN_TICKS;
    }

    /** The elevation the dish is at, at a game time: one per turn, cycling. */
    public double elevationAt(final double gameTime) {
        final double[] e = this.elevations;
        final long turn = (long) Math.floor((gameTime + this.phase()) / TURN_TICKS);
        return e[(int) Math.floorMod(turn, (long) e.length)];
    }

    private int phase() {
        return Math.floorMod(this.worldPosition.hashCode(), TURN_TICKS);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final WeatherRadarBlockEntity be) {
        if (level instanceof final ServerLevel server) {
            WeatherField.tick(server);
            be.scan(server);
        }
    }

    private void scan(final ServerLevel level) {
        final long t = level.getGameTime() + this.phase();
        final long turnNow = Math.floorDiv(t, TURN_TICKS);
        if (turnNow != this.turn) {
            // Finish the previous turn if it was the one just before; after a gap, start over
            if (this.turn == turnNow - 1 && this.nextRadial < RADIALS) {
                while (this.nextRadial < RADIALS) {
                    this.radial(level, this.nextRadial++);
                }
                this.publish(level);
            }
            this.turn = turnNow;
            this.nextRadial = 0;
            this.turnIndex = (int) Math.floorMod(turnNow, (long) this.elevations.length);
        }
        final int upTo = (int) (Math.floorMod(t, TURN_TICKS) * RADIALS / TURN_TICKS);
        final boolean wasComplete = this.nextRadial >= RADIALS;
        while (this.nextRadial <= upTo && this.nextRadial < RADIALS) {
            this.radial(level, this.nextRadial++);
        }
        if (!wasComplete && this.nextRadial >= RADIALS) {
            this.publish(level);
        }
    }

    private void publish(final ServerLevel level) {
        final int index = Math.min(this.turnIndex, this.elevations.length - 1);
        final WeatherSweep sweep = new WeatherSweep(index, this.elevations[index], level.getGameTime(),
                this.reflectivity, this.velocity);
        this.reflectivity = new byte[RADIALS][BINS];
        this.velocity = new byte[RADIALS][BINS];
        synchronized (this.sweeps) {
            this.sweeps[index] = sweep;
        }
        this.latest = sweep;
        if (this.peripheral != null) {
            this.peripheral.sweepDone(sweep);
        }
    }

    /** Listen along radial {@code r} of the current turn and code it into the sweep being built. */
    private void radial(final ServerLevel level, final int r) {
        final byte[] refl = this.reflectivity[r];
        final byte[] vel = this.velocity[r];
        final double elevation = Math.toRadians(this.elevations[Math.min(this.turnIndex, this.elevations.length - 1)]);
        final double cosE = Math.cos(elevation);
        final double sinE = Math.sin(elevation);

        // The beam's compass direction: the block's front, turned clockwise by the azimuth, then out
        // of the ship's frame, kept level
        final Direction front = this.facing();
        final Direction right = front.getClockWise();
        final double a = (r + 0.5) * 2 * Math.PI / RADIALS;
        final double lx = front.getStepX() * Math.cos(a) + right.getStepX() * Math.sin(a);
        final double lz = front.getStepZ() * Math.cos(a) + right.getStepZ() * Math.sin(a);
        final Vec3 local = Vec3.atCenterOf(this.worldPosition).add(0, DISH_HEIGHT, 0);
        final Vec3 origin = Sable.HELPER.projectOutOfSubLevel(level, local);
        final Vec3 ahead = Sable.HELPER.projectOutOfSubLevel(level, local.add(lx, 0, lz)).subtract(origin);
        final double h = Math.sqrt(ahead.x * ahead.x + ahead.z * ahead.z);
        if (h < 0.1) {
            // The ship is on its side: the antenna can't stay level
            Arrays.fill(refl, (byte) WeatherSweep.BLOCKED);
            Arrays.fill(vel, (byte) WeatherSweep.BLOCKED);
            return;
        }
        final double hx = ahead.x / h;
        final double hz = ahead.z / h;

        // Own motion and own hull
        final SubLevel ship = ShipScan.shipOf(this);
        Vec3 own = Vec3.ZERO;
        double hull = Double.POSITIVE_INFINITY;
        if (ship != null) {
            own = ship.logicalPose().transformPosition(local).subtract(ship.lastPose().transformPosition(local)).scale(20);
            hull = this.ownHull(level, ship, local, lx * cosE, sinE, lz * cosE);
        }

        final SignalModel model = SignalModel.current;
        final Scatterer[] kind = new Scatterer[BINS];
        final double[] rate = new double[BINS];
        final double[] wind = new double[BINS];  // the scatterer's radial velocity, NaN unknown
        final boolean[] noData = new boolean[BINS];
        Arrays.fill(kind, Scatterer.NONE);
        Arrays.fill(wind, Double.NaN);
        int blockedFrom = BINS;
        for (int i = 0; i < BINS; i++) {
            final double range = (i + 0.5) * BIN_LENGTH;
            if (i * BIN_LENGTH >= hull) {
                blockedFrom = i;
                break;
            }
            final double x = origin.x + hx * range * cosE;
            final double z = origin.z + hz * range * cosE;
            final double y = origin.y + range * sinE;
            final int bx = (int) Math.floor(x);
            final int bz = (int) Math.floor(z);
            // Terrain: the heightmap, where the chunk is loaded. Read from the chunk itself: Level's
            // getHeight waits for the chunk even where hasChunk said it was there
            final LevelChunk chunk = level.getChunkSource().getChunkNow(
                    SectionPos.blockToSectionCoord(bx), SectionPos.blockToSectionCoord(bz));
            if (chunk != null) {
                final int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx & 15, bz & 15) + 1;
                if (y < top) {
                    kind[i] = chunk.getFluidState(new BlockPos(bx, top - 1, bz)).is(FluidTags.WATER)
                            ? Scatterer.SEA : Scatterer.GROUND;
                    wind[i] = 0;
                    blockedFrom = i + 1;
                    break;
                }
            }
            final WeatherField.Column column = WeatherField.at(level, x, z);
            if (column == null) {
                noData[i] = true;
                continue;
            }
            fill(column, y, kind, rate, i);
            if (kind[i] != Scatterer.NONE && !Double.isNaN(column.windX()) && !Double.isNaN(column.windZ())) {
                final double fall = kind[i] == Scatterer.RAIN ? RAIN_FALL
                        : kind[i] == Scatterer.SNOW || kind[i] == Scatterer.ICE ? SNOW_FALL : 0;
                wind[i] = (column.windX() * hx + column.windZ() * hz) * cosE - fall * sinE;
            }
        }

        final double[] power = new double[BINS];
        model.weatherRadial(BIN_LENGTH, kind, rate, power);
        final double ownRadial = (own.x * hx + own.z * hz) * cosE + own.y * sinE;
        for (int i = 0; i < BINS; i++) {
            if (i >= blockedFrom) {
                refl[i] = (byte) WeatherSweep.BLOCKED;
                vel[i] = (byte) WeatherSweep.BLOCKED;
                continue;
            }
            if (noData[i]) {
                refl[i] = (byte) WeatherSweep.NO_DATA;
                vel[i] = (byte) WeatherSweep.NO_DATA;
                continue;
            }
            final double range = (i + 0.5) * BIN_LENGTH;
            if (power[i] == Double.NEGATIVE_INFINITY || power[i] < model.noiseFloor()) {
                refl[i] = (byte) WeatherSweep.NO_ECHO;
                vel[i] = (byte) WeatherSweep.NO_ECHO;
                continue;
            }
            final double v = Double.isNaN(wind[i]) ? Double.NaN : wind[i] - ownRadial;
            if (this.clutterFilter && !Double.isNaN(v) && Math.abs(v) < FILTER_SPEED) {
                refl[i] = (byte) WeatherSweep.NO_ECHO;
                vel[i] = (byte) WeatherSweep.NO_ECHO;
                continue;
            }
            refl[i] = (byte) WeatherSweep.encodeDbz(power[i] - model.volumeEcho(range, 0));
            vel[i] = (byte) (Double.isNaN(v) ? WeatherSweep.NO_DATA : WeatherSweep.encodeVelocity(v));
        }
    }

    /**
     * What fills the beam at height {@code y} in a column: rain below the freezing level, ice
     * above it (snow all the way down where it snows at the ground), thinning above the freezing
     * level or the cloud base, nothing above the cloud top; cloud without precipitation between
     * base and top. Sets {@code kind[i]} and {@code rate[i]}.
     */
    static void fill(final WeatherField.Column c, final double y, final Scatterer[] kind, final double[] rate, final int i) {
        if (!c.precipitates()) {
            if (c.cloud() > 0 && y >= c.cloudBase() && y <= c.cloudTop()) {
                kind[i] = Scatterer.CLOUD;
                rate[i] = c.cloud();
            }
            return;
        }
        final double top = Double.isNaN(c.cloudTop()) ? WeatherField.VANILLA_CLOUD_TOP : c.cloudTop();
        if (y > top) {
            return;
        }
        final double thinsFrom;
        if (c.kind() == 2) {
            kind[i] = Scatterer.SNOW;
            thinsFrom = Double.isNaN(c.cloudBase()) ? top : c.cloudBase();
        } else if (!Double.isNaN(c.freezingLevel()) && y > c.freezingLevel()) {
            kind[i] = Scatterer.ICE;
            thinsFrom = c.freezingLevel();
        } else {
            kind[i] = Scatterer.RAIN;
            thinsFrom = top;
        }
        // 1 dB per TAPER_BLOCKS, as a rate (Z grows about as R^1.6)
        final double db = Math.max(0, y - thinsFrom) / TAPER_BLOCKS;
        rate[i] = c.rate() * Math.pow(10, -db / 16);
    }

    /** How far out the radar's own hull stands in the beam, in blocks; infinite if it doesn't. */
    private double ownHull(final Level level, final SubLevel ship, final Vec3 local, final double dx, final double dy,
                           final double dz) {
        final Vec3 dir = new Vec3(dx, dy, dz).normalize();
        final Vec3 start = local.add(dir.scale(0.6));
        final BlockHitResult hit = level.clip(new ClipContext(start, start.add(dir.scale(OWN_SHIP_CHECK)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        final SubLevel hitShip = Sable.HELPER.getContaining(level, hit.getLocation());
        if (hitShip == null || !hitShip.getUniqueId().equals(ship.getUniqueId())) {
            return Double.POSITIVE_INFINITY;
        }
        return hit.getLocation().distanceTo(local);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final long[] bits = new long[this.elevations.length];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = Double.doubleToLongBits(this.elevations[i]);
        }
        tag.putLongArray("Elevations", bits);
        tag.putBoolean("ClutterFilter", this.clutterFilter);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final long[] bits = tag.getLongArray("Elevations");
        if (bits.length > 0 && bits.length <= MAX_ELEVATIONS) {
            final double[] e = new double[bits.length];
            for (int i = 0; i < bits.length; i++) {
                e[i] = Math.clamp(Double.longBitsToDouble(bits[i]), 0, MAX_ELEVATION);
            }
            this.elevations = e;
        }
        this.clutterFilter = tag.getBoolean("ClutterFilter");
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
