package ink.astrius.create_avionics.content.instrument.peripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import ink.astrius.create_avionics.content.instrument.InstrumentBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Shared surface of every instrument: one quantity, in fixed units, sampled once a second.
 * <p>
 * Readings are cached on the block, so these calls are cheap and safe to poll. Every reading is
 * taken at the instrument's world position: on an airship it is the air outside the airship.
 * <p>
 * Units by peripheral type: {@code thermometer} degrees Celsius, {@code hygrometer} relative humidity
 * 0..1, {@code barometer} hectopascals, {@code rain_gauge} millimetres per hour,
 * {@code almanac} fraction of the year 0..1 (or of the lunar cycle without seasons).
 *
 * @cc.module instrument
 */
public class InstrumentPeripheral implements IPeripheral {

    protected final InstrumentBlockEntity blockEntity;

    public InstrumentPeripheral(final InstrumentBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public static InstrumentPeripheral of(final InstrumentBlockEntity blockEntity) {
        return switch (blockEntity.instrument()) {
            case RAIN_GAUGE -> new RainGaugePeripheral(blockEntity);
            case ALMANAC -> new AlmanacPeripheral(blockEntity);
            default -> new InstrumentPeripheral(blockEntity);
        };
    }

    @Override
    public String getType() {
        return this.blockEntity.instrument().id();
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other instanceof final InstrumentPeripheral o && o.blockEntity == this.blockEntity;
    }

    /**
     * Get the latest reading.
     *
     * @return The measured value, in this instrument's unit.
     */
    @LuaFunction
    public final double getValue() {
        return this.blockEntity.value();
    }

    /**
     * Get the unit readings are in.
     *
     * @return {@code "C"}, {@code "rh"}, {@code "hPa"}, {@code "mm/h"}, or for the almanac
     * {@code "year"} (fraction of the season year) or {@code "moon"} (fraction of the lunar cycle,
     * where there is no season calendar).
     */
    @LuaFunction
    public final String getUnit() {
        return this.blockEntity.unit();
    }

    /**
     * Get the latest reading with its context.
     *
     * @return A table with {@code value}, {@code unit}, {@code time} (game time of the sample, in
     * ticks) and {@code source} (where the value came from, e.g. {@code "thermoo"}, {@code "sable"},
     * {@code "serene_seasons"} or {@code "vanilla"}).
     */
    @LuaFunction
    public final Map<String, Object> read() {
        return Map.of(
                "value", this.blockEntity.value(),
                "unit", this.blockEntity.unit(),
                "time", this.blockEntity.sampledAt(),
                "source", this.blockEntity.source()
        );
    }
}
