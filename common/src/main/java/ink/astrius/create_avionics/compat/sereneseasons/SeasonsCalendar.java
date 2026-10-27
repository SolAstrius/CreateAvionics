package ink.astrius.create_avionics.compat.sereneseasons;

import ink.astrius.create_avionics.content.instrument.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;

import java.util.Locale;
import java.util.Optional;

/**
 * Serene Seasons' calendar. Only referenced when Serene Seasons is loaded. Seasons are read in the
 * Overworld only (Serene Seasons' default; its dimension whitelist sits behind a config library that
 * isn't on our classpath), and tropical biomes (wet/dry instead of four seasons) fall back to the
 * vanilla calendar too.
 */
public final class SeasonsCalendar {

    private SeasonsCalendar() {
    }

    public static Optional<Calendar.Date> at(final Level level, final BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD
                || SeasonHelper.usesTropicalSeasons(level.getBiome(pos))) {
            return Optional.empty();
        }
        final ISeasonState state = SeasonHelper.getSeasonState(level);
        final double progress = (double) state.getSeasonCycleTicks() / state.getCycleDuration();
        return Optional.of(new Calendar.Date(
                progress,
                Optional.of(state.getSeason().name().toLowerCase(Locale.ROOT)),
                Optional.of(state.getSubSeason().name().toLowerCase(Locale.ROOT)),
                Optional.of(state.getDay() + 1),
                level.getMoonPhase(),
                "serene_seasons"
        ));
    }
}
