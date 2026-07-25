package ink.astrius.create_avionics.compat.create;

import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.create.peripherals.KineticScadaSurface;
import ink.astrius.create_avionics.compat.simulated.peripherals.generic.KineticSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The kinetic SCADA pack is declared twice because CC needs two shapes:
 * {@code @LuaFunction default} methods on {@link KineticScadaSurface} for
 * blocks with a dedicated peripheral, and {@code GenericSource} methods taking
 * the block entity on {@link KineticSource} for bare kinetic blocks.
 *
 * <p>Nothing in the compiler ties the two together, so they can drift — and
 * drift is invisible, because each half compiles fine alone. 0.5.0 renamed
 * {@code getStressCapacity} to {@code getStressContribution} and had to touch
 * both. Scripts see the difference as a method that exists on an encased shaft
 * but not on a gearbox, which is a miserable thing to debug from Lua.</p>
 */
class KineticScadaSurfaceTest {

    private static Set<String> luaMethodNames(final Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(LuaFunction.class))
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    void bothSurfacesExposeTheSameMethodNames() {
        final Set<String> surface = luaMethodNames(KineticScadaSurface.class);
        final Set<String> source = luaMethodNames(KineticSource.class);

        assertFalse(surface.isEmpty(), "KineticScadaSurface declared no @LuaFunction methods");
        assertEquals(
            surface,
            source,
            "kinetic SCADA pack has drifted between KineticScadaSurface (dedicated "
                + "peripherals) and KineticSource (bare kinetic blocks); every method "
                + "must exist on both or scripts see it on some kinetic blocks only");
    }

    @Test
    void generatedSourceTakesTheBlockEntityAsItsOnlyArgument() {
        // KineticSource is a GenericSource: CC dispatches on the first
        // parameter's type. A no-arg method there would silently never bind.
        Arrays.stream(KineticSource.class.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(LuaFunction.class))
            .forEach(m -> assertEquals(
                1,
                m.getParameterCount(),
                m.getName() + " must take exactly the block entity for CC to dispatch on it"));
    }
}
