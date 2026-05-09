package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.IControlContraption.RotationMode;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import ink.astrius.create_avionics.api.create.MechanicalBearingExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adds the kinetic-peripheral surface to Create's mechanical bearing:
 * <ul>
 *   <li>Injects a {@code ComputerBehaviour} into the BE's behaviours list so
 *       the bearing can resolve a peripheral capability at all (Create does
 *       not register one for this BE type).</li>
 *   <li>Exposes the {@code movementMode} scroll-option behaviour to our
 *       peripheral via {@link MechanicalBearingExt}.</li>
 * </ul>
 */
@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class MechanicalBearingBlockEntityMixin implements MechanicalBearingExt {

    @Unique
    private AbstractComputerBehaviour createAvionics$computerBehaviour;

    @Accessor("movementMode")
    public abstract ScrollOptionBehaviour<RotationMode> createAvionics$movementMode_accessor();

    @Override
    public ScrollOptionBehaviour<RotationMode> createAvionics$movementMode() {
        return this.createAvionics$movementMode_accessor();
    }

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
