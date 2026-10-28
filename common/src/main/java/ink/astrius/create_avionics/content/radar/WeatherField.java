package ink.astrius.create_avionics.content.radar;

import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.content.instrument.Atmosphere;
import ink.astrius.create_avionics.content.instrument.InstrumentSources;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The weather as the weather radars see it: one column of weather per grid cell, shared by every
 * radar in a level, sampled from the weather source under a per-tick budget and kept for a while.
 * <p>
 * With a {@code weather_field} source (see {@link InstrumentSources#field}) the grid is 64 blocks
 * and covers unloaded chunks too. Without one it falls back to what the rain gauge would read at
 * the surface ({@link Atmosphere}), on a 16-block grid, in loaded chunks only; precipitation then
 * reaches up to vanilla's cloud layer. Nothing here ever loads a chunk.
 */
public final class WeatherField {

    /** Source calls per level per tick, at most. Set from the server config. */
    public static int columnsPerTick = 32;

    static final int FIELD_SPACING = 64;
    static final int POINT_SPACING = 16;
    /** A cell older than this is sampled again when a radar looks at it. */
    static final int REFRESH_TICKS = 200;
    /** Cells no radar looked at for this long are dropped. */
    static final int FORGET_TICKS = 1200;
    static final int MAX_BATCH = 256;
    static final int MAX_QUEUE = 1 << 16;
    /** Vanilla's cloud layer, the top of precipitation when the source doesn't say. */
    public static final double VANILLA_CLOUD_TOP = 192;

    /**
     * One column's weather. {@code kind} 0 none, 1 rain, 2 snow (what falls out of the cloud base);
     * {@code rate} mm/h as liquid water; heights are world y; wind in blocks per second, x east and
     * z south; {@code cloud} the cover, 0..1. NaN for anything the source doesn't say. This is also
     * the order of the values a {@code weather_field} source returns.
     */
    public record Column(int kind, double rate, double cloudBase, double cloudTop, double windX, double windZ,
                         double freezingLevel, double cloud) {

        static Column read(final double[] v, final int at, final int stride) {
            return new Column(stride > 0 && !Double.isNaN(v[at]) ? (int) Math.round(v[at]) : 0,
                    value(v, at, stride, 1), value(v, at, stride, 2), value(v, at, stride, 3),
                    value(v, at, stride, 4), value(v, at, stride, 5), value(v, at, stride, 6), value(v, at, stride, 7));
        }

        private static double value(final double[] v, final int at, final int stride, final int field) {
            return field < stride ? v[at + field] : Double.NaN;
        }

        public boolean precipitates() {
            return (this.kind == 1 || this.kind == 2) && this.rate > 0;
        }

        static Column lerp(final double t, final Column a, final Column b) {
            return new Column(t < 0.5 ? a.kind : b.kind, a.rate + t * (b.rate - a.rate),
                    a.cloudBase + t * (b.cloudBase - a.cloudBase), a.cloudTop + t * (b.cloudTop - a.cloudTop),
                    a.windX + t * (b.windX - a.windX), a.windZ + t * (b.windZ - a.windZ),
                    a.freezingLevel + t * (b.freezingLevel - a.freezingLevel), a.cloud + t * (b.cloud - a.cloud));
        }
    }

    /** A sampled grid point: its column (null where the source can't say) and when. */
    private record Cell(@Nullable Column column, long time) {
    }

    private static final class LevelField {
        final Long2ObjectOpenHashMap<Cell> cells = new Long2ObjectOpenHashMap<>();
        final LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();
        final Long2LongOpenHashMap seen = new Long2LongOpenHashMap();
        long lastTick = Long.MIN_VALUE;
        /** Cleared when the field source says the level isn't its own, or fails. */
        boolean field = InstrumentSources.field != null;

        int spacing() {
            return this.field ? FIELD_SPACING : POINT_SPACING;
        }
    }

    private static final Map<Level, LevelField> LEVELS = new WeakHashMap<>();

    private WeatherField() {
    }

    private static LevelField of(final Level level) {
        return LEVELS.computeIfAbsent(level, l -> new LevelField());
    }

    /** Where the weather comes from in this level, for Lua: the source mod's id, or vanilla. */
    public static String source(final Level level) {
        final LevelField f = of(level);
        return f.field ? InstrumentSources.fieldSource : InstrumentSources.weatherSource;
    }

    /**
     * The weather over a world column, interpolated between grid points; null if none of the
     * surrounding points is known yet. Unknown or stale points are queued for sampling.
     */
    public static @Nullable Column at(final Level level, final double x, final double z) {
        final LevelField f = of(level);
        final int s = f.spacing();
        final double gx = x / s;
        final double gz = z / s;
        final int ix = (int) Math.floor(gx);
        final int iz = (int) Math.floor(gz);
        final double tx = gx - ix;
        final double tz = gz - iz;
        final long now = level.getGameTime();
        final Column a = point(level, f, ix, iz, now);
        final Column b = point(level, f, ix + 1, iz, now);
        final Column c = point(level, f, ix, iz + 1, now);
        final Column d = point(level, f, ix + 1, iz + 1, now);
        if (a != null && b != null && c != null && d != null) {
            return Column.lerp(tz, Column.lerp(tx, a, b), Column.lerp(tx, c, d));
        }
        // At the edge of what is known: the nearest known point
        final Column near = tx < 0.5 ? (tz < 0.5 ? a : c) : (tz < 0.5 ? b : d);
        if (near != null) {
            return near;
        }
        return a != null ? a : b != null ? b : c != null ? c : d;
    }

    private static @Nullable Column point(final Level level, final LevelField f, final int ix, final int iz,
                                          final long now) {
        // Rain-gauge readings exist only in loaded chunks: don't queue the rest
        if (!f.field && !level.hasChunk(SectionPos.blockToSectionCoord(ix * POINT_SPACING),
                SectionPos.blockToSectionCoord(iz * POINT_SPACING))) {
            return null;
        }
        final long key = BlockPos.asLong(ix, 0, iz);
        f.seen.put(key, now);
        final Cell cell = f.cells.get(key);
        if ((cell == null || now - cell.time() >= REFRESH_TICKS) && f.queue.size() < MAX_QUEUE) {
            f.queue.add(key);
        }
        return cell == null ? null : cell.column();
    }

    /** Sample queued cells within the budget. Called from the radars' ticks; runs once per game tick. */
    public static void tick(final ServerLevel level) {
        final LevelField f = of(level);
        final long now = level.getGameTime();
        if (f.lastTick == now) {
            return;
        }
        f.lastTick = now;
        if (now % REFRESH_TICKS == 0) {
            forget(f, now);
        }
        int budget = columnsPerTick;
        while (budget > 0 && !f.queue.isEmpty()) {
            final int n = Math.min(Math.min(budget, MAX_BATCH), f.queue.size());
            final long[] keys = new long[n];
            for (int i = 0; i < n; i++) {
                keys[i] = f.queue.removeFirstLong();
            }
            budget -= n;
            if (f.field && sampleField(level, f, keys, now)) {
                continue;
            }
            for (final long key : keys) {
                f.cells.put(key, new Cell(samplePoint(level, BlockPos.getX(key) * POINT_SPACING,
                        BlockPos.getZ(key) * POINT_SPACING), now));
            }
        }
    }

    /** Ask the {@code weather_field} source; false if it won't answer for this level (then never again). */
    private static boolean sampleField(final ServerLevel level, final LevelField f, final long[] keys, final long now) {
        final double[] query = new double[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            query[2 * i] = BlockPos.getX(keys[i]) * (double) FIELD_SPACING;
            query[2 * i + 1] = BlockPos.getZ(keys[i]) * (double) FIELD_SPACING;
        }
        double[] out;
        try {
            out = InstrumentSources.field == null ? null : InstrumentSources.field.apply(level, query);
        } catch (final RuntimeException e) {
            CreateAvionics.LOGGER.error("Weather field of {} failed; the weather radar falls back to {}",
                    InstrumentSources.fieldSource, InstrumentSources.weatherSource, e);
            out = null;
        }
        final int stride = out == null ? 0 : out.length / keys.length;
        if (out == null || stride < 2) {
            // Not this source's level: sample points instead, on their own grid
            f.field = false;
            f.cells.clear();
            f.queue.clear();
            f.seen.clear();
            return false;
        }
        for (int i = 0; i < keys.length; i++) {
            f.cells.put(keys[i], new Cell(Column.read(out, i * stride, stride), now));
        }
        return true;
    }

    /** What the rain gauge would read at the surface of a loaded column; null if it isn't loaded. */
    private static @Nullable Column samplePoint(final ServerLevel level, final int x, final int z) {
        // From the chunk itself: Level's getHeight waits for the chunk even where hasChunk said it was there
        final LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        if (chunk == null) {
            return null;
        }
        final int y = Math.min(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1, level.getMaxBuildHeight() - 2);
        final BlockPos pos = new BlockPos(x, y, z);
        final int kind = switch (Atmosphere.precipitation(level, pos)) {
            case NONE -> 0;
            case RAIN -> 1;
            case SNOW -> 2;
        };
        final double rate = kind == 0 ? 0 : Atmosphere.precipitationRate(level, pos).value();
        return new Column(kind, rate, Double.NaN, kind == 0 ? Double.NaN : VANILLA_CLOUD_TOP,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    private static void forget(final LevelField f, final long now) {
        final LongIterator it = f.seen.keySet().iterator();
        while (it.hasNext()) {
            final long key = it.nextLong();
            if (now - f.seen.get(key) > FORGET_TICKS) {
                it.remove();
                f.cells.remove(key);
            }
        }
    }
}
