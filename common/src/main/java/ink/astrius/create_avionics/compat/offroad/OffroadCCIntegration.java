package ink.astrius.create_avionics.compat.offroad;

import dev.ryanhcode.offroad.index.OffroadBlockEntityTypes;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimPlatformService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.CCIntegration;
import ink.astrius.create_avionics.compat.offroad.peripherals.WheelMountPeripheral;

public class OffroadCCIntegration implements CCIntegration {

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
}
