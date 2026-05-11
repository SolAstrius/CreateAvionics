package ink.astrius.create_avionics.compat.aeronautics;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.SimPlatformService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.aeronautics.peripherals.GyroscopicPropellerBearingPeripheral;
import ink.astrius.create_avionics.compat.aeronautics.peripherals.MountedPotatoCannonPeripheral;
import ink.astrius.create_avionics.compat.aeronautics.peripherals.PropellerBearingPeripheral;
import ink.astrius.create_avionics.compat.aeronautics.peripherals.PropellerPeripheral;
import ink.astrius.create_avionics.compat.aeronautics.peripherals.generic.GasProviderGenericSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class AeroCCIntegration implements SimModCompatibilityService {

    @Override
    public String getModId() {
        return "computercraft";
    }

    @Override
    public void init() {
        if (!SimPlatformService.INSTANCE.isLoaded("aeronautics")) {
            return;
        }

        CreateAvionics.LOGGER.info("Registering ComputerCraft peripherals for Create: Aeronautics");

        final SimPeripheralService service = ServiceUtil.load(SimPeripheralService.class);

        ComputerCraftAPI.registerGenericSource(new GasProviderGenericSource());

        add(service, AeroBlockEntityTypes.WOODEN_PROPELLER, be -> new PropellerPeripheral<>(be, "wooden_propeller"));
        add(service, AeroBlockEntityTypes.ANDESITE_PROPELLER, be -> new PropellerPeripheral<>(be, "andesite_propeller"));
        add(service, AeroBlockEntityTypes.SMART_PROPELLER, be -> new PropellerPeripheral<>(be, "smart_propeller"));

        add(service, AeroBlockEntityTypes.PROPELLER_BEARING, PropellerBearingPeripheral::new);
        add(service, AeroBlockEntityTypes.GYROSCOPIC_PROPELLER_BEARING, GyroscopicPropellerBearingPeripheral::new);
        add(service, AeroBlockEntityTypes.MOUNTED_POTATO_CANNON, MountedPotatoCannonPeripheral::new);
    }

    private static <T extends BlockEntity> void add(final SimPeripheralService service, final Supplier<BlockEntityType<T>> supplier, final SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> getter) {
        service.addPeripheral(supplier, getter);
    }
}
