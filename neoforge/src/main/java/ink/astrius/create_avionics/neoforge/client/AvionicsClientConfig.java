package ink.astrius.create_avionics.neoforge.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code create_avionics-client.toml}. */
public final class AvionicsClientConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_UPDATES;
    public static final ModConfigSpec.ConfigValue<String> LAST_ANNOUNCED;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ANNOUNCE_UPDATES = builder
                .comment("Say in chat what's new, once, the first time you join a world after installing or updating to a new version.")
                .define("announceUpdates", true);
        LAST_ANNOUNCED = builder
                .comment("The last version announced. Managed by the mod.")
                .define("lastAnnouncedVersion", "");
        SPEC = builder.build();
    }

    private AvionicsClientConfig() {
    }
}
