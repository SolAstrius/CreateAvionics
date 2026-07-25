package ink.astrius.create_avionics.compat.create;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CreatePeripheralRegistry} matches by {@code isInstance} and returns
 * the first hit, so a supertype listed ahead of one of its subtypes makes the
 * subtype unreachable — the block silently gets the wrong peripheral rather
 * than failing loudly. {@code ElevatorPulleyBlockEntity} sitting ahead of
 * {@code PulleyBlockEntity} is the live case.
 */
class CreatePeripheralRegistryTest {

    @Test
    void noEntryShadowsALaterOne() {
        final List<Class<? extends SmartBlockEntity>> types = CreatePeripheralRegistry.registeredTypes();

        for (int earlier = 0; earlier < types.size(); earlier++) {
            for (int later = earlier + 1; later < types.size(); later++) {
                final Class<?> before = types.get(earlier);
                final Class<?> after = types.get(later);
                assertFalse(
                    before.isAssignableFrom(after),
                    before.getSimpleName() + " is matched before its subtype "
                        + after.getSimpleName() + ", so " + after.getSimpleName()
                        + " can never be reached — move the more specific entry first");
            }
        }
    }

    @Test
    void noTypeIsRegisteredTwice() {
        final List<Class<? extends SmartBlockEntity>> types = CreatePeripheralRegistry.registeredTypes();
        final Set<Class<?>> seen = new HashSet<>();
        for (final Class<?> type : types) {
            assertTrue(seen.add(type), type.getSimpleName() + " is registered more than once");
        }
    }

    @Test
    void registryIsNotEmpty() {
        assertFalse(
            CreatePeripheralRegistry.registeredTypes().isEmpty(),
            "registry is empty — ComputerBehaviourMixin would override nothing");
    }
}
