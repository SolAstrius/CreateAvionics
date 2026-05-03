package ink.astrius.create_avionics.mixin.simulated;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlock;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import ink.astrius.create_avionics.api.simulated.AnalogTransmissionExt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// @WrapOperation on Level#getBestNeighborSignal short-circuits the redstone
// update branch when externally controlled, leaving super.tick() intact.
@Mixin(value = AnalogTransmissionBlockEntity.class, remap = false)
public abstract class AnalogTransmissionBlockEntityMixin extends KineticBlockEntity implements AnalogTransmissionExt {

    @Shadow private int signal;
    @Shadow private boolean oversaturated;
    @Shadow private AnalogTransmissionBlockEntity.AnalogTransmissionCogwheel extraWheel;

    @Unique
    private boolean createAvionics$externallyControlled = false;

    private AnalogTransmissionBlockEntityMixin() {
        super(null, null, null);
    }

    @Override
    public int getSignal() {
        return this.signal;
    }

    @Override
    public boolean isOversaturated() {
        return this.oversaturated;
    }

    @Override
    public boolean isExternallyControlled() {
        return this.createAvionics$externallyControlled;
    }

    @Override
    public void setExternallyControlled(final boolean externallyControlled) {
        this.createAvionics$externallyControlled = externallyControlled;
    }

    @Override
    public void applySignal(final int newSignal) {
        if (newSignal == this.signal) return;

        this.detachKinetics();
        this.extraWheel.detachKinetics();

        this.removeSource();
        this.extraWheel.removeSource();

        this.signal = newSignal;
        this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AnalogTransmissionBlock.POWERED, this.signal > 0));

        if (((KineticBlockEntityExtension) this).simulated$getConnectedToExtraKinetics()) {
            this.attachKinetics();
            this.extraWheel.attachKinetics();
        } else {
            this.extraWheel.attachKinetics();
            this.attachKinetics();
        }

        this.sendData();
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I"
        )
    )
    private int createAvionics$skipRedstoneWhenExternallyControlled(
            final Level level, final BlockPos pos, final Operation<Integer> original) {
        if (this.createAvionics$externallyControlled) {
            return this.signal;
        }
        return original.call(level, pos);
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createAvionics$writeExternallyControlled(
            final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (!clientPacket) {
            compound.putBoolean("ExternallyControlled", this.createAvionics$externallyControlled);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createAvionics$readExternallyControlled(
            final CompoundTag compound, final HolderLookup.Provider registries, final boolean clientPacket, final CallbackInfo ci) {
        if (!clientPacket) {
            this.createAvionics$externallyControlled = compound.getBoolean("ExternallyControlled");
        }
    }
}
