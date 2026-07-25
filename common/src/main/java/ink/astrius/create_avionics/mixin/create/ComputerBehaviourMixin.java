package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import ink.astrius.create_avionics.compat.create.CreatePeripheralRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Redirects Create's peripheral lookup to our richer SCADA wrapper for every
 * kinetic block we override. Keeps Create's peripheral type names so existing
 * scripts that {@code peripheral.find("Create_*")} still work.
 *
 * <p>Which block entity maps to which peripheral is {@link
 * CreatePeripheralRegistry}'s business, not this mixin's — see that class for
 * the table and its ordering rule.</p>
 */
@Mixin(value = ComputerBehaviour.class, remap = false)
public abstract class ComputerBehaviourMixin {

    @Inject(method = "getPeripheralFor", at = @At("HEAD"), cancellable = true)
    private static void createAvionics$override(
            final SmartBlockEntity be,
            final CallbackInfoReturnable<Supplier<SyncedPeripheral<?>>> cir) {
        final Supplier<SyncedPeripheral<?>> override = CreatePeripheralRegistry.peripheralFor(be);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
