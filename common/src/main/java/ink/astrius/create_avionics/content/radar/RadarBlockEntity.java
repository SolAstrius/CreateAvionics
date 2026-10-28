package ink.astrius.create_avionics.content.radar;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import ink.astrius.create_avionics.content.controller.ShipScan;
import ink.astrius.create_avionics.content.radio.SignalModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Radar Mk.1: a single narrow beam straight out of the radar's face. Twice a second it casts the
 * beam and records the first thing it meets: another ship, terrain, or the radar's own ship
 * blocking its face. It is a primary radar: it hears only its echo, which says how far and how
 * big, never who. Identities come from the controller's radio.
 * <p>
 * The beam is an ordinary block raycast; Sable makes it work from a moving ship and hit other
 * ships' blocks, the same way Simulated's laser sensor sees them. Whether the hit counts as a
 * contact is up to the {@link SignalModel}: the echo has to clear the noise floor.
 */
public class RadarBlockEntity extends BlockEntity {

    public static final int SWEEP_INTERVAL = 10;
    /** How far the beam is cast, for performance; the echo usually fades out well before this. */
    public static final double RAY_LENGTH = 512;
    /** Cross-section of terrain, which has no bounding box: about a 20x20 face. */
    public static final double TERRAIN_CROSS_SECTION = 400;
    /** The cross-section {@link #typicalRange} is quoted for: a ship with a 10x10 face to the beam. */
    public static final double TYPICAL_CROSS_SECTION = 100;

    public enum Kind {
        SHIP, TERRAIN, OBSTRUCTED
    }

    /**
     * @param ship   the hit ship's id, for tracking closing speed; null for terrain
     * @param signal the echo's strength, dBm
     * @param range  how far an echo off this target could still be heard, for the comparator
     */
    public record Contact(Kind kind, double distance, @Nullable UUID ship, double closingSpeed,
                          double signal, double range, long time) {
    }

    /** How far a typical ship shows up in clear weather, in blocks. */
    public static double typicalRange() {
        return Math.min(RAY_LENGTH, SignalModel.current.echoRange(TYPICAL_CROSS_SECTION));
    }

    private volatile @Nullable Contact contact;
    private @Nullable RadarPeripheral peripheral;

    public RadarBlockEntity(final BlockPos pos, final BlockState state) {
        super(Radars.BLOCK_ENTITY.get(), pos, state);
    }

    public RadarPeripheral peripheral() {
        if (this.peripheral == null) {
            this.peripheral = new RadarPeripheral(this);
        }
        return this.peripheral;
    }

    /** The latest return, or null if the beam met nothing within range. */
    public @Nullable Contact contact() {
        return this.contact;
    }

    public Direction facing() {
        return this.getBlockState().getValue(RadarBlock.FACING);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final RadarBlockEntity be) {
        if ((level.getGameTime() + pos.hashCode()) % SWEEP_INTERVAL == 0) {
            be.sweep(level);
        }
    }

    private void sweep(final Level level) {
        final int signal = this.comparatorSignal();
        this.cast(level);
        if (this.comparatorSignal() != signal) {
            level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    private void cast(final Level level) {
        final Direction facing = this.facing();
        final Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        // Start just outside the radar's face so it doesn't see itself
        final Vec3 start = Vec3.atCenterOf(this.worldPosition).add(normal.scale(0.51));
        final Vec3 end = start.add(normal.scale(RAY_LENGTH));
        final BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS) {
            this.contact = null;
            return;
        }
        final double distance = Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, start, hit.getLocation()));
        final SubLevel own = ShipScan.shipOf(this);
        final SubLevel target = Sable.HELPER.getContaining(level, hit.getLocation());
        final Kind kind;
        if (target == null) {
            kind = Kind.TERRAIN;
        } else if (own != null && own.getUniqueId().equals(target.getUniqueId())) {
            kind = Kind.OBSTRUCTED;
        } else {
            kind = Kind.SHIP;
        }

        // The echo, between world positions
        final Vec3 from = Sable.HELPER.projectOutOfSubLevel(level, start);
        final Vec3 to = Sable.HELPER.projectOutOfSubLevel(level, hit.getLocation());
        final Vec3 beam = Sable.HELPER.projectOutOfSubLevel(level, end).subtract(from).normalize();
        final double sigma = target == null ? TERRAIN_CROSS_SECTION : crossSection(target.boundingBox(), beam);
        final SignalModel model = SignalModel.current;
        final double signal = model.echo(level, from, to, sigma);
        if (signal < model.noiseFloor() && kind != Kind.OBSTRUCTED) {
            this.contact = null;
            return;
        }
        final UUID id = kind == Kind.SHIP ? target.getUniqueId() : null;

        // Closing speed, only while the beam stays on the same thing
        final Contact previous = this.contact;
        double closing = 0;
        if (previous != null && previous.kind() == kind && Objects.equals(previous.ship(), id)) {
            final double seconds = (level.getGameTime() - previous.time()) / 20.0;
            if (seconds > 0) {
                closing = (previous.distance() - distance) / seconds;
            }
        }
        final double range = Math.min(RAY_LENGTH, model.echoRange(sigma));
        this.contact = new Contact(kind, distance, id, closing, signal, range, level.getGameTime());
    }

    /**
     * The area a ship's world bounding box shows the beam: each face's area weighted by how
     * squarely the beam meets it, which is the box's exact shadow along the beam.
     */
    static double crossSection(final BoundingBox3dc box, final Vec3 beam) {
        final double dx = box.maxX() - box.minX();
        final double dy = box.maxY() - box.minY();
        final double dz = box.maxZ() - box.minZ();
        return Math.abs(beam.x) * dy * dz + Math.abs(beam.y) * dx * dz + Math.abs(beam.z) * dx * dy;
    }

    /**
     * Comparator strength: 15 right in front of the radar, falling to 1 as far out as this target
     * could be seen; 0 for nothing.
     */
    public int comparatorSignal() {
        final Contact c = this.contact;
        if (c == null || c.kind() == Kind.OBSTRUCTED) {
            return 0;
        }
        return Math.max(1, 15 - (int) (c.distance() / c.range() * 15));
    }
}
