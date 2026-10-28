package ink.astrius.create_avionics.content.instrument;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

/**
 * Which optional data sources are installed. Set once by the loader entrypoint, before any world
 * loads; {@code common} can't ask the mod loader itself.
 */
public final class InstrumentSources {

    /** Thermoo: temperature and humidity from its environment providers. */
    public static boolean thermoo;
    /** Serene Seasons: the almanac's season calendar. */
    public static boolean sereneSeasons;
    /**
     * A biome's downfall (0 dry .. 1 wet), with the loader's biome modifiers applied. Vanilla keeps
     * the climate settings private, and NeoForge redirects every read of them to its own getter.
     */
    public static ToDoubleFunction<Biome> downfall = biome -> 0.5;
    /**
     * A weather layer from another mod, sent over InterModComms as {@code weather_source}: for a
     * world position on the server, null to leave it to vanilla, else
     * {@code {kind (0 none, 1 rain, 2 snow), mm/h as liquid water, hPa added to Sable's curve}}.
     */
    public static @Nullable BiFunction<Level, BlockPos, double[]> weather;
    /** The mod that sent {@link #weather}, reported as the readings' source. */
    public static String weatherSource = "vanilla";

    private InstrumentSources() {
    }
}
