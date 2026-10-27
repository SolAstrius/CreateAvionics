package ink.astrius.create_avionics.content.radar;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Radar Mk.1: one narrow beam straight out of the radar's face, swept twice a second. Reports
 * the first thing the beam meets and nothing to either side.
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
     * @return Nil if the beam meets nothing within range. Otherwise a table with {@code kind}
     * ({@code "ship"}, {@code "terrain"}, or {@code "obstructed"} when the radar's own ship blocks
     * its face), {@code distance} in blocks, {@code closingSpeed} in blocks per second (positive
     * when approaching; 0 until the beam has held the same target for two sweeps), and for a ship
     * whose transponder is on, its {@code name}.
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
        c.name().ifPresent(name -> out.put("name", name));
        return out;
    }

    /**
     * Get the beam's range.
     *
     * @return The range in blocks.
     */
    @LuaFunction
    public final double getRange() {
        return RadarBlockEntity.RANGE;
    }
}
