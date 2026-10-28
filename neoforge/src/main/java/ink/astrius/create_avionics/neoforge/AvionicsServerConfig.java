package ink.astrius.create_avionics.neoforge;

import ink.astrius.create_avionics.content.radar.WeatherField;
import ink.astrius.create_avionics.content.radio.RadioConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code create_avionics-server.toml}, per world. */
public final class AvionicsServerConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue BROADCAST_INTERVAL;
    public static final ModConfigSpec.IntValue MAX_TRACKS;
    public static final ModConfigSpec.IntValue TRACK_EXPIRY;
    public static final ModConfigSpec.IntValue MESSAGE_BUFFER;
    public static final ModConfigSpec.IntValue WEATHER_COLUMNS;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("radio");
        BROADCAST_INTERVAL = builder
                .comment("Ticks between a ship's broadcasts. Lower is smoother traffic and more work for the server.")
                .defineInRange("broadcastIntervalTicks", 10, 1, 200);
        MAX_TRACKS = builder
                .comment("Ships one controller tracks at most. When full, the stalest track goes first, then the weakest.")
                .defineInRange("maxTracks", 64, 1, 1024);
        TRACK_EXPIRY = builder
                .comment("Seconds without a message before a controller drops a ship's track.")
                .defineInRange("trackExpirySeconds", 30, 1, 3600);
        MESSAGE_BUFFER = builder
                .comment("Raw messages one controller keeps for getMessages.")
                .defineInRange("messageBufferSize", 256, 0, 4096);
        builder.pop();
        builder.push("weather_radar");
        WEATHER_COLUMNS = builder
                .comment("Weather columns sampled per level per tick for the weather radars, shared by all of them.",
                        "More radars than this keeps fresh see older weather; it never costs more.")
                .defineInRange("columnsPerTick", 32, 1, 1024);
        builder.pop();
        SPEC = builder.build();
    }

    private AvionicsServerConfig() {
    }

    /** Copy the values to {@code common}, on load and on every reload. */
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        RadioConfig.broadcastInterval = BROADCAST_INTERVAL.get();
        RadioConfig.maxTracks = MAX_TRACKS.get();
        RadioConfig.trackExpirySeconds = TRACK_EXPIRY.get();
        RadioConfig.messageBufferSize = MESSAGE_BUFFER.get();
        WeatherField.columnsPerTick = WEATHER_COLUMNS.get();
    }
}
