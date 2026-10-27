package ink.astrius.create_avionics.content.radio;

/**
 * The radio's server settings. Copied in by the loader whenever the server config loads or
 * reloads; {@code common} can't read the loader's config itself.
 */
public final class RadioConfig {

    /** Ticks between broadcasts; each transmitting ship sends one report per interval. */
    public static volatile int broadcastInterval = 10;
    /** Tracks one receiver keeps at most. */
    public static volatile int maxTracks = 64;
    /** Seconds without a message before a track is dropped. */
    public static volatile int trackExpirySeconds = 30;
    /** Raw messages one receiver keeps for {@code getMessages}. */
    public static volatile int messageBufferSize = 256;

    private RadioConfig() {
    }
}
