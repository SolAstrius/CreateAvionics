package ink.astrius.create_avionics.content.radio;

/**
 * What a ship carries that its transponder can report from. A report only has the fields its
 * ship can measure: altitude from an altitude sensor (or, coarser, a barometer), vertical rate from
 * an altitude sensor, heading from a navigation table holding a compass, speed from a velocity
 * sensor on the ship's forward axis.
 */
public record Equipment(boolean altitudeSensor, boolean barometer, boolean compass, boolean forwardSpeed) {

    public static final Equipment NONE = new Equipment(false, false, false, false);
}
