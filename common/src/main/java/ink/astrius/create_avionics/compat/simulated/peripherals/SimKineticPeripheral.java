package ink.astrius.create_avionics.compat.simulated.peripherals;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import ink.astrius.create_avionics.compat.create.peripherals.KineticScadaSurface;

/**
 * Base class for Avionics-side peripherals that wrap a Create kinetic block.
 * Mirror of {@code KineticPeripheral} on top of {@link SimPeripheral} instead
 * of Create's {@code SyncedPeripheral}; the kinetic SCADA pack is supplied by
 * {@link KineticScadaSurface} so it lives in one place.
 */
public abstract class SimKineticPeripheral<T extends KineticBlockEntity> extends SimPeripheral<T> implements KineticScadaSurface {

    public SimKineticPeripheral(final T blockEntity) {
        super(blockEntity);
    }

    @Override
    public KineticBlockEntity scadaBlockEntity() {
        return this.blockEntity;
    }
}
