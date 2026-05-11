package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.compat.computercraft.implementation.peripherals.*;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import ink.astrius.create_avionics.compat.create.peripherals.CreativeMotorPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.ElevatorContactPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.ElevatorPulleyPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.GantryShaftPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.MechanicalBearingPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.MechanicalPistonPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.RopePulleyPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.SequencedGearshiftPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.SpeedControllerPeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.SpeedGaugePeripheral;
import ink.astrius.create_avionics.compat.create.peripherals.StressGaugePeripheral;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Redirects Create's peripheral lookup to our richer SCADA wrapper for every
 * kinetic block we override. Keeps Create's peripheral type names so existing
 * scripts that {@code peripheral.find("Create_*")} still work.
 */
@Mixin(value = ComputerBehaviour.class, remap = false)
public abstract class ComputerBehaviourMixin {

    @Inject(method = "getPeripheralFor", at = @At("HEAD"), cancellable = true)
    private static void createAvionics$override(
            final SmartBlockEntity be,
            final CallbackInfoReturnable<Supplier<SyncedPeripheral<?>>> cir) {
        if (be instanceof final SequencedGearshiftBlockEntity sgbe) {
            cir.setReturnValue(() -> new SequencedGearshiftPeripheral(sgbe));
        } else if (be instanceof final SpeedGaugeBlockEntity spgbe) {
            cir.setReturnValue(() -> new SpeedGaugePeripheral(spgbe));
        } else if (be instanceof final StressGaugeBlockEntity stgbe) {
            cir.setReturnValue(() -> new StressGaugePeripheral(stgbe));
        } else if (be instanceof final SpeedControllerBlockEntity scbe) {
            cir.setReturnValue(() -> new SpeedControllerPeripheral(scbe, scbe.targetSpeed));
        } else if (be instanceof final CreativeMotorBlockEntity cmbe) {
            cir.setReturnValue(() -> new CreativeMotorPeripheral(cmbe, cmbe.generatedSpeed));
        } else if (be instanceof final MechanicalBearingBlockEntity mbbe) {
            cir.setReturnValue(() -> new MechanicalBearingPeripheral(mbbe));
        } else if (be instanceof final MechanicalPistonBlockEntity mpbe) {
            cir.setReturnValue(() -> new MechanicalPistonPeripheral(mpbe));
        } else if (be instanceof final GantryShaftBlockEntity gsbe) {
            cir.setReturnValue(() -> new GantryShaftPeripheral(gsbe));
        } else if (be instanceof final ElevatorPulleyBlockEntity epbe) {
            cir.setReturnValue(() -> new ElevatorPulleyPeripheral(epbe));
        } else if (be instanceof final PulleyBlockEntity pbe) {
            cir.setReturnValue(() -> new RopePulleyPeripheral(pbe));
        } else if (be instanceof final ElevatorContactBlockEntity ecbe) {
            cir.setReturnValue(() -> new ElevatorContactPeripheral(ecbe));
        }
    }
}
