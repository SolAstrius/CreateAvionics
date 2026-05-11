package ink.astrius.create_avionics.compat.simulated;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.simulated_team.simulated.compat.computercraft.peripherals.*;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.service.ServiceUtil;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.simulated.peripherals.*;
import ink.astrius.create_avionics.compat.simulated.peripherals.AltitudeSensorPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.GimbalSensorPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.LinkedTypewriterPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.NavTablePeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.SwivelBearingPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.TorsionSpringPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.VelocitySensorPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class SimulatedCCIntegration implements SimModCompatibilityService {

    @Override
    public String getModId() {
        return "computercraft";
    }

    @Override
    public void init() {
        CreateAvionics.LOGGER.info("Registering ComputerCraft peripherals for Create: Simulated");

        final SimPeripheralService service = ServiceUtil.load(SimPeripheralService.class);

        add(service, SimBlockEntityTypes.ALTITUDE_SENSOR, AltitudeSensorPeripheral::new);
        add(service, SimBlockEntityTypes.GIMBAL_SENSOR, GimbalSensorPeripheral::new);
        add(service, SimBlockEntityTypes.NAVIGATION_TABLE, NavTablePeripheral::new);
        add(service, SimBlockEntityTypes.LINKED_TYPEWRITER, LinkedTypewriterPeripheral::new);
        add(service, SimBlockEntityTypes.OPTICAL_SENSOR, OpticalSensorPeripheral::new);
        add(service, SimBlockEntityTypes.SWIVEL_BEARING, SwivelBearingPeripheral::new);
        add(service, SimBlockEntityTypes.VELOCITY_SENSOR, VelocitySensorPeripheral::new);

        add(service, SimBlockEntityTypes.DIRECTIONAL_LINKED_RECEIVER, DirectionalLinkPeripheral::new);
        add(service, SimBlockEntityTypes.MODULATING_LINKED_RECEIVER, ModulatingLinkPeripheral::new);

        add(service, SimBlockEntityTypes.DOCKING_CONNECTOR, DockingConnectorPeripheral::new);
        add(service, SimBlockEntityTypes.TORSION_SPRING, TorsionSpringPeripheral::new);
        add(service, SimBlockEntityTypes.NAMEPLATE, NamePlatePeripheral::new);

        add(service, SimBlockEntityTypes.SIMPLE_BE, AnalogTransmissionPeripheral::new);
        add(service, SimBlockEntityTypes.PHYSICS_ASSEMBLER, PhysicsAssemblerPeripheral::new);
        add(service, SimBlockEntityTypes.PORTABLE_ENGINE, PortableEnginePeripheral::new);
        add(service, SimBlockEntityTypes.STEERING_WHEEL, SteeringWheelPeripheral::new);
        add(service, SimBlockEntityTypes.THROTTLE_LEVER, ThrottleLeverPeripheral::new);

        add(service, SimBlockEntityTypes.LASER_POINTER, LaserPointerPeripheral::new);
        add(service, SimBlockEntityTypes.LASER_SENSOR, LaserSensorPeripheral::new);
        add(service, SimBlockEntityTypes.ROPE_WINCH, RopeWinchPeripheral::new);
        add(service, SimBlockEntityTypes.DIRECTIONAL_GEARSHIFT, DirectionalGearshiftPeripheral::new);

        service.addWired(SimBlockEntityTypes.DOCKING_CONNECTOR, (blockEntity, direction) -> {
            if (blockEntity.getBlockState().getValue(DockingConnectorBlock.FACING) == direction) {
                return null;
            }
            return (WiredElement) blockEntity.ccWiredElement;
        });
    }

    private static <T extends BlockEntity> void add(final SimPeripheralService service, final Supplier<BlockEntityType<T>> supplier, final SimPeripheralService.SimpleCapabilityGetter<T, IPeripheral> getter) {
        service.addPeripheral(supplier, getter);
    }
}
