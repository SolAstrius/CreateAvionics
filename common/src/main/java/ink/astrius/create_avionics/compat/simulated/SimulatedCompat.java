package ink.astrius.create_avionics.compat.simulated;

import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;
import ink.astrius.create_avionics.CreateAvionics;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Bridges the handful of Simulated members whose spelling differs across the
 * versions we support, so one jar runs on all of them.
 *
 * <p>Only members that moved <em>and</em> are reached from plain code belong
 * here. Mixin injection points cannot be bridged this way — a mixin resolves
 * its target by name at apply time, so a moved target is a hard launch failure
 * under {@code defaultRequire: 1}. As of Simulated 1.3.0 no injection point we
 * touch has moved; if one ever does, that version has to be excluded by a
 * version range rather than shimmed.</p>
 *
 * <p>Differences currently bridged:</p>
 * <ul>
 *   <li>{@code LaserPointerBlockEntity#getLaserRange} was renamed to
 *       {@code getRaycastLength} in 1.3.0. Same {@code float()} signature.</li>
 * </ul>
 *
 * <p>Not every cross-version difference needs a handle. {@code
 * SimDataComponents#COMPASS_PLACER_UUID} changed from
 * {@code DataComponentType<String>} to {@code DataComponentType<UUID>}, but
 * generics are erased — that one is handled at its call site by widening the
 * receiving local to {@code Object}.</p>
 */
public final class SimulatedCompat {

    /** Newest spelling first; the first that resolves wins. */
    private static final String[] LASER_RANGE_NAMES = {"getRaycastLength", "getLaserRange"};

    private static final MethodHandle LASER_RANGE = resolveLaserRange();

    private SimulatedCompat() {
    }

    private static MethodHandle resolveLaserRange() {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType signature = MethodType.methodType(float.class);
        for (final String name : LASER_RANGE_NAMES) {
            try {
                return lookup.findVirtual(LaserPointerBlockEntity.class, name, signature);
            } catch (final NoSuchMethodException | IllegalAccessException ignored) {
                // Wrong Simulated version for this spelling; try the next.
            }
        }
        CreateAvionics.LOGGER.error(
            "No laser-pointer range accessor found on this Create: Simulated build "
                + "(tried {}); laser_pointer.getRange() will fail. Please report the "
                + "Simulated version.",
            String.join(", ", LASER_RANGE_NAMES));
        return null;
    }

    /**
     * The laser pointer's configured range, whatever upstream calls it today.
     *
     * @throws IllegalStateException if no known accessor resolved, so the
     *         failure surfaces to the Lua caller instead of reading as 0.
     */
    public static float laserRange(final LaserPointerBlockEntity blockEntity) {
        if (LASER_RANGE == null) {
            throw new IllegalStateException(
                "laser-pointer range is unavailable on this Create: Simulated version");
        }
        try {
            return (float) LASER_RANGE.invokeExact(blockEntity);
        } catch (final RuntimeException | Error e) {
            throw e;
        } catch (final Throwable t) {
            throw new IllegalStateException("failed to read laser-pointer range", t);
        }
    }
}
