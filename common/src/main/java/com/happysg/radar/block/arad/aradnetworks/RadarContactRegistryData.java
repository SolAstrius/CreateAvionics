package com.happysg.radar.block.arad.aradnetworks;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class RadarContactRegistryData extends SavedData {

    public static final int DEFAULT_IN_RANGE_TTL = 20; // 1s
    public static final int DEFAULT_LOCK_TTL = 10;     // 0.5s

    private static final String DATA_NAME = "create_radar_contact_registry";

    private final Map<UUID, Entry> entries = new HashMap<>();

    // ===== access =====

    public static RadarContactRegistryData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        RadarContactRegistryData::new,
                        RadarContactRegistryData::load,
                        null
                ),
                DATA_NAME
        );
    }

    // ===== model =====

    public enum RadarContactState {
        IN_RANGE,
        LOCKED
    }

    public static class Entry {
        public int inRangeTtl;
        public int lockedTtl;

        public Entry(final int inRangeTtl, final int lockedTtl) {
            this.inRangeTtl = inRangeTtl;
            this.lockedTtl = lockedTtl;
        }
    }

    // ===== core API (range/lock) =====

    public void markInRange(final UUID subLevelId, int ttlTicks) {
        if (ttlTicks <= 0) ttlTicks = DEFAULT_IN_RANGE_TTL;

        final Entry e = entries.get(subLevelId);
        if (e == null) {
            entries.put(subLevelId, new Entry(ttlTicks, 0));
        } else {
            e.inRangeTtl = Math.max(e.inRangeTtl, ttlTicks);
        }
        setDirty();
    }

    public void markLocked(final UUID subLevelId, int ttlTicks) {
        if (ttlTicks <= 0) ttlTicks = DEFAULT_LOCK_TTL;

        final Entry e = entries.get(subLevelId);
        if (e == null) {
            entries.put(subLevelId, new Entry(0, ttlTicks));
        } else {
            e.lockedTtl = Math.max(e.lockedTtl, ttlTicks);
        }
        setDirty();
    }

    public boolean isInRange(final UUID subLevelId) {
        final Entry e = entries.get(subLevelId);
        return e != null && e.inRangeTtl > 0;
    }

    public boolean isLocked(final UUID subLevelId) {
        final Entry e = entries.get(subLevelId);
        return e != null && e.lockedTtl > 0;
    }

    // highest state wins
    public RadarContactState getState(final UUID subLevelId) {
        if (isLocked(subLevelId)) return RadarContactState.LOCKED;
        if (isInRange(subLevelId)) return RadarContactState.IN_RANGE;
        return null;
    }

    public void tickDecay() {
        if (entries.isEmpty()) return;

        boolean changed = false;
        final Iterator<Map.Entry<UUID, Entry>> it = entries.entrySet().iterator();

        while (it.hasNext()) {
            final Map.Entry<UUID, Entry> me = it.next();
            final Entry e = me.getValue();

            if (e.inRangeTtl > 0) e.inRangeTtl--;
            if (e.lockedTtl > 0) e.lockedTtl--;

            if (e.inRangeTtl <= 0 && e.lockedTtl <= 0) {
                it.remove();
            }
            changed = true;
        }

        if (changed) setDirty();
    }

    public void unlock(final UUID subLevelId) {
        final Entry e = entries.get(subLevelId);
        if (e == null) return;

        if (e.lockedTtl != 0) {
            e.lockedTtl = 0;
            if (e.inRangeTtl <= 0) {
                entries.remove(subLevelId);
            }
            setDirty();
        }
    }

    // ===== persistence =====

    public static RadarContactRegistryData load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final RadarContactRegistryData data = new RadarContactRegistryData();

        final CompoundTag subsTag = tag.getCompound("SubLevels");
        for (final String key : subsTag.getAllKeys()) {
            try {
                final UUID id = UUID.fromString(key);
                final CompoundTag eTag = subsTag.getCompound(key);
                final int inRange = eTag.getInt("InRange");
                final int locked = eTag.getInt("Locked");

                if (inRange > 0 || locked > 0) {
                    data.entries.put(id, new Entry(inRange, locked));
                }
            } catch (final IllegalArgumentException ignored) {
                // skip malformed UUID keys
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag subsTag = new CompoundTag();

        for (final var e : entries.entrySet()) {
            final Entry entry = e.getValue();
            if (entry.inRangeTtl <= 0 && entry.lockedTtl <= 0) continue;

            final CompoundTag eTag = new CompoundTag();
            eTag.putInt("InRange", entry.inRangeTtl);
            eTag.putInt("Locked", entry.lockedTtl);
            subsTag.put(e.getKey().toString(), eTag);
        }
        tag.put("SubLevels", subsTag);
        return tag;
    }
}
