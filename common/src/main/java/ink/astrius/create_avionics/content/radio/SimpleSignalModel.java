package ink.astrius.create_avionics.content.radio;

import ink.astrius.create_avionics.content.instrument.Atmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Free-space path loss with constants tuned for Minecraft distances, plus rain. A broadcast falls
 * with the square of distance, an echo with the fourth power (out and back), so radar range is
 * much shorter than radio range and grows only slowly with the target's size. Precipitation
 * scatters by the usual Z-R relations, and attenuates by its rate.
 */
public final class SimpleSignalModel implements SignalModel {

    /*
     * With these, a 30 dBm broadcast is heard out to ~3160 blocks (40 dBm: 10 000, 10 dBm: 316,
     * 0 dBm: 100), and a target with 100 blocks² facing the radar shows up out to ~250 blocks
     * (400 blocks²: ~355).
     */
    /** Broadcast loss at 1 block, dB. */
    static final double BROADCAST_LOSS_1 = 60;
    /** Broadcast loss per decade of distance, dB (free space: 20). */
    static final double BROADCAST_LOSS_PER_DECADE = 20;
    /** Radar transmit power plus antenna gains, dBm. */
    static final double RADAR_POWER = 60;
    /** Echo loss at 1 block, dB. */
    static final double ECHO_LOSS_1 = 84;
    /** Echo loss per decade of distance, dB (radar equation: 40). */
    static final double ECHO_LOSS_PER_DECADE = 40;
    static final double NOISE_FLOOR = -100;

    /*
     * Weather. Rain Z = 200 R^1.6 (Marshall-Palmer): 1 mm/h is 23 dBZ, 2.5 (vanilla rain) 29,
     * 8 (vanilla thunder) 37.5, 25 45, 100 55. Snow Ze = 400 R^2 by its water: 1.25 mm/h (vanilla
     * snow) is 28 dBZ. Ice aloft is rain's minus 7 dB, ice's weaker dielectric factor. The volume
     * echo P = -40 dBm + dBZ - 20 log r makes the weakest detectable echo -60 + 20 log r dBZ:
     * -20 at 100 blocks, -6 at 512, +6 at 2048, so drizzle (0.1 mm/h, 7 dBZ) just reaches full range.
     */
    /** Rain Z-R: Z = A R^B, mm^6/m^3 with R in mm/h. */
    static final double RAIN_Z_A = 200;
    static final double RAIN_Z_B = 1.6;
    /** Snow Ze = A R^B, R as liquid water. */
    static final double SNOW_Z_A = 400;
    static final double SNOW_Z_B = 2;
    /** Ice crystals aloft against rain at the same rate, dB. */
    static final double ICE_DB = -7;
    /** Cloud without precipitation: this at full cover, 15 dB less at none. */
    static final double CLOUD_DBZ = -10;
    /** Clutter, as an equivalent reflectivity. */
    static final double GROUND_DBZ = 55;
    static final double SEA_DBZ = 25;
    /** Volume echo at 1 block from 0 dBZ, dBm (radar constant). */
    static final double VOLUME_ECHO_1 = -40;
    /**
     * Rain attenuation, one way, dB per block: A R^B. 2.5 mm/h is 0.0045, 8 mm/h 0.018 (what the
     * flat rain loss used to be), 25 mm/h 0.071. Snow and ice are a tenth of rain's.
     */
    static final double RAIN_K_A = 0.0015;
    static final double RAIN_K_B = 1.2;
    static final double FROZEN_K = 0.1;

    @Override
    public double broadcast(final Level level, final Vec3 from, final Vec3 to, final double txPowerDbm) {
        final double d = from.distanceTo(to);
        return txPowerDbm - (BROADCAST_LOSS_1 + BROADCAST_LOSS_PER_DECADE * Math.log10(Math.max(d, 1)))
                - this.rainLoss(level, from, to, d);
    }

    @Override
    public double echo(final Level level, final Vec3 radar, final Vec3 target, final double crossSectionBlocks2) {
        final double d = radar.distanceTo(target);
        return RADAR_POWER + 10 * Math.log10(Math.max(crossSectionBlocks2, 1e-3))
                - (ECHO_LOSS_1 + ECHO_LOSS_PER_DECADE * Math.log10(Math.max(d, 1)))
                - 2 * this.rainLoss(level, radar, target, d);
    }

    @Override
    public double noiseFloor() {
        return NOISE_FLOOR;
    }

    @Override
    public double broadcastRange(final double txPowerDbm) {
        return Math.pow(10, (txPowerDbm - BROADCAST_LOSS_1 - NOISE_FLOOR) / BROADCAST_LOSS_PER_DECADE);
    }

    @Override
    public double echoRange(final double crossSectionBlocks2) {
        return Math.pow(10, (RADAR_POWER + 10 * Math.log10(Math.max(crossSectionBlocks2, 1e-3))
                - ECHO_LOSS_1 - NOISE_FLOOR) / ECHO_LOSS_PER_DECADE);
    }

    @Override
    public double reflectivity(final Scatterer kind, final double rate) {
        return switch (kind) {
            case NONE -> Double.NEGATIVE_INFINITY;
            case RAIN -> rate > 0 ? 10 * Math.log10(RAIN_Z_A * Math.pow(rate, RAIN_Z_B)) : Double.NEGATIVE_INFINITY;
            case ICE -> rate > 0 ? 10 * Math.log10(RAIN_Z_A * Math.pow(rate, RAIN_Z_B)) + ICE_DB : Double.NEGATIVE_INFINITY;
            case SNOW -> rate > 0 ? 10 * Math.log10(SNOW_Z_A * Math.pow(rate, SNOW_Z_B)) : Double.NEGATIVE_INFINITY;
            case CLOUD -> rate > 0 ? CLOUD_DBZ - 15 * (1 - Math.min(rate, 1)) : Double.NEGATIVE_INFINITY;
            case GROUND -> GROUND_DBZ;
            case SEA -> SEA_DBZ;
        };
    }

    @Override
    public double specificAttenuation(final Scatterer kind, final double rate) {
        return switch (kind) {
            case RAIN -> rate > 0 ? RAIN_K_A * Math.pow(rate, RAIN_K_B) : 0;
            case SNOW, ICE -> rate > 0 ? FROZEN_K * RAIN_K_A * Math.pow(rate, RAIN_K_B) : 0;
            default -> 0;
        };
    }

    @Override
    public double volumeEcho(final double distance, final double dbz) {
        return VOLUME_ECHO_1 + dbz - 20 * Math.log10(Math.max(distance, 1));
    }

    /**
     * Loss over a path of length {@code d}, judged by the precipitation at its midpoint: what the
     * rain gauge would read there, so a weather mod's local rain counts (see
     * {@link Atmosphere#precipitationRate}).
     */
    private double rainLoss(final Level level, final Vec3 a, final Vec3 b, final double d) {
        if (d <= 0 || level.isClientSide()) {
            return 0;
        }
        final Vec3 mid = a.add(b).scale(0.5);
        final int y = Math.clamp((long) Math.floor(mid.y), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        final BlockPos pos = BlockPos.containing(mid.x, y, mid.z);
        if (!level.isLoaded(pos)) {
            return 0;
        }
        final Scatterer kind = switch (Atmosphere.precipitation(level, pos)) {
            case RAIN -> Scatterer.RAIN;
            case SNOW -> Scatterer.SNOW;
            case NONE -> Scatterer.NONE;
        };
        if (kind == Scatterer.NONE) {
            return 0;
        }
        return this.specificAttenuation(kind, Atmosphere.precipitationRate(level, pos).value()) * d;
    }
}
