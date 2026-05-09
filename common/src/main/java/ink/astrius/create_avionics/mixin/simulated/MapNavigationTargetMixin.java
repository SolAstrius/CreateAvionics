package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.navigation_targets.MapNavigationTarget;
import ink.astrius.create_avionics.api.simulated.NavigationTargetExt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = MapNavigationTarget.class, remap = false)
public abstract class MapNavigationTargetMixin implements NavigationTargetExt {

    @Override
    public Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
        final Map<String, Object> out = new HashMap<>();
        final MapId mapId = self.getComponents().get(DataComponents.MAP_ID);
        if (mapId != null) {
            out.put("map_id", mapId.id());
        }
        return out;
    }
}
