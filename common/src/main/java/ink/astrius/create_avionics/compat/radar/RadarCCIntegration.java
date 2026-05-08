package ink.astrius.create_avionics.compat.radar;

import com.happysg.radar.registry.ModBlockEntityTypes;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.radar.peripherals.FireControllerPeripheral;
import ink.astrius.create_avionics.compat.radar.peripherals.MonitorPeripheral;
import ink.astrius.create_avionics.compat.radar.peripherals.PitchControllerPeripheral;
import ink.astrius.create_avionics.compat.radar.peripherals.RadarBearingPeripheral;
import ink.astrius.create_avionics.compat.radar.peripherals.YawControllerPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class RadarCCIntegration implements SimModCompatibilityService {

    @Override
    public String getModId() {
        return "computercraft";
    }

    @Override
    public void init() {
        CreateAvionics.LOGGER.info("Registering ComputerCraft peripherals for the radar subsystem");

        final SimPeripheralService service = ServiceUtil.load(SimPeripheralService.class);

        add(service, ModBlockEntityTypes.RADAR_BEARING, RadarBearingPeripheral::new);
        add(service, ModBlockEntityTypes.MONITOR, MonitorPeripheral::new);
        add(service, ModBlockEntityTypes.AUTO_YAW_CONTROLLER, YawControllerPeripheral::new);
        add(service, ModBlockEntityTypes.AUTO_PITCH_CONTROLLER, PitchControllerPeripheral::new);
        add(service, ModBlockEntityTypes.FIRE_CONTROLLER, FireControllerPeripheral::new);

        // The plane (stationary) radar BE is registered upstream in VS2CompatRegister.java,
        // which we removed with compat/vs2/. PlaneRadarPeripheral exists for when a
        // Sable-aware StationaryRadar is wired into the BE registry; not registered here
        // until that lands.
    }

    private static <T extends BlockEntity> void add(final SimPeripheralService service,
                                                    final Supplier<BlockEntityType<T>> supplier,
                                                    final SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> getter) {
        service.addPeripheral(supplier, getter);
    }
}
