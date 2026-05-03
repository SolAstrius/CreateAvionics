package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.compat.computercraft.ComputerCraftPeripherals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ComputerCraftPeripherals.class, remap = false)
public class ComputerCraftPeripheralsMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void createAvionics$cancel(final CallbackInfo ci) {
        ci.cancel();
    }
}
