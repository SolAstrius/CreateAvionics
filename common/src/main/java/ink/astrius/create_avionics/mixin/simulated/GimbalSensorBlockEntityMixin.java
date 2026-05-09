package ink.astrius.create_avionics.mixin.simulated;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import ink.astrius.create_avionics.api.simulated.GimbalSensorExt;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GimbalSensorBlockEntity.class, remap = false)
public abstract class GimbalSensorBlockEntityMixin implements GimbalSensorExt {

    @Unique private final Vector3d createAvionics$angularVelocityBody = new Vector3d();
    @Unique private final Vector3d createAvionics$gravityBody = new Vector3d();
    @Unique private final Vector3d createAvionics$linearAccelerationBody = new Vector3d();
    @Unique private final Vector3d createAvionics$lastLinearVelocity = new Vector3d();
    @Unique private boolean createAvionics$hasLastLinearVelocity = false;

    @Override
    public Vector3dc getAngularVelocityBody() {
        return this.createAvionics$angularVelocityBody;
    }

    @Override
    public Vector3dc getGravityBody() {
        return this.createAvionics$gravityBody;
    }

    @Override
    public Vector3dc getLinearAccelerationBody() {
        return this.createAvionics$linearAccelerationBody;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void createAvionics$updateBodyFrameVectors(final CallbackInfo ci) {
        final GimbalSensorBlockEntity self = (GimbalSensorBlockEntity) (Object) this;
        final SubLevel subLevel = Sable.HELPER.getContaining(self);

        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            final RigidBodyHandle body = RigidBodyHandle.of(serverSubLevel);

            body.getAngularVelocity(this.createAvionics$angularVelocityBody);
            serverSubLevel.logicalPose().orientation().transformInverse(this.createAvionics$angularVelocityBody);

            final Vector3dc worldPos = Sable.HELPER.projectOutOfSubLevel(self.getLevel(), JOMLConversion.atCenterOf(self.getBlockPos()));
            this.createAvionics$gravityBody.set(DimensionPhysicsData.getGravity(self.getLevel(), worldPos));
            serverSubLevel.logicalPose().orientation().transformInverse(this.createAvionics$gravityBody);

            final Vector3d currentLinearVelocity = body.getLinearVelocity(new Vector3d());
            if (this.createAvionics$hasLastLinearVelocity) {
                currentLinearVelocity.sub(this.createAvionics$lastLinearVelocity, this.createAvionics$linearAccelerationBody).mul(20.0);
                serverSubLevel.logicalPose().orientation().transformInverse(this.createAvionics$linearAccelerationBody);
                this.createAvionics$linearAccelerationBody.sub(this.createAvionics$gravityBody);
            } else {
                this.createAvionics$linearAccelerationBody.zero();
                this.createAvionics$hasLastLinearVelocity = true;
            }
            this.createAvionics$lastLinearVelocity.set(currentLinearVelocity);
        } else {
            this.createAvionics$hasLastLinearVelocity = false;
            this.createAvionics$linearAccelerationBody.zero();
        }
    }
}
