package ink.astrius.create_avionics.content.radar;

/**
 * One full turn of the weather radar at one elevation: a byte per range bin per radial for the
 * reflectivity and for the radial velocity, coded like NEXRAD's level III products.
 * <ul>
 * <li>{@code 0}: no echo above the weakest detectable one; {@code 1}: the beam was blocked before
 * this bin (terrain, or the radar's own ship); {@code 2}: no data (the weather there isn't known
 * yet, or for velocity, the wind isn't).</li>
 * <li>Reflectivity {@code 3..255}: dBZ = (b - 3) / 2 - 32, so -32 to +94 dBZ in 0.5 dB steps.</li>
 * <li>Velocity {@code 3..255}: (b - 129) / 4 blocks per second, positive away from the radar.</li>
 * </ul>
 * Immutable once built: the arrays are never written after the turn completes.
 *
 * @param index     which of the radar's elevations this turn used (0-based)
 * @param elevation degrees above the horizon
 * @param time      the game tick the turn completed
 */
public record WeatherSweep(int index, double elevation, long time, byte[][] reflectivity, byte[][] velocity) {

    public static final int NO_ECHO = 0;
    public static final int BLOCKED = 1;
    public static final int NO_DATA = 2;
    static final int FIRST = 3;

    public static int encodeDbz(final double dbz) {
        return (int) Math.max(FIRST, Math.min(255, Math.round((dbz + 32) * 2) + FIRST));
    }

    public static double decodeDbz(final int code) {
        return (code - FIRST) / 2.0 - 32;
    }

    public static int encodeVelocity(final double blocksPerSecond) {
        return (int) Math.max(FIRST, Math.min(255, Math.round(blocksPerSecond * 4) + 129));
    }

    public static double decodeVelocity(final int code) {
        return (code - 129) / 4.0;
    }
}
