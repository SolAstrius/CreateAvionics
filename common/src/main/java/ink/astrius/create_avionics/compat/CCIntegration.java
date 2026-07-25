package ink.astrius.create_avionics.compat;

import dan200.computercraft.api.peripheral.IPeripheral;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Shared base for the three {@code SimModCompatibilityService} implementations
 * Avionics registers through Simulated's SPI (see
 * {@code META-INF/services/dev.simulated_team.simulated.service.SimModCompatibilityService}).
 *
 * <p>Exists only to hold {@link #add}, which was previously copied verbatim
 * into each integration. Extends {@code SimModCompatibilityService} rather
 * than replacing it, so the SPI file keeps naming the concrete classes and
 * upstream discovery is unaffected.</p>
 */
public interface CCIntegration extends SimModCompatibilityService {

    /**
     * Register one block-entity type's peripheral supplier.
     *
     * <p>A named indirection over {@code service.addPeripheral} purely so the
     * registration lists in {@code init()} read as a table rather than as
     * repeated service calls.</p>
     */
    default <T extends BlockEntity> void add(
            final SimPeripheralService service,
            final Supplier<BlockEntityType<T>> supplier,
            final SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> getter) {
        service.addPeripheral(supplier, getter);
    }
}
