package ink.astrius.create_avionics.content.controller;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * A quantity a flight gauge can show, if the ship has the sensor for it. Combined from the
 * sensors by the controller, since a gauge can't do that maths itself.
 */
public enum Channel implements StringRepresentable {
    /** World height of the altitude sensor, in blocks. */
    ALTITUDE("m"),
    /** Climb rate from the altitude sensor, m/s, positive up. */
    VERTICAL_SPEED("m/s"),
    /** Nose up/down from the gimbal sensor, degrees, relative to the controller's facing. */
    PITCH("deg"),
    /** Bank from the gimbal sensor, degrees, relative to the controller's facing. */
    ROLL("deg"),
    /** Where the nose points, from the navigation table, degrees (0 = south, like player yaw). */
    HEADING("deg"),
    /** Speed along the controller's facing, from a velocity sensor on that axis, m/s. */
    AIRSPEED_FORWARD("m/s"),
    /** Speed along the vertical axis, from a vertical velocity sensor, m/s. */
    AIRSPEED_VERTICAL("m/s"),
    /** Speed to the right of the controller's facing, from a lateral velocity sensor, m/s. */
    AIRSPEED_LATERAL("m/s"),
    /** From a thermometer, degrees Celsius. */
    AIR_TEMPERATURE("C"),
    /** From a hygrometer, 0..1. */
    HUMIDITY("rh"),
    /** From a barometer, hectopascals. */
    AIR_PRESSURE("hPa"),
    /** From a rain gauge, mm/h. */
    PRECIPITATION("mm/h");

    private final String unit;

    Channel(final String unit) {
        this.unit = unit;
    }

    public String unit() {
        return this.unit;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
