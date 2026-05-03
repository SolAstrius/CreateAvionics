package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.service.SimPlatformService;
import ink.astrius.create_avionics.api.simulated.LinkedTypewriterExt;
import ink.astrius.create_avionics.compat.AttachedComputerHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LinkedTypewriterBlockEntity.class, remap = false)
public abstract class LinkedTypewriterBlockEntityMixin implements LinkedTypewriterExt {

    @Shadow private LinkedTypewriterEntries entryMap;

    @Unique
    private final AttachedComputerHandler computerHandler = new AttachedComputerHandler();

    @Override
    public AttachedComputerHandler getComputerHandler() {
        return this.computerHandler;
    }

    @Inject(method = "pressKey", at = @At("HEAD"))
    private void createAvionics$queueKeyEvent(final int key, final CallbackInfo ci) {
        if (SimPlatformService.INSTANCE.isLoaded("computercraft")) {
            this.computerHandler.queueEvent("key", key, this.entryMap.getEntry(key).isAlive());
        }
    }

    @Inject(method = "releaseKey", at = @At("HEAD"))
    private void createAvionics$queueKeyUpEvent(final int key, final CallbackInfo ci) {
        if (SimPlatformService.INSTANCE.isLoaded("computercraft")) {
            this.computerHandler.queueEvent("key_up", key);
        }
    }
}
