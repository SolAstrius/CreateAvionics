package ink.astrius.create_avionics.content.instrument.peripheral;

import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.content.instrument.InstrumentBlockEntity;

import java.util.Locale;

/**
 * Measures precipitation falling on the gauge: the current rate, and the total collected since the
 * gauge was placed or last emptied. Snow counts as its melted water.
 *
 * @cc.module rain_gauge
 */
public class RainGaugePeripheral extends InstrumentPeripheral {

    public RainGaugePeripheral(final InstrumentBlockEntity blockEntity) {
        super(blockEntity);
    }

    /**
     * Get what is falling on the gauge right now.
     *
     * @return {@code "none"}, {@code "rain"} or {@code "snow"}.
     */
    @LuaFunction
    public final String getPrecipitation() {
        return this.blockEntity.precipitation().name().toLowerCase(Locale.ROOT);
    }

    /**
     * Get the water collected since the gauge was placed or last emptied.
     *
     * @return The total in millimetres.
     */
    @LuaFunction
    public final double getTotal() {
        return this.blockEntity.totalMm();
    }

    /**
     * Empty the gauge, resetting the collected total to zero.
     */
    @LuaFunction(mainThread = true)
    public final void empty() {
        this.blockEntity.resetTotal();
    }
}
