package ink.astrius.create_avionics.api.simulated;

public interface NavTableExt {

    /**
     * The rate of change of the bearing to the resolved target, in degrees per
     * second, sampled once per server tick. Zero while no target is resolved,
     * and for the first tick after the target changes.
     */
    double getBearingRate();
}
