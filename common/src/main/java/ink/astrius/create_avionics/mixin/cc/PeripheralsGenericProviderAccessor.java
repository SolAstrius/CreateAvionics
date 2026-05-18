package ink.astrius.create_avionics.mixin.cc;

import dan200.computercraft.impl.Peripherals;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the static {@code genericProvider} on CC's {@link Peripherals} so the
 * peripheral-composition mixin can run the full generic-source pass (BE-target
 * methods plus the {@code ComponentLookup} chain that surfaces {@code IItemHandler},
 * fluid, and energy methods) on top of a capability-supplied primary.
 */
@Mixin(value = Peripherals.class, remap = false)
public interface PeripheralsGenericProviderAccessor {

    @Accessor("genericProvider")
    static GenericPeripheralProvider createAvionics$getGenericProvider() {
        throw new AssertionError();
    }
}
