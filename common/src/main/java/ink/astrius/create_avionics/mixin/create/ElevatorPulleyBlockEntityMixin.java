package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import ink.astrius.create_avionics.api.create.ElevatorPulleyExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Attaches a {@code ComputerBehaviour} to Create's elevator pulley. Needed as
 * a separate mixin from the linear-actuator hook because the elevator's
 * {@code addBehaviours} override doesn't call super.
 */
@Mixin(value = ElevatorPulleyBlockEntity.class, remap = false)
public abstract class ElevatorPulleyBlockEntityMixin implements ElevatorPulleyExt {

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
