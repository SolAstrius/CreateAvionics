package ink.astrius.create_avionics.neoforge;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.impl.Peripherals;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.api.create.ElevatorContactExt;
import ink.astrius.create_avionics.api.create.ElevatorPulleyExt;
import ink.astrius.create_avionics.api.create.GantryShaftExt;
import ink.astrius.create_avionics.api.create.LinearActuatorExt;
import ink.astrius.create_avionics.api.create.MechanicalBearingExt;
import ink.astrius.create_avionics.compat.cc.PeripheralComposition;
import ink.astrius.create_avionics.content.controller.Controllers;
import ink.astrius.create_avionics.content.controller.ShipBridge;
import ink.astrius.create_avionics.content.controller.ShipScan;
import ink.astrius.create_avionics.content.instrument.Instrument;
import ink.astrius.create_avionics.content.radar.Radars;
import ink.astrius.create_avionics.content.instrument.InstrumentSources;
import ink.astrius.create_avionics.content.instrument.Instruments;
import ink.astrius.create_avionics.neoforge.client.AvionicsClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

@Mod(CreateAvionics.MOD_ID)
public class CreateAvionicsNeoForge {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAvionics.MOD_ID);

    /** The mod's own tab, placed after Create's. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CreateAvionics.MOD_ID))
                    .icon(() -> Instruments.BLOCKS_BY_INSTRUMENT.get(Instrument.BAROMETER).toStack())
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath("create", "palettes"))
                    .displayItems((parameters, output) -> {
                        output.accept(Controllers.BLOCK.get());
                        output.accept(Radars.MK1.get());
                        Instruments.BLOCKS_BY_INSTRUMENT.values().forEach(output::accept);
                    })
                    .build());

    public CreateAvionicsNeoForge(final IEventBus bus, final ModContainer container) {
        CreateAvionics.LOGGER.info("Loaded {}", CreateAvionics.MOD_NAME);
        container.registerConfig(ModConfig.Type.CLIENT, AvionicsClientConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, AvionicsServerConfig.SPEC);
        bus.addListener(ModConfigEvent.Loading.class, AvionicsServerConfig::onLoad);
        bus.addListener(ModConfigEvent.Reloading.class, AvionicsServerConfig::onLoad);
        InstrumentSources.thermoo = ModList.get().isLoaded("thermoo");
        InstrumentSources.sereneSeasons = ModList.get().isLoaded("sereneseasons");
        InstrumentSources.downfall = biome -> biome.getModifiedClimateSettings().downfall();
        Instruments.registers().forEach(register -> register.register(bus));
        Controllers.registers().forEach(register -> register.register(bus));
        Radars.registers().forEach(register -> register.register(bus));
        ShipScan.lookup = CreateAvionicsNeoForge::peripheralAt;
        ShipBridge.elementAt = (level, pos, side) -> level.getCapability(WiredElementCapability.get(), pos, side);
        TABS.register(bus);
        bus.addListener(CreateAvionicsNeoForge::registerCapabilities);
    }

    /**
     * What a wired modem would see at a position: the block's own peripheral if it has one, else
     * CC's generic peripheral (inventories, tanks, Create's kinetic blocks), which CC serves through
     * its own lookup rather than the capability.
     */
    private static @Nullable IPeripheral peripheralAt(final Level level, final BlockPos pos) {
        final IPeripheral own = level.getCapability(PeripheralCapability.get(), pos, null);
        if (own != null || !(level instanceof final ServerLevel server)) {
            return own;
        }
        final BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : Peripherals.getGenericPeripheral(server, pos, null, be);
    }

    private static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        if (!Mods.COMPUTERCRAFT.isLoaded()) return;

        event.registerBlockEntity(
                PeripheralCapability.get(),
                Radars.BLOCK_ENTITY.get(),
                (be, side) -> be.peripheral()
        );
        // The controller's own peripheral is the ship's radio
        event.registerBlockEntity(
                PeripheralCapability.get(),
                Controllers.BLOCK_ENTITY.get(),
                (be, side) -> be.peripheral()
        );
        // The controller is a wired-network block: cables and modems connect to it
        event.registerBlockEntity(
                WiredElementCapability.get(),
                Controllers.BLOCK_ENTITY.get(),
                (be, side) -> be.bridge()
        );
        event.registerBlockEntity(
                PeripheralCapability.get(),
                Instruments.BLOCK_ENTITY.get(),
                (be, context) -> be.peripheral()
        );

        // Smoke test: compose KineticSource SCADA onto Create's pre-peripheraled
        // sequenced gearshift, which today exposes our subclass's queue surface
        // but no SCADA pack.
        PeripheralComposition.register(AllBlockEntityTypes.SEQUENCED_GEARSHIFT);

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
