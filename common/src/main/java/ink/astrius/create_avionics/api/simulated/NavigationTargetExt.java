package ink.astrius.create_avionics.api.simulated;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface NavigationTargetExt {
    default Map<String, Object> getPeripheralMetadata(NavTableBlockEntity be, ItemStack self) {
        return Map.of();
    }
}
