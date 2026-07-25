package ink.astrius.create_avionics.compat.create;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContactBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Which Avionics peripheral replaces Create's for a given block entity.
 *
 * <p>Read by {@code ComputerBehaviourMixin}, which does nothing but consult
 * this table. The table lives outside the mixin deliberately: a mixin has to
 * be re-verified against every upstream release and cannot be unit tested, so
 * it should adapt rather than decide.</p>
 *
 * <p><strong>Order is significant.</strong> Matching is by {@code isInstance},
 * so where two entries are related by inheritance the more specific one must
 * come first or it can never be reached — {@code ElevatorPulleyBlockEntity}
 * ahead of {@code PulleyBlockEntity} is the live example.
 * {@code CreatePeripheralRegistryTest} enforces this.</p>
 */
public final class CreatePeripheralRegistry {

    private CreatePeripheralRegistry() {
    }

    private record Entry<T extends SmartBlockEntity>(
            Class<T> type,
            Function<T, SyncedPeripheral<?>> factory) {

        boolean matches(final SmartBlockEntity be) {
            return this.type.isInstance(be);
        }

        Supplier<SyncedPeripheral<?>> bind(final SmartBlockEntity be) {
            final T typed = this.type.cast(be);
            return () -> this.factory.apply(typed);
        }
    }

    private static <T extends SmartBlockEntity> Entry<T> entry(
            final Class<T> type,
            final Function<T, SyncedPeripheral<?>> factory) {
        return new Entry<>(type, factory);
    }

    /** Most specific first; see the ordering note in the class javadoc. */
    private static final List<Entry<?>> ENTRIES = List.of(
        entry(SequencedGearshiftBlockEntity.class, SequencedGearshiftPeripheral::new),
        entry(SpeedGaugeBlockEntity.class, SpeedGaugePeripheral::new),
        entry(StressGaugeBlockEntity.class, StressGaugePeripheral::new),
        entry(SpeedControllerBlockEntity.class, be -> new SpeedControllerPeripheral(be, be.targetSpeed)),
        entry(CreativeMotorBlockEntity.class, be -> new CreativeMotorPeripheral(be, be.generatedSpeed)),
        entry(MechanicalBearingBlockEntity.class, MechanicalBearingPeripheral::new),
        entry(MechanicalPistonBlockEntity.class, MechanicalPistonPeripheral::new),
        entry(GantryShaftBlockEntity.class, GantryShaftPeripheral::new),
        // ElevatorPulleyBlockEntity extends PulleyBlockEntity — it MUST stay
        // ahead of it or elevator pulleys silently get the rope-pulley
        // peripheral. Verified by CreatePeripheralRegistryTest.
        entry(ElevatorPulleyBlockEntity.class, ElevatorPulleyPeripheral::new),
        entry(PulleyBlockEntity.class, RopePulleyPeripheral::new),
        entry(ElevatorContactBlockEntity.class, ElevatorContactPeripheral::new)
    );

    /**
     * The Avionics peripheral supplier for this block entity, or {@code null}
     * to let Create's own lookup stand.
     */
    public static Supplier<SyncedPeripheral<?>> peripheralFor(final SmartBlockEntity be) {
        for (final Entry<?> candidate : ENTRIES) {
            if (candidate.matches(be)) {
                return candidate.bind(be);
            }
        }
        return null;
    }

    /** Registered types in match order. For tests. */
    public static List<Class<? extends SmartBlockEntity>> registeredTypes() {
        final List<Class<? extends SmartBlockEntity>> types = new ArrayList<>(ENTRIES.size());
        for (final Entry<?> candidate : ENTRIES) {
            types.add(candidate.type());
        }
        return List.copyOf(types);
    }
}
