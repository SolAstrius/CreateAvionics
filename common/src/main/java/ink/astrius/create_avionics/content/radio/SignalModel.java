package ink.astrius.create_avionics.content.radio;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * How radio signals travel and scatter. The radars and the controller's radio work out received
 * power only through this, so a fuller propagation model (terrain, obstruction, antennas) can replace
 * {@link #current} without changing either of them or the Lua API. All powers are in dBm, all
 * positions are world positions (projected out of any sub-level), all calls on the server thread.
 */
public interface SignalModel {

    /** The model in use. */
    SignalModel current = new SimpleSignalModel();

    /** Power received at {@code to} from a transmitter at {@code from} sending {@code txPowerDbm}. */
    double broadcast(Level level, Vec3 from, Vec3 to, double txPowerDbm);

    /**
     * Power of a radar's own echo off a target at {@code target}, whose cross-section facing the beam
     * is {@code crossSectionBlocks2} square blocks.
     */
    double echo(Level level, Vec3 radar, Vec3 target, double crossSectionBlocks2);

    /** The weakest signal a receiver can make out, in dBm. */
    double noiseFloor();

    /**
     * How far a broadcast of {@code txPowerDbm} can possibly be heard, in blocks: an upper bound,
     * so receivers further away can be skipped without working out the signal.
     */
    double broadcastRange(double txPowerDbm);

    /** How far an echo off {@code crossSectionBlocks2} can possibly clear the noise floor, in blocks. */
    double echoRange(double crossSectionBlocks2);

    /**
     * Reflectivity factor of what fills a range bin, dBZ: precipitation by its rate in mm/h (as
     * liquid water), cloud by its cover (0..1), clutter regardless of {@code rate}.
     * {@link Double#NEGATIVE_INFINITY} for {@link Scatterer#NONE}.
     */
    double reflectivity(Scatterer kind, double rate);

    /** One-way loss per block of path through it, dB. */
    double specificAttenuation(Scatterer kind, double rate);

    /**
     * Power a weather radar receives from a volume of reflectivity {@code dbz} at {@code distance}
     * blocks, before attenuation, dBm. A volume target fills more of the beam the further out it
     * is, so this falls with the square of distance, not the fourth power like {@link #echo}.
     */
    double volumeEcho(double distance, double dbz);

    /**
     * Received power from every range bin along one weather radar radial, attenuation included:
     * bin {@code i} spans {@code i..i+1} bin lengths out, holds {@code kind[i]} at {@code rate[i]},
     * and its echo crosses every bin before it twice and half of its own. Bins after clutter are
     * not asked for: the beam has stopped. {@code outDbm[i]} is {@link Double#NEGATIVE_INFINITY}
     * where nothing returns. A fuller model can override this whole, e.g. to spread the beam.
     */
    default void weatherRadial(final double binLength, final Scatterer[] kind, final double[] rate,
                               final double[] outDbm) {
        double loss = 0;
        for (int i = 0; i < kind.length; i++) {
            final double k = this.specificAttenuation(kind[i], rate[i]) * binLength;
            final double z = this.reflectivity(kind[i], rate[i]);
            outDbm[i] = z == Double.NEGATIVE_INFINITY ? Double.NEGATIVE_INFINITY
                    : this.volumeEcho((i + 0.5) * binLength, z) - 2 * (loss + k / 2);
            loss += k;
        }
    }
}
