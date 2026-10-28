package ink.astrius.create_avionics.neoforge.client;

import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.content.instrument.Instruments;
import ink.astrius.create_avionics.content.radar.Radars;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CreateAvionics.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CreateAvionicsClient {

    private CreateAvionicsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Instruments.BLOCK_ENTITY.get(), context -> new InstrumentRenderer());
        event.registerBlockEntityRenderer(Radars.WEATHER_BLOCK_ENTITY.get(), context -> new WeatherRadarRenderer());
    }

    /** The moving partials aren't referenced by any blockstate, so ask for them to be baked. */
    @SubscribeEvent
    public static void registerPartials(final ModelEvent.RegisterAdditional event) {
        InstrumentRenderer.PARTIALS.forEach(event::register);
        event.register(WeatherRadarRenderer.DISH);
    }
}
