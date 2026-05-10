package ink.astrius.create_avionics.mixin.offroad;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import ink.astrius.create_avionics.api.offroad.WheelMountExt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WheelMountBlockEntity.class, remap = false)
public abstract class WheelMountBlockEntityMixin implements WheelMountExt {

    @Shadow private int clientSteeringSignal;
    @Shadow protected int clientSteeringSignalLeft;
    @Shadow protected int clientSteeringSignalRight;
    @Shadow private int lastServerSteeringSignal;
    @Shadow private int lastServerSteeringSignalLeft;
    @Shadow private int lastServerSteeringSignalRight;
    @Shadow private double extension;
    @Shadow private double angularVelocity;
    @Shadow private double touchingFriction;
    @Shadow private boolean liftedUp;

    @Unique private volatile boolean createAvionics$steeringOverridden = false;
    @Unique private volatile int createAvionics$steeringOverride = 0;
    @Unique private volatile boolean createAvionics$brakeOverridden = false;
    @Unique private volatile int createAvionics$brakeOverride = 0;

    @Unique
    private void createAvionics$pushSync() {
        ((SyncedBlockEntity) (Object) this).sendData();
    }

    @Override
    public boolean isSteeringOverridden() {
        return this.createAvionics$steeringOverridden;
    }

    @Override
    public int getSteeringSignalValue() {
        return this.createAvionics$steeringOverridden
                ? this.createAvionics$steeringOverride
                : this.lastServerSteeringSignal;
    }

    @Override
    public void setSteeringOverride(final int signal) {
        final int clamped = Mth.clamp(signal, -15, 15);
        this.createAvionics$steeringOverride = clamped;
        this.createAvionics$steeringOverridden = true;
        this.lastServerSteeringSignal = clamped;
        this.lastServerSteeringSignalLeft = Math.max(clamped, 0);
        this.lastServerSteeringSignalRight = Math.max(-clamped, 0);
        this.clientSteeringSignal = clamped;
        this.clientSteeringSignalLeft = Math.max(clamped, 0);
        this.clientSteeringSignalRight = Math.max(-clamped, 0);
        this.createAvionics$pushSync();
    }

    @Override
    public void clearSteeringOverride() {
        this.createAvionics$steeringOverridden = false;
        this.createAvionics$pushSync();
    }

    @Override
    public boolean isBrakeOverridden() {
        return this.createAvionics$brakeOverridden;
    }

    @Override
    public int getBrakeSignalValue() {
        return this.createAvionics$brakeOverride;
    }

    @Override
    public void setBrakeOverride(final int signal) {
        this.createAvionics$brakeOverride = Mth.clamp(signal, 0, 15);
        this.createAvionics$brakeOverridden = true;
        this.createAvionics$pushSync();
    }

    @Override
    public void clearBrakeOverride() {
        this.createAvionics$brakeOverridden = false;
        this.createAvionics$pushSync();
    }

    @Override
    public double getExtension() {
        return this.extension;
    }

    @Override
    public double getAngularVelocity() {
        return this.angularVelocity;
    }

    @Override
    public double getTouchingFriction() {
        return this.touchingFriction;
    }

    @Override
    public boolean isLiftedUp() {
        return this.liftedUp;
    }

    /**
     * Replace the entire steering-signal computation when override is active.
     * Bypasses both redstone reads on each side and the sendData diff check;
     * setSteeringOverride already pushed the latest value to the synced fields.
     */
    @Inject(method = "getSteeringSignal", at = @At("HEAD"), cancellable = true)
    private void createAvionics$overrideSteering(final CallbackInfoReturnable<Integer> cir) {
        if (this.createAvionics$steeringOverridden) {
            cir.setReturnValue(this.createAvionics$steeringOverride);
        }
    }

    /**
     * Server-side physics brake read (in sable$physicsTick). One call per tick
     * with (pos.above(), UP).
     */
    @WrapOperation(
            method = "sable$physicsTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I")
    )
    private int createAvionics$overrideBrakePhysics(final Level level, final BlockPos pos, final Direction dir, final Operation<Integer> orig) {
        return this.createAvionics$brakeOverridden ? this.createAvionics$brakeOverride : orig.call(level, pos, dir);
    }

    /**
     * Client-side visual brake read (in tick(), after the isClientSide gate).
     * Slows the wheel's drawn angular velocity to match braking.
     */
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I")
    )
    private int createAvionics$overrideBrakeTick(final Level level, final BlockPos pos, final Direction dir, final Operation<Integer> orig) {
        return this.createAvionics$brakeOverridden ? this.createAvionics$brakeOverride : orig.call(level, pos, dir);
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createAvionics$writeOverrides(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (this.createAvionics$steeringOverridden) {
            tag.putInt("CreateAvionicsSteeringOverride", this.createAvionics$steeringOverride);
        }
        if (this.createAvionics$brakeOverridden) {
            tag.putInt("CreateAvionicsBrakeOverride", this.createAvionics$brakeOverride);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createAvionics$readOverrides(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (tag.contains("CreateAvionicsSteeringOverride", 3)) {
            this.createAvionics$steeringOverride = Mth.clamp(tag.getInt("CreateAvionicsSteeringOverride"), -15, 15);
            this.createAvionics$steeringOverridden = true;
        } else {
            this.createAvionics$steeringOverridden = false;
        }
        if (tag.contains("CreateAvionicsBrakeOverride", 3)) {
            this.createAvionics$brakeOverride = Mth.clamp(tag.getInt("CreateAvionicsBrakeOverride"), 0, 15);
            this.createAvionics$brakeOverridden = true;
        } else {
            this.createAvionics$brakeOverridden = false;
        }
    }
}
