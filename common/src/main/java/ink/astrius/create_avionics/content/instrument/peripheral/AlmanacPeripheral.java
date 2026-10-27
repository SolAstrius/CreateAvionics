package ink.astrius.create_avionics.content.instrument.peripheral;

import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.content.instrument.InstrumentBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Reports where in the year it is. With Serene Seasons installed this is the season calendar; in a
 * dimension or biome without seasons, or without the mod, the season functions return nil and
 * {@link #getValue} follows the eight-day lunar cycle instead.
 *
 * @cc.module almanac
 */
public class AlmanacPeripheral extends InstrumentPeripheral {

    public AlmanacPeripheral(final InstrumentBlockEntity blockEntity) {
        super(blockEntity);
    }

    /**
     * Get the current season.
     *
     * @return {@code "spring"}, {@code "summer"}, {@code "autumn"} or {@code "winter"}, or nil
     * without a season calendar.
     */
    @LuaFunction
    public final @Nullable String getSeason() {
        return this.blockEntity.date().season().orElse(null);
    }

    /**
     * Get the current part of the season.
     *
     * @return For example {@code "early_spring"}, {@code "mid_summer"} or {@code "late_winter"}, or
     * nil without a season calendar.
     */
    @LuaFunction
    public final @Nullable String getSubSeason() {
        return this.blockEntity.date().subSeason().orElse(null);
    }

    /**
     * Get the day of the year.
     *
     * @return The day, starting at 1 on the first day of spring, or nil without a season calendar.
     */
    @LuaFunction
    public final @Nullable Integer getDay() {
        return this.blockEntity.date().day().orElse(null);
    }

    /**
     * Get the moon phase.
     *
     * @return The vanilla moon phase, 0 (full moon) to 7.
     */
    @LuaFunction
    public final int getMoonPhase() {
        return this.blockEntity.date().moonPhase();
    }
}
