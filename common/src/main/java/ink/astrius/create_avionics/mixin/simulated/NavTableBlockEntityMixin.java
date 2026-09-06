package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import ink.astrius.create_avionics.api.simulated.NavTableExt;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Derives the bearing rate -- how fast the needle is swinging -- which upstream
 * never computes. The table recomputes {@code relativeAngle} every server tick
 * (only the distance pair is on the 11-tick cycle), so this differences it at
 * the same cadence the value actually changes.
 */
@Mixin(value = NavTableBlockEntity.class, remap = false)
public abstract class NavTableBlockEntityMixin implements NavTableExt {

    @Unique
    private double createAvionics$bearingRate = 0;

    @Unique
    private float createAvionics$lastAngle = 0;

    @Unique
    private boolean createAvionics$hasLastAngle = false;

    @Unique
    private ItemStack createAvionics$lastHeld = ItemStack.EMPTY;

    @Override
    public double getBearingRate() {
        return this.createAvionics$bearingRate;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void createAvionics$updateBearingRate(final CallbackInfo ci) {
        final NavTableBlockEntity self = (NavTableBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide()) {
            return;
        }

        // A swap of the held item re-aims the needle in one tick. Differencing
        // across that discontinuity would report hundreds of degrees per second
        // of swing that the vehicle never flew, so the sample is dropped and
        // re-seeded instead.
        final ItemStack held = self.getHeldItem();
        if (!ItemStack.matches(held, this.createAvionics$lastHeld)) {
            this.createAvionics$lastHeld = held.copy();
            this.createAvionics$hasLastAngle = false;
            this.createAvionics$bearingRate = 0;
        }

        if (self.currentTarget == null) {
            this.createAvionics$hasLastAngle = false;
            this.createAvionics$bearingRate = 0;
            return;
        }

        final float angle = self.getRelativeAngle();
        if (this.createAvionics$hasLastAngle) {
            // Bearing is relativeAngle minus a constant, so their rates are
            // identical; wrapping the delta to +/-180 keeps a sweep past the
            // +/-180 seam from reading as a full turn in the other direction.
            final double delta = ((angle - this.createAvionics$lastAngle) % 360.0 + 540.0) % 360.0 - 180.0;
            this.createAvionics$bearingRate = delta * 20.0;
        }
        this.createAvionics$lastAngle = angle;
        this.createAvionics$hasLastAngle = true;
    }
}
