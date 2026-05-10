package ink.astrius.create_avionics.compat.offroad;

import dan200.computercraft.api.peripheral.IPeripheral;
import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.SimPlatformService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.offroad.peripherals.WheelMountPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class OffroadCCIntegration implements SimModCompatibilityService {

    @Override
    public String getModId() {
        return "computercraft";
    }

    @Override
    public void init() {
        if (!SimPlatformService.INSTANCE.isLoaded("offroad")) {
            return;
        }

        CreateAvionics.LOGGER.info("Registering ComputerCraft peripherals for Create: Offroad");

        final SimPeripheralService service = ServiceUtil.load(SimPeripheralService.class);

        add(service, OffroadBlockEntityTypes.WHEEL_MOUNT, WheelMountPeripheral::new);
    }

    private static <T extends BlockEntity> void add(final SimPeripheralService service, final Supplier<BlockEntityType<T>> supplier, final SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> getter) {
        service.addPeripheral(supplier, getter);
    }
}
