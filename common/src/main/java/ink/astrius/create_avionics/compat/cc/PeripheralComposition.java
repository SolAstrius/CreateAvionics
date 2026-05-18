package ink.astrius.create_avionics.compat.cc;

import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Opt-in registry of block-entity types whose capability-supplied peripheral
 * should be augmented with CC's generic-source methods. Without this, CC
 * short-circuits its generic-source pass for any BE that already exposes an
 * {@code IPeripheral} via capability — leaving e.g. Create's pre-peripheraled
 * kinetic BEs without the SCADA pack {@code KineticSource} would otherwise
 * provide.
 *
 * <p>The composition is performed by {@code PeripheralAccessMixin}: primary
 * methods win on name collision, primary's type/getTarget are preserved,
 * additional types are unioned.</p>
 */
public final class PeripheralComposition {

    private static final Set<BlockEntityType<?>> TYPES = Collections.synchronizedSet(new HashSet<>());

    private PeripheralComposition() {
    }

    public static void register(final BlockEntityType<?> type) {
        TYPES.add(type);
    }

    public static void register(final Supplier<? extends BlockEntityType<?>> type) {
        TYPES.add(type.get());
    }

    public static boolean isRegistered(final BlockEntityType<?> type) {
        return TYPES.contains(type);
    }
}
