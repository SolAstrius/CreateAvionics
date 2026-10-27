package ink.astrius.create_avionics.content.instrument;

import net.minecraft.world.level.biome.Biome;

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

    private InstrumentSources() {
    }
}
