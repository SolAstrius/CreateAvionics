package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import ink.astrius.create_avionics.api.create.ElevatorContactExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Attaches a {@code ComputerBehaviour} to Create's elevator contact block
 * entity, so floor buttons become CC peripherals.
 */
@Mixin(value = ElevatorContactBlockEntity.class, remap = false)
public abstract class ElevatorContactBlockEntityMixin implements ElevatorContactExt {

    @Unique
    private AbstractComputerBehaviour createAvionics$computerBehaviour;

    @Override
    public AbstractComputerBehaviour createAvionics$getComputerBehaviour() {
        return this.createAvionics$computerBehaviour;
    }

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void createAvionics$addComputerBehaviour(final List<BlockEntityBehaviour> behaviours, final CallbackInfo ci) {
        this.createAvionics$computerBehaviour = ComputerCraftProxy.behaviour((SmartBlockEntity) (Object) this);
        behaviours.add(this.createAvionics$computerBehaviour);
    }
}
