package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import ink.astrius.create_avionics.api.create.GantryShaftExt;
import ink.astrius.create_avionics.compat.create.peripherals.GantryShaftPeripheral;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Attaches a {@code ComputerBehaviour} to selected kinetic block entities
 * that don't override {@code addBehaviours} themselves (so we can't inject
 * into a subclass-specific override). Gates by {@code instanceof} so only
 * the BE types we've peripheraled at this level actually get the behaviour
 * — eager {@code getPeripheralFor} would throw on unknown types.
 */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityComputerHookMixin implements GantryShaftExt {

    @Unique
    private AbstractComputerBehaviour createAvionics$computerBehaviour;

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

    /**
     * Drive the gantry shaft's rail events. Nothing upstream fires on a
     * carriage arriving or stalling, so the peripheral has to watch for the
     * edges itself — but only where it can matter: a shaft with no attached
     * computer has nobody to tell, and a full rail scan is not free.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void createAvionics$tickGantryEvents(final CallbackInfo ci) {
        final AbstractComputerBehaviour behaviour = this.createAvionics$computerBehaviour;
        if (behaviour == null || !behaviour.hasAttachedComputer()) return;

        final Level level = ((SmartBlockEntity) (Object) this).getLevel();
        if (level == null || level.isClientSide) return;

        if (behaviour.getPeripheralCapability() instanceof final GantryShaftPeripheral peripheral) {
            peripheral.tickComputerEvents();
        }
    }

    @Unique
    private boolean createAvionics$shouldAttachPeripheral() {
        return (Object) this instanceof GantryShaftBlockEntity;
    }
}
