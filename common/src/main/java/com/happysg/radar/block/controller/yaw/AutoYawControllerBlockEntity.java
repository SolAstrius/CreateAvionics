package com.happysg.radar.block.controller.yaw;

import com.happysg.radar.block.behavior.networks.WeaponNetworkData;
import com.happysg.radar.compat.Mods;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.cannon_control.cannon_mount.CannonMountBlockEntity;

import javax.annotation.Nullable;

/**
 * Auto-yaw controller for cannon mounts. v1 drives a Create Big Cannons
 * {@link CannonMountBlockEntity} directly; the sublevel-aware "PHYS" path
 * (drove a VS2 Clockwork PhysBearing) is archived for v2 — see
 * {@code archive/v2-future/} for revival notes.
 */
public class AutoYawControllerBlockEntity extends KineticBlockEntity {

    private static final double TOLERANCE_DEG = 0.15;
    private static final double DEADBAND_DEG = 0.5;

    private double targetAngle = 0.0;
    private boolean isRunning = false;

    private double lastCbcYawWritten = 0.0;
    private boolean hasLastCbcYawWritten = false;

    private double minAngleDeg = 0.0;
    private double maxAngleDeg = 360.0;

    private BlockPos lastKnownPos = BlockPos.ZERO;

    @Nullable
    private CannonMountBlockEntity cachedMount = null;

    private boolean mountDirty = true;

    private final CannonMountYaw cannonHandler;

    public AutoYawControllerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.cannonHandler = new CannonMountYaw(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide()) {
            return;
        }

        final CannonMountBlockEntity mount = resolveMount();
        if (mount != null && Mods.CREATEBIGCANNONS.isLoaded()) {
            cannonHandler.tick(mount);
        }

        if (level.getGameTime() % 40 == 0 && level instanceof ServerLevel serverLevel) {
            if (!lastKnownPos.equals(worldPosition)) {
                ResourceKey<Level> dim = serverLevel.dimension();
                WeaponNetworkData data = WeaponNetworkData.get(serverLevel);

                boolean updated = data.updateWeaponEndpointPosition(dim, lastKnownPos, worldPosition);
                if (updated) {
                    lastKnownPos = worldPosition;
                    setChanged();
                }
            }
        }
    }

    public void setTargetAngle(float targetAngle) {
        this.targetAngle = targetAngle;
        this.isRunning = true;
        notifyUpdate();
        setChanged();
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public void setTarget(@Nullable Vec3 targetPos) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (targetPos == null) {
            isRunning = false;
            notifyUpdate();
            setChanged();
            return;
        }

        final CannonMountBlockEntity mount = resolveMount();
        if (mount == null) return;

        if (Mods.CREATEBIGCANNONS.isLoaded()) {
            cannonHandler.setTarget(mount, targetPos);
        }
    }

    public boolean atTargetYaw(boolean lag) {
        if (level == null) return false;

        final CannonMountBlockEntity mount = resolveMount();
        if (mount == null) return false;

        if (Mods.CREATEBIGCANNONS.isLoaded()) {
            return cannonHandler.atTargetYaw(mount, lag);
        }
        return false;
    }

    public boolean isUpsideDown() {
        if (level == null) {
            return false;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(DirectionalKineticBlock.FACING)) {
            return false;
        }

        return state.getValue(DirectionalKineticBlock.FACING) == Direction.UP;
    }

    public void markMountDirtyExternal() {
        mountDirty = true;
    }

    public void onRelevantNeighborChanged(BlockPos fromPos) {
        BlockPos mountPos = getMountPos();
        if (mountPos == null) {
            return;
        }

        if (fromPos.equals(mountPos)) {
            mountDirty = true;
        }
    }

    @Nullable
    public CannonMountBlockEntity resolveMount() {
        if (level == null) {
            return null;
        }

        if (mountDirty) {
            refreshMountCache();
        }

        return cachedMount;
    }

    private void refreshMountCache() {
        if (level == null) {
            return;
        }

        BlockPos mountPos = getMountPos();
        CannonMountBlockEntity newMount = null;

        if (mountPos != null) {
            BlockEntity adjacent = level.getBlockEntity(mountPos);

            if (Mods.CREATEBIGCANNONS.isLoaded() && adjacent instanceof CannonMountBlockEntity cbc) {
                newMount = cbc;
            }
        }

        cachedMount = newMount;
        mountDirty = false;

        if (newMount == null) {
            isRunning = false;
            hasLastCbcYawWritten = false;
        }

        setChanged();
        notifyUpdate();
    }

    @Nullable
    private BlockPos getMountPos() {
        if (level == null) {
            return null;
        }

        BlockPos preferred = isUpsideDown() ? worldPosition.below() : worldPosition.above();
        BlockPos opposite = isUpsideDown() ? worldPosition.above() : worldPosition.below();

        if (isControllableMount(preferred)) {
            return preferred;
        }
        if (isControllableMount(opposite)) {
            return opposite;
        }

        return preferred;
    }

    private boolean isControllableMount(BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return Mods.CREATEBIGCANNONS.isLoaded() && be instanceof CannonMountBlockEntity;
    }

    public double getMinAngleDeg() {
        return minAngleDeg;
    }

    public double getMaxAngleDeg() {
        return maxAngleDeg;
    }

    public void setMinAngleDeg(double v) {
        minAngleDeg = wrap360(wrap180(v));
        notifyUpdate();
        setChanged();
    }

    public void setMaxAngleDeg(double v) {
        maxAngleDeg = wrap360(wrap180(v));
        notifyUpdate();
        setChanged();
    }

    public boolean canPossiblyAimAt(Vec3 originWorld, Vec3 targetWorld) {
        if (originWorld == null || targetWorld == null) {
            return false;
        }

        Vec3 d = targetWorld.subtract(originWorld);
        return d.lengthSqr() < 1.0e-6 || true;
    }

    /**
     * Pure world-frame yaw to a target. The sublevel-frame branch
     * (transforming through ship-to-world) is archived with the v2
     * sublevel control work — see {@code archive/v2-future/}.
     */
    public double computeYawToTargetDeg(Vec3 cannonCenterWorld, Vec3 targetWorld) {
        double dx = targetWorld.x - cannonCenterWorld.x;
        double dz = targetWorld.z - cannonCenterWorld.z;
        return Math.toDegrees(Math.atan2(dz, dx)) + 90.0;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        if (compound.contains("MinAngleDeg", Tag.TAG_DOUBLE)) {
            minAngleDeg = compound.getDouble("MinAngleDeg");
        }
        if (compound.contains("MaxAngleDeg", Tag.TAG_DOUBLE)) {
            maxAngleDeg = compound.getDouble("MaxAngleDeg");
        }

        targetAngle = wrap360(compound.getDouble("TargetAngle"));
        isRunning = compound.getBoolean("IsRunning");

        if (compound.contains("LastKnownPos", Tag.TAG_LONG)) {
            lastKnownPos = BlockPos.of(compound.getLong("LastKnownPos"));
        } else {
            lastKnownPos = worldPosition;
        }

        hasLastCbcYawWritten = false;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        compound.putDouble("MinAngleDeg", minAngleDeg);
        compound.putDouble("MaxAngleDeg", maxAngleDeg);
        compound.putDouble("TargetAngle", wrap360(targetAngle));
        compound.putBoolean("IsRunning", isRunning);
        compound.putLong("LastKnownPos", lastKnownPos.asLong());
    }

    @Override
    protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
    }

    public void setInternalTargetAngle(double targetAngle) {
        this.targetAngle = targetAngle;
    }

    /**
     * Stops auto-aim — clears the running flag so manual angle commands
     * (via setTargetAngle) stick instead of being immediately overwritten
     * by the WeaponFiringControl pipeline. Public for the CC peripheral.
     */
    public void stopAuto() {
        this.isRunning = false;
        notifyUpdate();
        setChanged();
    }

    void setRunning(boolean running) {
        this.isRunning = running;
    }

    boolean isRunningController() {
        return isRunning;
    }

    void recordCbcYawWritten(double yawDeg) {
        this.lastCbcYawWritten = wrap360(yawDeg);
        this.hasLastCbcYawWritten = true;
    }

    boolean hasLastCbcYawWritten() {
        return hasLastCbcYawWritten;
    }

    double getLastCbcYawWritten() {
        return lastCbcYawWritten;
    }

    void setInternalMinAngleDeg(double v) {
        this.minAngleDeg = v;
    }

    void setInternalMaxAngleDeg(double v) {
        this.maxAngleDeg = v;
    }

    static double getToleranceDeg() {
        return TOLERANCE_DEG;
    }

    static double getDeadbandDeg() {
        return DEADBAND_DEG;
    }

    static double wrap360(double deg) {
        deg %= 360.0;
        if (deg < 0) deg += 360.0;
        return deg;
    }

    static double wrap180(double deg) {
        deg = wrap360(deg);
        if (deg >= 180.0) deg -= 360.0;
        return deg;
    }

    static double shortestDelta(double from, double to) {
        return ((to - from + 540.0) % 360.0) - 180.0;
    }
}
