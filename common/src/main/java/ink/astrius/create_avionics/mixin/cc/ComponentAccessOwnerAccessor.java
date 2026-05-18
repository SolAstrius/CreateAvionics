package ink.astrius.create_avionics.mixin.cc;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@code owner} field of CC's package-private
 * {@code PlatformHelperImpl$ComponentAccessImpl} so the peripheral-composition
 * mixin can resolve the neighbour BE from the {@code Direction} parameter
 * alone, without rebuilding a capability cache.
 */
@Mixin(targets = "dan200.computercraft.shared.platform.PlatformHelperImpl$ComponentAccessImpl", remap = false)
public interface ComponentAccessOwnerAccessor {

    @Accessor("owner")
    BlockEntity createAvionics$owner();
}
