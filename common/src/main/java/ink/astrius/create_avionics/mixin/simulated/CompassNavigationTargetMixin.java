package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.navigation_targets.CompassNavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import ink.astrius.create_avionics.api.simulated.NavigationTargetExt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(value = CompassNavigationTarget.class, remap = false)
public abstract class CompassNavigationTargetMixin implements NavigationTargetExt {

    @Override
    public Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
        final Map<String, Object> out = new HashMap<>();
        final UUID tracker = self.get(SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER);
        if (tracker != null) {
            out.put("kind", "lodestone");
            out.put("sublevel_id", tracker.toString());
        } else {
            out.put("kind", "spawn");
        }
        return out;
    }
}
