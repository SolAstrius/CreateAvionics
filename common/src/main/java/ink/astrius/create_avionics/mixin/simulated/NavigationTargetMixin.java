package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import ink.astrius.create_avionics.api.simulated.NavigationTargetExt;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = NavigationTarget.class, remap = false)
public interface NavigationTargetMixin extends NavigationTargetExt {
}
