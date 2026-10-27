package ink.astrius.create_avionics.neoforge.client;

import ink.astrius.create_avionics.CreateAvionics;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Tells the player once what a new version brings, the first time they join a world after
 * installing or updating it. Only a new minor or major version is announced; patch releases stay
 * quiet. The message is printed on the player's own client and never reaches the server.
 */
@EventBusSubscriber(modid = CreateAvionics.MOD_ID, value = Dist.CLIENT)
public final class UpdateAnnouncer {

    static final String RELEASES = "https://github.com/SolAstrius/CreateAvionics/releases/tag/v";
    static final String DOCS = "https://solastrius.github.io/CreateAvionics/";

    private UpdateAnnouncer() {
    }

    @SubscribeEvent
    public static void onLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        final String version = ModList.get().getModContainerById(CreateAvionics.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("");
        final String last = AvionicsClientConfig.LAST_ANNOUNCED.get();
        if (version.isEmpty() || version.equals(last)) {
            return;
        }
        AvionicsClientConfig.LAST_ANNOUNCED.set(version);
        AvionicsClientConfig.SPEC.save();
        if (!AvionicsClientConfig.ANNOUNCE_UPDATES.get() || minor(version).equals(minor(last))) {
            return;
        }
        final MutableComponent links = Component.empty()
                .append(link("create_avionics.announce.whats_new", RELEASES + version))
                .append(" ")
                .append(link("create_avionics.announce.guides", DOCS));
        // Each language names its own community (Discord, Telegram for Russian); none set, no link
        final String community = Language.getInstance().getOrDefault("create_avionics.announce.community_url", "");
        if (!community.isBlank()) {
            links.append(" ").append(link("create_avionics.announce.community", community));
        }
        event.getPlayer().displayClientMessage(Component.translatable("create_avionics.announce.updated",
                version, Component.translatable("create_avionics.announce.summary"))
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("create_avionics.announce.turn_off")))), false);
        event.getPlayer().displayClientMessage(links, false);
    }

    /** "0.7.2" -> "0.7": the part of a version that gets announced. */
    static String minor(final String version) {
        final String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }

    private static MutableComponent link(final String key, final String url) {
        return Component.literal("[").append(Component.translatable(key)).append("]")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url))));
    }
}
