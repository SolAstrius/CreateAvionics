package ink.astrius.create_avionics.content.radar;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import ink.astrius.create_avionics.content.controller.ShipControllers;
import ink.astrius.create_avionics.content.controller.ShipScan;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Radar Mk.1: a single narrow beam straight out of the radar's face. Twice a second it casts the
 * beam and records the first thing it meets: another ship (named, if that ship's transponder is
 * on), terrain, or the radar's own ship blocking its face.
 * <p>
 * The beam is an ordinary block raycast; Sable makes it work from a moving ship and hit other
 * ships' blocks, the same way Simulated's laser sensor sees them.
 */
public class RadarBlockEntity extends BlockEntity {

    public static final int SWEEP_INTERVAL = 10;
    public static final double RANGE = 256;

    public enum Kind {
        SHIP, TERRAIN, OBSTRUCTED
    }

    /**
     * @param ship   the hit ship's id, for tracking closing speed; null for terrain
     * @param name   what the ship's transponder says; empty if it has none on, or for terrain
     */
    public record Contact(Kind kind, double distance, @Nullable UUID ship, Optional<String> name,
                          double closingSpeed, long time) {
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
        final Vec3 end = start.add(normal.scale(RANGE));
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
        final UUID id = kind == Kind.SHIP ? target.getUniqueId() : null;
        final Optional<String> name = id == null ? Optional.empty() : ShipControllers.transponder(id, target.getName());

        // Closing speed, only while the beam stays on the same thing
        final Contact previous = this.contact;
        double closing = 0;
        if (previous != null && previous.kind() == kind && Objects.equals(previous.ship(), id)) {
            final double seconds = (level.getGameTime() - previous.time()) / 20.0;
            if (seconds > 0) {
                closing = (previous.distance() - distance) / seconds;
            }
        }
        this.contact = new Contact(kind, distance, id, name, closing, level.getGameTime());
    }

    /** Comparator strength: 15 right in front of the radar, falling to 1 at full range; 0 for nothing. */
    public int comparatorSignal() {
        final Contact c = this.contact;
        if (c == null || c.kind() == Kind.OBSTRUCTED) {
            return 0;
        }
        return Math.max(1, 15 - (int) (c.distance() / RANGE * 15));
    }
}
