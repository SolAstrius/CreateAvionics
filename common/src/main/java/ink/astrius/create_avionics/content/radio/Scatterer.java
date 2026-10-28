package ink.astrius.create_avionics.content.radio;

/**
 * What fills a weather radar's range bin. Precipitation scatters by its rate; the ground and the
 * sea are hard clutter that also stops the beam.
 */
public enum Scatterer {
    /** Nothing that returns an echo. */
    NONE,
    RAIN,
    SNOW,
    /** Frozen precipitation above the freezing level, in a column that rains at the ground. */
    ICE,
    /** Cloud droplets without precipitation; the rate is the cloud cover, 0..1. */
    CLOUD,
    /** Terrain in the beam. */
    GROUND,
    /** Water in the beam, which reflects less than land at a grazing angle. */
    SEA;

    public boolean clutter() {
        return this == GROUND || this == SEA;
    }
}
