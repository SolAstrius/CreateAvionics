package ink.astrius.create_avionics.mixin.cc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.computer.core.ServerContext;
import ink.astrius.create_avionics.compat.cc.ComposingPeripheral;
import ink.astrius.create_avionics.compat.cc.PeripheralComposition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Composes generic-source methods onto a registered BE's capability-supplied
 * peripheral. Without this, CC's
 * {@code PlatformHelperImpl$PeripheralAccess#get(Direction)} short-circuits to
 * the capability peripheral and never runs the generic-source pass, so e.g.
 * the kinetic SCADA methods from {@code KineticSource} never reach Create's
 * pre-peripheraled BEs (motor, speed controller, gauges, sequenced gearshift).
 *
 * <p>Opt-in: a BE type is composed only when registered via
 * {@link PeripheralComposition#register}.</p>
 */
@Mixin(targets = "dan200.computercraft.shared.platform.PlatformHelperImpl$PeripheralAccess", remap = false)
public abstract class PeripheralAccessMixin {

    @ModifyExpressionValue(
        method = "get",
        at = @At(
            value = "INVOKE",
            target = "Ldan200/computercraft/shared/platform/PlatformHelperImpl$ComponentAccessImpl;get(Lnet/minecraft/core/Direction;)Ljava/lang/Object;"
        )
    )
    private Object createAvionics$composeOnPrimary(final Object original, final Direction direction) {
        if (!(original instanceof final IPeripheral primary)) return original;

        final BlockEntity owner = ((ComponentAccessOwnerAccessor) (Object) this).createAvionics$owner();
        if (owner == null) return original;
        final Level level = owner.getLevel();
        if (!(level instanceof final ServerLevel serverLevel)) return original;

        final BlockPos neighborPos = owner.getBlockPos().relative(direction);
        final BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor == null) return original;
        if (!PeripheralComposition.isRegistered(neighbor.getType())) return original;

        final MinecraftServer server = serverLevel.getServer();
        if (server == null) return original;

        final ComposingPeripheral composed = ComposingPeripheral.build(
            primary, neighbor, ServerContext.get(server).peripheralMethods());
        return composed != null ? composed : original;
    }
}
