package ink.astrius.create_avionics.neoforge;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.api.create.ElevatorContactExt;
import ink.astrius.create_avionics.api.create.ElevatorPulleyExt;
import ink.astrius.create_avionics.api.create.GantryShaftExt;
import ink.astrius.create_avionics.api.create.LinearActuatorExt;
import ink.astrius.create_avionics.api.create.MechanicalBearingExt;
import ink.astrius.create_avionics.compat.cc.PeripheralComposition;
import ink.astrius.create_avionics.registry.AvionicsBlockEntities;
import ink.astrius.create_avionics.registry.AvionicsBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(CreateAvionics.MOD_ID)
public class CreateAvionicsNeoForge {

    public CreateAvionicsNeoForge(final IEventBus bus) {
        CreateAvionics.LOGGER.info("Loaded {}", CreateAvionics.MOD_NAME);

        // Blocks before block entities: the type's builder dereferences the
        // block it is valid for.
        AvionicsBlocks.BLOCKS.register(bus);
        AvionicsBlocks.ITEMS.register(bus);
        AvionicsBlockEntities.BLOCK_ENTITIES.register(bus);

        bus.addListener(CreateAvionicsNeoForge::registerCapabilities);
        bus.addListener(CreateAvionicsNeoForge::addToCreativeTabs);
    }

    /** Put the rail modem somewhere a player can actually find it. */
    private static void addToCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(AvionicsBlocks.RAIL_MODEM_ITEM.get());
        }
    }

    private static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        if (!Mods.COMPUTERCRAFT.isLoaded()) return;

        // Smoke test: compose KineticSource SCADA onto Create's pre-peripheraled
        // sequenced gearshift, which today exposes our subclass's queue surface
        // but no SCADA pack.
        PeripheralComposition.register(AllBlockEntityTypes.SEQUENCED_GEARSHIFT);

        // Our own block, so we register its capability directly rather than
        // going through the mixin that retrofits Create's block entities.
        event.registerBlockEntity(
                PeripheralCapability.get(),
                AvionicsBlockEntities.RAIL_MODEM.get(),
                (be, context) -> be.computerBehaviour.getPeripheralCapability()
        );

        event.registerBlockEntity(
                PeripheralCapability.get(),
                AllBlockEntityTypes.MECHANICAL_BEARING.get(),
                (be, context) -> ((MechanicalBearingExt) be).createAvionics$getComputerBehaviour().getPeripheralCapability()
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                AllBlockEntityTypes.MECHANICAL_PISTON.get(),
                (be, context) -> ((LinearActuatorExt) be).createAvionics$getComputerBehaviour().getPeripheralCapability()
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                AllBlockEntityTypes.GANTRY_SHAFT.get(),
                (be, context) -> ((GantryShaftExt) be).createAvionics$getComputerBehaviour().getPeripheralCapability()
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                AllBlockEntityTypes.ROPE_PULLEY.get(),
                (be, context) -> ((LinearActuatorExt) be).createAvionics$getComputerBehaviour().getPeripheralCapability()
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                AllBlockEntityTypes.ELEVATOR_PULLEY.get(),
                (be, context) -> ((ElevatorPulleyExt) be).createAvionics$getComputerBehaviour().getPeripheralCapability()
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                AllBlockEntityTypes.ELEVATOR_CONTACT.get(),
                (be, context) -> ((ElevatorContactExt) be).createAvionics$getComputerBehaviour().getPeripheralCapability()
        );
    }
}
