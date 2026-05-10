package ink.astrius.create_avionics.mixin.aeronautics;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlockEntity;
import ink.astrius.create_avionics.api.aero.GyroscopicPropellerBearingExt;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GyroscopicPropellerBearingBlockEntity.class, remap = false)
public abstract class GyroscopicPropellerBearingBlockEntityMixin implements GyroscopicPropellerBearingExt {

    @Unique
    private static final String createAvionics$NBT_KEY = "CreateAvionicsManualTarget";

    @Unique
    private volatile @Nullable Vector3d createAvionics$manualTarget = null;

    @Unique
    private void createAvionics$pushSync() {
        ((SyncedBlockEntity) (Object) this).sendData();
    }

    @Override
    public void setManualTarget(final Vector3dc target) {
        if (!Double.isFinite(target.x()) || !Double.isFinite(target.y()) || !Double.isFinite(target.z())) {
            throw new IllegalArgumentException("manual target components must be finite");
        }
        final double lenSq = target.lengthSquared();
        if (lenSq < 1.0e-12) {
            throw new IllegalArgumentException("manual target must be non-zero");
        }
        this.createAvionics$manualTarget = new Vector3d(target).normalize();
        this.createAvionics$pushSync();
    }

    @Override
    public void clearManualTarget() {
        this.createAvionics$manualTarget = null;
        this.createAvionics$pushSync();
    }

    @Override
    public @Nullable Vector3dc getManualTarget() {
        return this.createAvionics$manualTarget;
    }

    /**
     * Replace the gravity-derived target inside updateTilt with a script-supplied
     * one when manual override is set. The bearing's own setTilt still applies
     * the 12° cone clamp, the redstone power gate, and the stabilization-strength
     * lerp on top of our value.
     */
    @Redirect(
            method = "updateTilt",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/eriksonn/aeronautics/content/blocks/propeller/bearing/gyroscopic_propeller_bearing/GyroscopicPropellerBearingBlockEntity;setTilt(Lorg/joml/Vector3d;Lorg/joml/Vector3d;D)V"
            )
    )
    private void createAvionics$redirectSetTilt(final GyroscopicPropellerBearingBlockEntity self, final Vector3d tilt, final Vector3d target, final double maxStep) {
        final Vector3d manual = this.createAvionics$manualTarget;
        self.setTilt(tilt, manual != null ? new Vector3d(manual) : target, maxStep);
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createAvionics$writeManualTarget(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        final Vector3d t = this.createAvionics$manualTarget;
        if (t == null) {
            return;
        }
        final ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(t.x));
        list.add(DoubleTag.valueOf(t.y));
        list.add(DoubleTag.valueOf(t.z));
        compound.put(createAvionics$NBT_KEY, list);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createAvionics$readManualTarget(final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (!compound.contains(createAvionics$NBT_KEY, 9)) {
            this.createAvionics$manualTarget = null;
            return;
        }
        final ListTag list = compound.getList(createAvionics$NBT_KEY, 6);
        if (list.size() != 3) {
            this.createAvionics$manualTarget = null;
            return;
        }
        final Vector3d v = new Vector3d(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z) || v.lengthSquared() < 1.0e-12) {
            this.createAvionics$manualTarget = null;
            return;
        }
        this.createAvionics$manualTarget = v.normalize();
    }
}
