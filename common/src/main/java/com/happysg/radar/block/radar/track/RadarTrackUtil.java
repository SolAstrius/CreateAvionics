package com.happysg.radar.block.radar.track;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RadarTrackUtil {

    public static RadarTrack getRadarTrack(final SubLevelAccess subLevel, final Level level) {
        return new RadarTrack(
                subLevel.getUniqueId().toString(),
                getPosition(subLevel),
                getVelocity(subLevel),
                level.getGameTime(),
                TrackCategory.SUBLEVEL,
                "sable:sublevel",
                getSubLevelHeight(subLevel)
        );
    }

    /** Approximate "size" of a sublevel — uses the global bounding-box height. */
    public static float getSubLevelHeight(final SubLevelAccess subLevel) {
        return (float) subLevel.boundingBox().height();
    }

    /**
     * Sublevel velocity in blocks-per-second (matches the unit convention
     * the radar previously expected from VS2's per-second velocity API).
     * Sable's RigidBodyHandle reports per-tick, so we scale by 20.
     */
    public static Vec3 getVelocity(final SubLevelAccess subLevel) {
        if (!(subLevel instanceof final ServerSubLevel server)) return Vec3.ZERO;
        final Vector3d v = RigidBodyHandle.of(server).getLinearVelocity(new Vector3d());
        return new Vec3(v.x * 20.0, v.y * 20.0, v.z * 20.0);
    }

    /** Global-space center of the sublevel's bounding box. */
    public static Vec3 getPosition(final SubLevelAccess subLevel) {
        final Vector3d c = subLevel.boundingBox().center();
        return new Vec3(c.x, c.y, c.z);
    }


    public static CompoundTag serializeNBTList(final Collection<RadarTrack> tracks) {
        final ListTag list = new ListTag();
        for (final RadarTrack track : tracks) {
            list.add(track.serializeNBT());
        }
        final CompoundTag tag = new CompoundTag();
        tag.put("tracks", list);
        return tag;
    }

    public static List<RadarTrack> deserializeListNBT(final CompoundTag tag) {
        final List<RadarTrack> tracks = new ArrayList<>();
        final ListTag list = tag.getList("tracks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            tracks.add(RadarTrack.deserializeNBT(list.getCompound(i)));
        }
        return tracks;
    }
}
