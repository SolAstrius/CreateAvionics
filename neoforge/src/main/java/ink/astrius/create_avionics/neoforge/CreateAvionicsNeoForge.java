package ink.astrius.create_avionics.neoforge;

import ink.astrius.create_avionics.CreateAvionics;
import net.neoforged.fml.common.Mod;

@Mod(CreateAvionics.MOD_ID)
public class CreateAvionicsNeoForge {

    public CreateAvionicsNeoForge() {
        CreateAvionics.LOGGER.info("Loaded {}", CreateAvionics.MOD_NAME);
    }
}
