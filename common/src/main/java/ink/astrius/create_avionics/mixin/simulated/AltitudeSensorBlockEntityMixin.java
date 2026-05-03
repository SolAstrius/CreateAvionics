package ink.astrius.create_avionics.mixin.simulated;

import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import ink.astrius.create_avionics.api.simulated.AltitudeSensorExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AltitudeSensorBlockEntity.class, remap = false)
public abstract class AltitudeSensorBlockEntityMixin implements AltitudeSensorExt {

    @Unique
    private double createAvionics$verticalSpeed = 0;

    @Unique
    private float createAvionics$lastWorldHeight = 0;

    @Unique
    private boolean createAvionics$hasLastWorldHeight = false;

    @Override
    public double getVerticalSpeed() {
        return this.createAvionics$verticalSpeed;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void createAvionics$updateVerticalSpeed(final CallbackInfo ci) {
        final AltitudeSensorBlockEntity self = (AltitudeSensorBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide()) {
            return;
        }
        final float currentHeight = self.getWorldHeight();
        if (this.createAvionics$hasLastWorldHeight) {
            this.createAvionics$verticalSpeed = (currentHeight - this.createAvionics$lastWorldHeight) * 20.0;
        }
        this.createAvionics$lastWorldHeight = currentHeight;
        this.createAvionics$hasLastWorldHeight = true;
    }
}
