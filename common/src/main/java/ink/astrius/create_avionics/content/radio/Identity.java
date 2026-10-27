package ink.astrius.create_avionics.content.radio;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Callsigns and squawk codes: what's allowed, and the defaults. */
public final class Identity {

    public static final String DEFAULT_SQUAWK = "1200";
    public static final int MAX_CALLSIGN = 12;
    private static final Pattern CALLSIGN = Pattern.compile("[A-Za-z0-9-]{1," + MAX_CALLSIGN + "}");
    private static final Pattern SQUAWK = Pattern.compile("[0-7]{4}");

    private Identity() {
    }

    /** A ship's callsign until one is set: its Sable name, or a six-character code from its id. */
    public static String defaultCallsign(final UUID ship, final String name) {
        return name != null && !name.isBlank() ? name : ship.toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    /** The callsign trimmed, or an exception saying what is wrong with it. */
    public static String callsign(final String raw) {
        final String s = raw.trim();
        if (!CALLSIGN.matcher(s).matches()) {
            throw new IllegalArgumentException("callsign must be 1 to " + MAX_CALLSIGN
                    + " letters, digits or '-'");
        }
        return s;
    }

    /**
     * A squawk code as four octal digits. Numbers are padded, since Lua drops leading zeros:
     * {@code 7} is {@code "0007"}.
     */
    public static String squawk(final Object raw) {
        final String s;
        if (raw instanceof final Number n) {
            final double d = n.doubleValue();
            if (d != Math.rint(d) || d < 0 || d > 7777) {
                throw new IllegalArgumentException("squawk must be four digits 0-7");
            }
            s = String.format(Locale.ROOT, "%04d", (int) d);
        } else if (raw instanceof final String str) {
            s = str.trim();
        } else {
            throw new IllegalArgumentException("squawk must be a number or a string");
        }
        if (!SQUAWK.matcher(s).matches()) {
            throw new IllegalArgumentException("squawk must be four digits 0-7");
        }
        return s;
    }
}
