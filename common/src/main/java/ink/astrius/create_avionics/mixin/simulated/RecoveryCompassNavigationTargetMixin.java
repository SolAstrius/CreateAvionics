package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.navigation_targets.RecoveryCompassNavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import ink.astrius.create_avionics.api.simulated.NavigationTargetExt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = RecoveryCompassNavigationTarget.class, remap = false)
public abstract class RecoveryCompassNavigationTargetMixin implements NavigationTargetExt {

    @Override
    public Map<String, Object> getPeripheralMetadata(final NavTableBlockEntity be, final ItemStack self) {
        final Map<String, Object> out = new HashMap<>();
        final String placer = self.getComponents().get(SimDataComponents.COMPASS_PLACER_UUID);
        if (placer != null) {
            out.put("placer_uuid", placer);
        }
        return out;
    }
}
