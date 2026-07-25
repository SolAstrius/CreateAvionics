package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.compat.computercraft.ComputerCraftPeripherals;
import ink.astrius.create_avionics.CreateAvionics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses Simulated's own ComputerCraft registration wholesale;
 * {@code SimulatedCCIntegration} re-registers a superset through the same
 * {@code SimModCompatibilityService} SPI.
 *
 * <p>The cancel is all-or-nothing: anything upstream adds to {@code init()} in
 * a future version is dropped silently. The log line below is the only
 * evidence of that in a user's log, so keep it at INFO — it pairs with
 * {@code SimulatedCCIntegration}'s own registration line, and the two
 * together are what make a bug report legible.</p>
 */
@Mixin(value = ComputerCraftPeripherals.class, remap = false)
public class ComputerCraftPeripheralsMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void createAvionics$cancel(final CallbackInfo ci) {
        CreateAvionics.LOGGER.info(
            "Suppressed Create: Simulated's ComputerCraftPeripherals.init(); "
                + "Avionics registers the Simulated peripheral set instead");
        ci.cancel();
    }
}
