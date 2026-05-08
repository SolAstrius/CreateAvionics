package com.happysg.radar.compat.physics;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Physics-framework facade for the radar subsystem. Delegates to Sable
 * Companion, which provides safe defaults when Sable itself is not
 * installed (queries return null / pass-through), so callers do not need
 * to gate on Sable presence themselves.
 *
 * Replaces the previous VS2-targeted PhysicsHandler. Method names are
 * preserved where they made sense in the new framework so existing call
 * sites need only an import change.
 */
public final class PhysicsHandler {

    private PhysicsHandler() {}

    // --- sublevel lookup ---

    @Nullable
    public static SubLevelAccess getContaining(final Level level, final BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos);
    }

    @Nullable
    public static SubLevelAccess getContaining(final BlockEntity blockEntity) {
        return SableCompanion.INSTANCE.getContaining(blockEntity);
    }

    public static boolean isBlockInShipyard(final Level level, final BlockPos pos) {
        return SableCompanion.INSTANCE.isInPlotGrid(level, pos);
    }

    // --- local → global ---

    /**
     * Translate a sublevel-local block position to its current global
     * position. Returns the input unchanged when not in a sublevel.
     */
    public static BlockPos getWorldPos(final Level level, final BlockPos pos) {
        final SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, pos);
        if (sub == null) return pos;
        final Vec3 global = sub.logicalPose().transformPosition(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        return BlockPos.containing(global);
    }

    public static BlockPos getWorldPos(final BlockEntity be) {
        return getWorldPos(be.getLevel(), be.getBlockPos());
    }

    /**
     * Project a sublevel-local block position to its current global Vec3.
     * Returns the block centre when not in a sublevel.
     */
    public static Vec3 getWorldVec(final Level level, final BlockPos pos) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos.getCenter());
    }

    /**
     * Project an arbitrary sublevel-local Vec3 to its current global
     * position. Pass-through when not in a sublevel.
     */
    public static Vec3 getWorldVec(final Level level, final Vec3 vec) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, vec);
    }

    public static Vec3 getWorldVec(final BlockEntity be) {
        return getWorldVec(be.getLevel(), be.getBlockPos());
    }

    /**
     * Transform a body-local direction vector to global frame using the
     * orientation of the sublevel containing the given block-entity.
     * Pass-through when the block-entity is not in a sublevel.
     */
    public static Vec3 getWorldVecDirectionTransform(final Vec3 localDir, final BlockEntity be) {
        final SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(be);
        if (sub == null) return localDir;
        final Pose3dc pose = sub.logicalPose();
        // Pose's transformNormal mutates an input Vector3d; build one and read back.
        final org.joml.Vector3d work = new org.joml.Vector3d(localDir.x, localDir.y, localDir.z);
        pose.transformNormal(work, work);
        return new Vec3(work.x, work.y, work.z);
    }

    // --- global → local ---

    /**
     * Transform a global Vec3 into the body-local frame of the sublevel
     * containing the given block-entity. Pass-through when not in a
     * sublevel.
     */
    public static Vec3 getShipVec(final Vec3 globalVec, final BlockEntity be) {
        final SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(be);
        if (sub == null) return globalVec;
        return sub.logicalPose().transformPositionInverse(globalVec);
    }

    /**
     * Transform a global direction vector into the body-local frame of
     * the sublevel containing the given block-entity. Pass-through when
     * not in a sublevel.
     */
    public static Vec3 getShipVecDirectionTransform(final Vec3 globalDir, final BlockEntity be) {
        final SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(be);
        if (sub == null) return globalDir;
        final Pose3dc pose = sub.logicalPose();
        final org.joml.Vector3d work = new org.joml.Vector3d(globalDir.x, globalDir.y, globalDir.z);
        pose.transformNormalInverse(work, work);
        return new Vec3(work.x, work.y, work.z);
    }
}
