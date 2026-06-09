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
    private static final Set<Supplier<? extends BlockEntityType<?>>> PENDING = Collections.synchronizedSet(new HashSet<>());

    private PeripheralComposition() {
    }

    public static void register(final BlockEntityType<?> type) {
        TYPES.add(type);
    }

    /**
     * Registration runs from mod constructors (Simulated's SPI init), before
     * block-entity registries are bound — resolving the supplier here would
     * throw "Trying to access unbound value". Park it and resolve on first
     * lookup, which can only happen once a level exists.
     */
    public static void register(final Supplier<? extends BlockEntityType<?>> type) {
        PENDING.add(type);
    }

    public static boolean isRegistered(final BlockEntityType<?> type) {
        if (!PENDING.isEmpty()) {
            synchronized (PENDING) {
                PENDING.forEach(s -> TYPES.add(s.get()));
                PENDING.clear();
            }
        }
        return TYPES.contains(type);
    }
}
