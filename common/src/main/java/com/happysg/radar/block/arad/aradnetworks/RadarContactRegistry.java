package com.happysg.radar.block.arad.aradnetworks;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class RadarContactRegistry {
    private RadarContactRegistry() {}

    public static void markInRange(final ServerLevel level, final UUID subLevelId, final int ttlTicks) {
        RadarContactRegistryData.get(level).markInRange(subLevelId, ttlTicks);
    }

    public static void markLocked(final ServerLevel level, final UUID subLevelId, final int ttlTicks) {
        RadarContactRegistryData.get(level).markLocked(subLevelId, ttlTicks);
    }

    public static boolean isInRange(final ServerLevel level, final UUID subLevelId) {
        return RadarContactRegistryData.get(level).isInRange(subLevelId);
    }

    public static boolean isLocked(final ServerLevel level, final UUID subLevelId) {
        return RadarContactRegistryData.get(level).isLocked(subLevelId);
    }

    public static void unLock(final ServerLevel level, final UUID subLevelId) {
        RadarContactRegistryData.get(level).unlock(subLevelId);
    }

    public static void tickDecay(final ServerLevel level) {
        RadarContactRegistryData.get(level).tickDecay();
    }
}
