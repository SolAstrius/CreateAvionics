package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/**
 * Base class for Avionics peripherals (Create-side) that wrap a Create kinetic
 * block. The kinetic SCADA pack is supplied by {@link KineticScadaSurface};
 * subclasses only add their block-specific surface.
 */
public abstract class KineticPeripheral<T extends KineticBlockEntity> extends SyncedPeripheral<T> implements KineticScadaSurface {

    public KineticPeripheral(final T blockEntity) {
        super(blockEntity);
    }

    @Override
    public KineticBlockEntity scadaBlockEntity() {
        return this.blockEntity;
    }
}
