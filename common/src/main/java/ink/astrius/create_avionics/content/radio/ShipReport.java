package ink.astrius.create_avionics.content.radio;

/**
 * What a ship's transponder sends. The address never changes (see {@link Identity#address}). Positions are world positions of the transmitting controller,
 * heading is the bow's compass bearing (0 north, clockwise), speeds are in blocks per second.
 */
public record ShipReport(String address, String callsign, String squawk, double x, double y, double z,
                         double heading, double speed, double verticalRate) {
}
