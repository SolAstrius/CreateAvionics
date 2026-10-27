package ink.astrius.create_avionics.content.radio;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * How radio signals travel. The radar and the controller's radio work out received power only
 * through this, so a fuller propagation model (terrain, obstruction, antennas) can replace
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
}
