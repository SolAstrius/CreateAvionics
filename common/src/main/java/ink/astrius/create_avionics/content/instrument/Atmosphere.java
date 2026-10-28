package ink.astrius.create_avionics.content.instrument;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import ink.astrius.create_avionics.compat.thermoo.ThermooAtmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.OptionalDouble;

/**
 * What the instruments measure. Every reading is taken at the block's world position, projected out
 * of any Sable sub-level, so an instrument on an airship reads the air the airship is flying through.
 * <p>
 * Instruments never talk to a climate mod directly: temperature and humidity come from Thermoo's
 * environment when it is installed (any mod plugs in there as a Thermoo provider), pressure comes
 * from Sable's dimension physics (the same value airship buoyancy uses), and both fall back to an
 * estimate from vanilla biome data. A mod with its own weather layer can hand precipitation and
 * pressure highs and lows to {@link InstrumentSources#weather}.
 */
public final class Atmosphere {

    /** Sea-level pressure that Sable's pressure fraction of 1.0 corresponds to. */
    public static final double SEA_LEVEL_HPA = 1013.25;
    /** Pressure drop under rain and thunder, on top of Sable's altitude curve. */
    private static final double RAIN_HPA = -8;
    private static final double THUNDER_HPA = -22;

    private Atmosphere() {
    }

    public record Reading(double value, String source) {
    }

    public enum Precipitation {
        NONE, RAIN, SNOW
    }

    /** The block's position in the world, outside any Sable sub-level. */
    public static BlockPos worldPos(final Level level, final BlockPos pos) {
        final Vector3d world = Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.atCenterOf(pos));
        return BlockPos.containing(world.x, world.y, world.z);
    }

    /** Air temperature in degrees Celsius. */
    public static Reading temperature(final Level level, final BlockPos world) {
        if (InstrumentSources.thermoo) {
            final OptionalDouble celsius = ThermooAtmosphere.temperature(level, world);
            if (celsius.isPresent()) {
                return new Reading(celsius.getAsDouble(), "thermoo");
            }
        }
        // Vanilla biome temperature, cooled above y = 80 the way vanilla does (minus its noise).
        // Roughly 0 at the snow line (0.15), 13 in plains (0.8), 37 in deserts (2.0), plus a few
        // degrees of day/night swing.
        final float biome = level.getBiome(world).value().getBaseTemperature()
                - Math.max(0, world.getY() - 80) * 0.05f / 40f;
        final double day = Math.cos(2 * Math.PI * ((level.getDayTime() % 24000) - 8000) / 24000.0);
        final double sky = level.canSeeSky(world) ? 1 : 0.3;
        return new Reading(20 * biome - 3 + 3 * day * sky, "vanilla");
    }

    /** Relative humidity, 0..1. */
    public static Reading humidity(final Level level, final BlockPos world) {
        if (InstrumentSources.thermoo) {
            final OptionalDouble rh = ThermooAtmosphere.humidity(level, world);
            if (rh.isPresent()) {
                return new Reading(Mth.clamp(rh.getAsDouble(), 0, 1), "thermoo");
            }
        }
        final Holder<Biome> biome = level.getBiome(world);
        double rh = Mth.clamp(InstrumentSources.downfall.applyAsDouble(biome.value()), 0, 1) * 0.8 + 0.1;
        if (level.isRainingAt(world.above()) || level.isRainingAt(world)) {
            rh = Math.max(rh, 0.95);
        }
        return new Reading(rh, "vanilla");
    }

    /** Static pressure in hectopascals: Sable's altitude curve, lowered by local weather. */
    public static Reading pressure(final Level level, final BlockPos world) {
        final double fraction = DimensionPhysicsData.getAirPressure(level, JOMLConversion.atCenterOf(world));
        double hpa = fraction * SEA_LEVEL_HPA;
        final double[] layer = weatherLayer(level, world);
        if (layer != null) {
            return new Reading(hpa + layer[2], "sable+" + InstrumentSources.weatherSource);
        }
        if (precipitation(level, world) != Precipitation.NONE) {
            hpa += level.isThundering() ? THUNDER_HPA : RAIN_HPA;
        }
        return new Reading(hpa, "sable");
    }

    /** What the installed weather layer says about a position, or null to measure vanilla's. */
    private static double @Nullable [] weatherLayer(final Level level, final BlockPos world) {
        if (InstrumentSources.weather == null || level.isClientSide()) {
            return null;
        }
        final double[] layer = InstrumentSources.weather.apply(level, world);
        return layer != null && layer.length >= 3 ? layer : null;
    }

    public static Precipitation precipitation(final Level level, final BlockPos world) {
        final double[] layer = weatherLayer(level, world);
        if (layer != null) {
            return layer[0] == 2 ? Precipitation.SNOW : layer[0] == 1 ? Precipitation.RAIN : Precipitation.NONE;
        }
        final BlockPos sky = world.above();
        if (!level.isRaining() || !level.canSeeSky(sky)) {
            return Precipitation.NONE;
        }
        return switch (level.getBiome(sky).value().getPrecipitationAt(sky)) {
            case RAIN -> Precipitation.RAIN;
            case SNOW -> Precipitation.SNOW;
            case NONE -> Precipitation.NONE;
        };
    }

    /** Precipitation rate in millimetres per hour (as liquid water for snow). */
    public static Reading precipitationRate(final Level level, final BlockPos world) {
        final double[] layer = weatherLayer(level, world);
        if (layer != null) {
            final double rate = layer[0] == 1 || layer[0] == 2 ? Math.max(0, layer[1]) : 0;
            return new Reading(rate, InstrumentSources.weatherSource);
        }
        final Precipitation type = precipitation(level, world);
        if (type == Precipitation.NONE) {
            return new Reading(0, "vanilla");
        }
        final double rate = level.isThundering() ? 8 : 2.5;
        return new Reading(type == Precipitation.SNOW ? rate * 0.5 : rate, "vanilla");
    }
}
