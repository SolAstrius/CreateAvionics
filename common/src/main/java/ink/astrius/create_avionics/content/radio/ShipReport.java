package ink.astrius.create_avionics.content.radio;

import org.jetbrains.annotations.Nullable;

/**
 * What a ship's transponder sends. The address never changes (see {@link Identity#address}). The
 * measured fields are null when the ship lacks the instrument for them (see {@link Equipment}).
 * Altitude is the world height in blocks, heading the bow's compass bearing (0 north, clockwise),
 * speeds are in blocks per second.
 */
public record ShipReport(String address, String callsign, String squawk, @Nullable Double altitude,
                         @Nullable Double heading, @Nullable Double speed, @Nullable Double verticalRate) {
}
