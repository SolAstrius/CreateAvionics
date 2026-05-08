package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.arad.aradnetworks.RadarContactRegistry;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.block.radar.track.RadarTrackUtil;
import com.happysg.radar.block.radar.track.TrackCategory;
import com.happysg.radar.compat.physics.PhysicsHandler;
import com.happysg.radar.config.RadarConfig;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RadarScanningBlockBehavior extends BlockEntityBehaviour {

    public static final BehaviourType<RadarScanningBlockBehavior> TYPE = new BehaviourType<>();

    private int trackExpiration = 100;
    private int fov = RadarConfig.server().radarFOV.get();
    private int yRange = 20;
    private double range = RadarConfig.server().radarBaseRange.get();
    private double angle;
    private boolean running = false;
    private final SmartBlockEntity bearingEntity;
    private RadarBearingBlockEntity radarBearing;
    Vec3 scanPos = Vec3.ZERO;

    private final Set<Entity> scannedEntities = new HashSet<>();
    private final Set<SubLevelAccess> scannedSubLevels = new HashSet<>();
    private final Set<Projectile> scannedProjectiles = new HashSet<>();
    private final HashMap<String, RadarTrack> radarTracks = new HashMap<>();

    public RadarScanningBlockBehavior(final SmartBlockEntity be) {
        super(be);
        this.bearingEntity = be;
    }

    public void applyDetectionConfig(DetectionConfig cfg) {
        if (cfg == null) cfg = DetectionConfig.DEFAULT;
        setScanFlags(
                cfg.player(),
                cfg.vs2(),
                cfg.contraption(),
                cfg.mob(),
                cfg.animal(),
                cfg.projectile(),
                cfg.item()
        );
    }

    private boolean scanPlayers = true;
    private boolean scanSubLevels = true;
    private boolean scanContraptions = true;
    private boolean scanMobs = true;
    private boolean scanAnimals = true;
    private boolean scanProjectiles = true;
    private boolean scanItems = true;

    private boolean allowCategory(final TrackCategory c) {
        return switch (c) {
            case PLAYER -> scanPlayers;
            case SUBLEVEL -> scanSubLevels;
            case CONTRAPTION -> scanContraptions;
            case PROJECTILE -> scanProjectiles;
            case ITEM -> scanItems;
            case ANIMAL -> scanAnimals;
            case HOSTILE, MOB -> scanMobs;
            default -> true;
        };
    }

    private void pruneDisabledTracksNow() {
        radarTracks.entrySet().removeIf(e -> !allowCategory(e.getValue().trackCategory()));
    }

    public void setScanFlags(final boolean players, final boolean subLevels, final boolean contraptions,
                             final boolean mobs, final boolean animals, final boolean projectiles, final boolean items) {
        final boolean changed = players != scanPlayers || subLevels != scanSubLevels || contraptions != scanContraptions
                || mobs != scanMobs || animals != scanAnimals || projectiles != scanProjectiles || items != scanItems;

        this.scanPlayers = players;
        this.scanSubLevels = subLevels;
        this.scanContraptions = contraptions;
        this.scanMobs = mobs;
        this.scanAnimals = animals;
        this.scanProjectiles = projectiles;
        this.scanItems = items;

        if (changed) {
            pruneDisabledTracksNow();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;
        if (blockEntity.getLevel().getGameTime() % 5 != 1) return;

        removeDeadTracks();
        if (running) updateRadarTracks();
        if (running) {
            scannedEntities.clear();
            scannedSubLevels.clear();
            scannedProjectiles.clear();

            scanForEntityTracks();
            if (scanSubLevels) scanForSubLevelTracks();
        }
    }


    private void updateRadarTracks() {
        scanPos = PhysicsHandler.getWorldPos(bearingEntity).getCenter();
        final Level level = blockEntity.getLevel();
        if (level == null) return;
        final ServerLevel sl = level instanceof ServerLevel server ? server : null;

        for (final Entity entity : scannedEntities) {
            if (entity.isAlive() && isInFovAndRange(entity.position())) {
                radarTracks.compute(entity.getUUID().toString(), (id, track) -> {
                    if (track == null) return new RadarTrack(entity);
                    track.updateRadarTrack(entity);
                    return track;
                });

                if (entity instanceof final Projectile p) {
                    scannedProjectiles.add(p);
                }
            }
        }

        for (final SubLevelAccess subLevel : scannedSubLevels) {
            final Vec3 pos = RadarTrackUtil.getPosition(subLevel);
            if (isInFovAndRange(pos)) {
                radarTracks.compute(subLevel.getUniqueId().toString(), (id, track) -> {
                    if (track == null) return RadarTrackUtil.getRadarTrack(subLevel, level);
                    track.updateRadarTrack(subLevel, level);
                    return track;
                });
                if (sl != null) {
                    RadarContactRegistry.markInRange(sl, subLevel.getUniqueId(), 20);
                }
            }
        }
    }

    private boolean isInFovAndRange(final Vec3 target) {
        final double horizontalDistance = Math.sqrt(Math.pow(target.x() - scanPos.x(), 2) + Math.pow(target.z() - scanPos.z(), 2));
        final double verticalDistance = Math.abs(target.y() - scanPos.y());
        final double yScanRange = RadarConfig.server().radarYScanRange.get();

        if (horizontalDistance > range || verticalDistance > yScanRange) return false;
        if (horizontalDistance < 2) return true;

        double angleToEntity = Math.toDegrees(Math.atan2(target.x() - scanPos.x(), target.z() - scanPos.z()));
        angleToEntity = (angleToEntity + 360) % 360;
        double angleDiff = Math.abs(angleToEntity - angle);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        return angleDiff <= fov / 2.0;
    }

    private void removeDeadTracks() {
        // entities
        for (final Entity entity : scannedEntities) {
            if (!entity.isAlive()) {
                radarTracks.remove(entity.getUUID().toString());
            }
        }

        // sublevels: no per-tick "is this still alive" check — TTL expiration below
        // catches stale tracks once scanForSubLevelTracks stops re-detecting the
        // sublevel (e.g. it left scan range, was disassembled, or unloaded).

        // ttl expiration (works for everything: entities, sublevels, projectiles)
        final List<String> toRemove = new ArrayList<>();
        assert blockEntity.getLevel() != null;
        final long currentTime = blockEntity.getLevel().getGameTime();
        for (final RadarTrack track : radarTracks.values()) {
            if (currentTime - track.scannedTime() > trackExpiration) {
                toRemove.add(track.id());
            }
        }
        toRemove.forEach(radarTracks::remove);

        // projectiles
        scannedProjectiles.removeIf(p -> {
            final boolean dead = !p.isAlive();
            if (dead) radarTracks.remove(p.getUUID().toString());
            return dead;
        });
    }

    private void scanForEntityTracks() {
        final Level level = blockEntity.getLevel();
        if (level == null) return;

        final boolean scanAll = scanPlayers && scanContraptions && scanMobs && scanAnimals && scanProjectiles && scanItems;

        for (final AABB aabb : splitAABB(getRadarAABB(), 256)) {
            if (scanAll) {
                scannedEntities.addAll(level.getEntities(null, aabb));
                continue;
            }
            if (scanPlayers) scannedEntities.addAll(level.getEntitiesOfClass(Player.class, aabb));
            if (scanProjectiles) scannedEntities.addAll(level.getEntitiesOfClass(Projectile.class, aabb));
            if (scanItems) scannedEntities.addAll(level.getEntitiesOfClass(ItemEntity.class, aabb));
            if (scanContraptions) scannedEntities.addAll(level.getEntitiesOfClass(AbstractContraptionEntity.class, aabb));
            if (scanAnimals) scannedEntities.addAll(level.getEntitiesOfClass(Animal.class, aabb));
            if (scanMobs) {
                scannedEntities.addAll(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, aabb,
                        e -> !(e instanceof Animal)));
            }
        }
    }

    private void scanForSubLevelTracks() {
        final Level level = blockEntity.getLevel();
        if (level == null) return;

        // Don't list ourselves — skip the sublevel containing this radar's BE.
        final SubLevelAccess hostSubLevel = Sable.HELPER.getContaining(blockEntity);

        for (final AABB aabb : splitAABB(getRadarAABB(), 256)) {
            for (final SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(aabb))) {
                if (hostSubLevel != null && subLevel.getUniqueId().equals(hostSubLevel.getUniqueId())) continue;
                scannedSubLevels.add(subLevel);
            }
        }
    }

    private AABB getRadarAABB() {
        final BlockPos radarPos = PhysicsHandler.getWorldPos(blockEntity);
        final double x = radarPos.getX() + 0.5;
        final double y = radarPos.getY() + 0.5;
        final double z = radarPos.getZ() + 0.5;

        final double yScan = RadarConfig.server().radarYScanRange.get();
        final Level level = blockEntity.getLevel();
        final double minY = level != null ? Math.max(y - yScan, level.getMinBuildHeight()) : y - yScan;
        final double maxY = level != null ? Math.min(y + yScan, level.getMaxBuildHeight()) : y + yScan;

        return new AABB(x - range, minY, z - range, x + range, maxY, z + range);
    }

    public static List<AABB> splitAABB(final AABB aabb, final double maxSize) {
        final List<AABB> result = new ArrayList<>();
        for (double x = aabb.minX; x < aabb.maxX; x += maxSize) {
            for (double y = aabb.minY; y < aabb.maxY; y += maxSize) {
                for (double z = aabb.minZ; z < aabb.maxZ; z += maxSize) {
                    result.add(new AABB(x, y, z,
                            Math.min(x + maxSize, aabb.maxX),
                            Math.min(y + maxSize, aabb.maxY),
                            Math.min(z + maxSize, aabb.maxZ)));
                }
            }
        }
        return result;
    }

    @Override
    public void read(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if (nbt.contains("fov")) fov = nbt.getInt("fov");
        if (nbt.contains("yRange")) yRange = nbt.getInt("yRange");
        if (nbt.contains("range")) range = nbt.getDouble("range");
        if (nbt.contains("angle")) angle = nbt.getDouble("angle");
        if (nbt.contains("scanPosX")) scanPos = new Vec3(nbt.getDouble("scanPosX"), nbt.getDouble("scanPosY"), nbt.getDouble("scanPosZ"));
        if (nbt.contains("running")) running = nbt.getBoolean("running");
        if (nbt.contains("trackExpiration")) trackExpiration = nbt.getInt("trackExpiration");
    }

    @Override
    public void write(final CompoundTag nbt, final HolderLookup.Provider registries, final boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putInt("fov", fov);
        nbt.putInt("yRange", yRange);
        nbt.putDouble("range", range);
        nbt.putDouble("angle", angle);
        nbt.putDouble("scanPosX", scanPos.x);
        nbt.putDouble("scanPosY", scanPos.y);
        nbt.putDouble("scanPosZ", scanPos.z);
        nbt.putBoolean("running", running);
        nbt.putInt("trackExpiration", trackExpiration);
    }

    public void setFov(final int fov) { this.fov = fov; }
    public void setYRange(final int yRange) { this.yRange = yRange; }
    public void setRange(final double range) { this.range = range; }
    public void setAngle(final double angle) { this.angle = angle; }
    public void setScanPos(final Vec3 scanPos) { this.scanPos = scanPos; }
    public void setRunning(final boolean running) { this.running = running; }
    public void setTrackExpiration(final int trackExpiration) { this.trackExpiration = trackExpiration; }

    public Collection<RadarTrack> getRadarTracks() {
        return radarTracks.values();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public float getAngle() {
        return (float) angle;
    }
}
