package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.IControlContraption.MovementMode;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import ink.astrius.create_avionics.api.create.LinearActuatorExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adds the kinetic-peripheral plumbing to Create's linear actuators (piston,
 * gantry, pulleys). For each subclass we've peripheraled, we attach a
 * {@code ComputerBehaviour} so the BE can resolve a peripheral capability.
 * Subclasses we haven't peripheraled stay untouched — adding the behaviour
 * eagerly invokes {@code getPeripheralFor} which would throw on unknown BE
 * types.
 *
 * <p>The {@code movementMode} accessor and the stored computer-behaviour
 * reference are exposed via {@link LinearActuatorExt}.</p>
 */
@Mixin(value = LinearActuatorBlockEntity.class, remap = false)
public abstract class LinearActuatorBlockEntityMixin implements LinearActuatorExt {

    @Unique
    private AbstractComputerBehaviour createAvionics$computerBehaviour;

    @Accessor("movementMode")
    public abstract ScrollOptionBehaviour<MovementMode> createAvionics$movementMode_accessor();

    @Override
    public ScrollOptionBehaviour<MovementMode> createAvionics$movementMode() {
        return this.createAvionics$movementMode_accessor();
    }

    @Override
    public AbstractComputerBehaviour createAvionics$getComputerBehaviour() {
        return this.createAvionics$computerBehaviour;
    }

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void createAvionics$addComputerBehaviour(final List<BlockEntityBehaviour> behaviours, final CallbackInfo ci) {
        if (this.createAvionics$shouldAttachPeripheral()) {
            this.createAvionics$computerBehaviour = ComputerCraftProxy.behaviour((SmartBlockEntity) (Object) this);
            behaviours.add(this.createAvionics$computerBehaviour);
        }
    }

    @Unique
    private boolean createAvionics$shouldAttachPeripheral() {
        if ((Object) this instanceof MechanicalPistonBlockEntity) return true;
        // Rope pulleys, excluding the target-driven elevator subclass.
        if ((Object) this instanceof PulleyBlockEntity && !((Object) this instanceof ElevatorPulleyBlockEntity)) return true;
        return false;
    }
}
