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
        // Received as Object on purpose: COMPASS_PLACER_UUID is a
        // DataComponentType<String> up to Simulated 1.2.1 and a
        // DataComponentType<UUID> from 1.3.0. Generics are erased, so the only
        // thing that breaks across versions is inference at this call site --
        // widening the local defeats it and both builds compile. toString()
        // yields the same canonical UUID text either way, so the Lua value is
        // unchanged.
        final Object placer = self.getComponents().get(SimDataComponents.COMPASS_PLACER_UUID);
        if (placer != null) {
            out.put("placer_uuid", placer.toString());
        }
        return out;
    }
}
