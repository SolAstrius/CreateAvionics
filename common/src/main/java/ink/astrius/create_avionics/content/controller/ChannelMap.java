package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.peripheral.IPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.AltitudeSensorPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.GimbalSensorPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.NavTablePeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.VelocitySensorPeripheral;
import ink.astrius.create_avionics.content.instrument.peripheral.InstrumentPeripheral;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.DoubleSupplier;

/**
 * Turns the peripherals found on a ship into channels. Each channel can have several sources
 * (two altitude sensors, say); the first one that answers wins, so a destroyed sensor is
 * covered by its twin. Values come from the sensors' own peripheral methods, so a channel
 * always agrees with what that sensor reports to a computer.
 * <p>
 * Directions are relative to the controller's facing, which marks the ship's nose.
 */
public final class ChannelMap {

    private final Map<Channel, List<DoubleSupplier>> sources = new EnumMap<>(Channel.class);

    public ChannelMap(final List<ShipScan.Found> found, final Direction forward) {
        final Direction right = forward.getClockWise();
        for (final ShipScan.Found f : found) {
            final IPeripheral p = f.peripheral();
            if (p instanceof final AltitudeSensorPeripheral altitude) {
                this.add(Channel.ALTITUDE, altitude::getHeight);
                this.add(Channel.VERTICAL_SPEED, altitude::getVerticalSpeed);
            } else if (p instanceof final GimbalSensorPeripheral gimbal) {
                this.add(Channel.PITCH, () -> attitude(gimbal.getGravity(), forward, right)[0]);
                this.add(Channel.ROLL, () -> attitude(gimbal.getGravity(), forward, right)[1]);
            } else if (p instanceof final NavTablePeripheral nav) {
                // The table measures the ship's +Z axis; turn that to the controller's facing
                final double offset = Math.toDegrees(Math.atan2(forward.getStepX(), forward.getStepZ()));
                this.add(Channel.HEADING, () -> wrap(nav.getHeading() + offset));
            } else if (p instanceof final VelocitySensorPeripheral velocity) {
                final Direction.Axis axis = Direction.Axis.byName(velocity.getAxis());
                if (axis == Direction.Axis.Y) {
                    this.add(Channel.AIRSPEED_VERTICAL, velocity::getVelocity);
                } else if (axis == forward.getAxis()) {
                    final int sign = forward.getAxisDirection().getStep();
                    this.add(Channel.AIRSPEED_FORWARD, () -> sign * velocity.getVelocity());
                } else if (axis != null) {
                    final int sign = right.getAxisDirection().getStep();
                    this.add(Channel.AIRSPEED_LATERAL, () -> sign * velocity.getVelocity());
                }
            } else if (p instanceof final InstrumentPeripheral instrument) {
                final Channel channel = switch (instrument.getType()) {
                    case "thermometer" -> Channel.AIR_TEMPERATURE;
                    case "hygrometer" -> Channel.HUMIDITY;
                    case "barometer" -> Channel.AIR_PRESSURE;
                    case "rain_gauge" -> Channel.PRECIPITATION;
                    default -> null;
                };
                if (channel != null) {
                    this.add(channel, instrument::getValue);
                }
            }
        }
    }

    private void add(final Channel channel, final DoubleSupplier source) {
        this.sources.computeIfAbsent(channel, c -> new ArrayList<>()).add(source);
    }

    public boolean has(final Channel channel) {
        return this.sources.containsKey(channel);
    }

    public List<Channel> channels() {
        return List.copyOf(this.sources.keySet());
    }

    /** The channel's value from its first working source, or empty if none answers. */
    public OptionalDouble read(final Channel channel) {
        for (final DoubleSupplier source : this.sources.getOrDefault(channel, List.of())) {
            try {
                final double value = source.getAsDouble();
                if (Double.isFinite(value)) {
                    return OptionalDouble.of(value);
                }
            } catch (final RuntimeException e) {
                // The sensor went away between scans; try the next one
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * Pitch (nose up positive) and roll (right wing down positive) in degrees, from the body-frame
     * gravity vector and the controller's facing.
     */
    static double[] attitude(final List<Double> gravity, final Direction forward, final Direction right) {
        final double gx = gravity.get(0), gy = gravity.get(1), gz = gravity.get(2);
        final double gForward = gx * forward.getStepX() + gz * forward.getStepZ();
        final double gRight = gx * right.getStepX() + gz * right.getStepZ();
        final double down = -gy;
        return new double[]{
                Math.toDegrees(Math.atan2(-gForward, down)),
                Math.toDegrees(Math.atan2(gRight, down))
        };
    }

    private static double wrap(final double degrees) {
        final double d = degrees % 360;
        return d < -180 ? d + 360 : d >= 180 ? d - 360 : d;
    }
}
