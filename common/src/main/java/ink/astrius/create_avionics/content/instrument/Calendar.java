package ink.astrius.create_avionics.content.instrument;

import ink.astrius.create_avionics.compat.sereneseasons.SeasonsCalendar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Where in the year we are. Serene Seasons supplies a real calendar; without it the almanac's hand
 * follows the moon, the only cycle longer than a day that vanilla has.
 */
public final class Calendar {

    private static final long LUNAR_CYCLE = 8 * 24000L;

    private Calendar() {
    }

    /**
     * @param progress   0..1 through the year, starting at the beginning of spring
     * @param season     "spring", "summer", "autumn" or "winter"; empty without a calendar
     * @param subSeason  e.g. "early_spring"; empty without a calendar
     * @param day        day of the year, starting at 1; empty without a calendar
     * @param moonPhase  vanilla moon phase 0..7
     */
    public record Date(double progress, Optional<String> season, Optional<String> subSeason,
                       Optional<Integer> day, int moonPhase, String source) {
    }

    public static Date at(final Level level, final BlockPos world) {
        if (InstrumentSources.sereneSeasons) {
            final Optional<Date> date = SeasonsCalendar.at(level, world);
            if (date.isPresent()) {
                return date.get();
            }
        }
        final double lunar = (double) Math.floorMod(level.getDayTime(), LUNAR_CYCLE) / LUNAR_CYCLE;
        return new Date(lunar, Optional.empty(), Optional.empty(), Optional.empty(), level.getMoonPhase(), "vanilla");
    }
}
