package ink.astrius.create_avionics.content.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

/**
 * Free-space path loss with constants tuned for Minecraft distances, plus rain. A broadcast falls
 * with the square of distance, an echo with the fourth power (out and back), so radar range is
 * much shorter than radio range and grows only slowly with the target's size.
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
    /** Loss per block of path through rain, dB; snow is half. An echo crosses it twice. */
    static final double RAIN_LOSS_PER_BLOCK = 0.02;

    @Override
    public double broadcast(final Level level, final Vec3 from, final Vec3 to, final double txPowerDbm) {
        final double d = from.distanceTo(to);
        return txPowerDbm - (BROADCAST_LOSS_1 + BROADCAST_LOSS_PER_DECADE * Math.log10(Math.max(d, 1)))
                - rainLoss(level, from, to, d);
    }

    @Override
    public double echo(final Level level, final Vec3 radar, final Vec3 target, final double crossSectionBlocks2) {
        final double d = radar.distanceTo(target);
        return RADAR_POWER + 10 * Math.log10(Math.max(crossSectionBlocks2, 1e-3))
                - (ECHO_LOSS_1 + ECHO_LOSS_PER_DECADE * Math.log10(Math.max(d, 1)))
                - 2 * rainLoss(level, radar, target, d);
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

    /** Loss over a path of length {@code d}, judged by the weather at its midpoint. */
    private static double rainLoss(final Level level, final Vec3 a, final Vec3 b, final double d) {
        if (!level.isRaining() || d <= 0) {
            return 0;
        }
        final Vec3 mid = a.add(b).scale(0.5);
        final int y = Math.clamp((long) Math.floor(mid.y), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        final BlockPos pos = BlockPos.containing(mid.x, y, mid.z);
        if (!level.isLoaded(pos)) {
            return 0;
        }
        if (level.isRainingAt(pos)) {
            return RAIN_LOSS_PER_BLOCK * d;
        }
        // isRainingAt is false for snow; snow still attenuates, less
        if (level.canSeeSky(pos) && level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW) {
            return RAIN_LOSS_PER_BLOCK / 2 * d;
        }
        return 0;
    }
}
