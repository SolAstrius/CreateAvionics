package ink.astrius.create_avionics.content.radar;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Radar Mk.1: one narrow beam straight out of the radar's face, swept twice a second. Reports
 * the first thing the beam meets and nothing to either side. It is a primary radar: a contact
 * says how far and how strong the echo is, not who it is. To identify a ship, match the
 * contact against the traffic an avionics controller hears ({@code getTraffic}).
 * <p>
 * The echo falls with the fourth power of distance and grows with the target's cross-section,
 * the area its bounding box shows the beam; rain and snow weaken it. A target is reported while
 * its echo clears the noise floor (-100 dBm), so big ships show up further out than small ones.
 *
 * @cc.module radar
 */
public class RadarPeripheral implements IPeripheral {

    private final RadarBlockEntity blockEntity;

    public RadarPeripheral(final RadarBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "radar";
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other instanceof final RadarPeripheral o && o.blockEntity == this.blockEntity;
    }

    /**
     * Get what the beam is hitting.
     *
     * @return Nil if the beam meets nothing whose echo clears the noise floor. Otherwise a table
     * with {@code kind} ({@code "ship"}, {@code "terrain"}, or {@code "obstructed"} when the radar's
     * own ship blocks its face), {@code distance} in blocks, {@code closingSpeed} in blocks per
     * second (positive when approaching; 0 until the beam has held the same target for two
     * sweeps) and {@code signal}, the echo's strength in dBm.
     */
    @LuaFunction
    public final @Nullable Map<String, Object> getContact() {
        final RadarBlockEntity.Contact c = this.blockEntity.contact();
        if (c == null) {
            return null;
        }
        final Map<String, Object> out = new HashMap<>();
        out.put("kind", c.kind().name().toLowerCase(Locale.ROOT));
        out.put("distance", c.distance());
        out.put("closingSpeed", c.closingSpeed());
        out.put("signal", c.signal());
        return out;
    }

    /**
     * Get how far a typical ship shows up: one with a 10x10 face to the beam, in clear weather.
     * Bigger targets show up further, up to 512 blocks, the farthest the beam is cast; smaller ones
     * and rain bring it in.
     *
     * @return The range in blocks.
     */
    @LuaFunction
    public final double getRange() {
        return RadarBlockEntity.typicalRange();
    }
}
