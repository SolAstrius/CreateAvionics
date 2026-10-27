package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserBlockEntity;
import ink.astrius.create_avionics.compat.simulated.peripherals.VelocitySensorPeripheral;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.content.radar.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The controller's place on a ComputerCraft wired network. Cables and modems connect to it like
 * to any wired block, and the network then sees the ship's peripherals as if each were cabled
 * in: the same peripheral objects and types, under {@code ship_<type>_<n>} names that stay with
 * each block. Directional sensors carry their role, e.g. {@code ship_velocity_sensor_forward_0}.
 * <p>
 * Only one controller per ship publishes (see {@link ShipControllers}); the rest join its
 * network and stand by, so a computer on any of them reaches the whole ship.
 */
public class ShipBridge implements WiredElement {

    /** The wired element on a neighbouring block, or null. Set by the loader, like {@link ShipScan#lookup}. */
    public static TriFunction elementAt = (level, pos, side) -> null;

    @FunctionalInterface
    public interface TriFunction {
        @Nullable WiredElement apply(Level level, BlockPos pos, Direction side);
    }

    private final AvionicsControllerBlockEntity owner;
    private @Nullable WiredNode node;
    private Map<String, IPeripheral> published = Map.of();
    /** Peripheral objects handed to CC, kept per block entity so a rescan doesn't swap them. */
    private final Map<BlockEntity, IPeripheral> stable = new HashMap<>();
    private final Set<WiredNode> peers = new HashSet<>();
    /** Ship peripheral names by block position; saved with the controller so they survive reloads. */
    private final Map<BlockPos, String> names = new HashMap<>();
    /** The publishing controller's own peripheral (the transponder switch), the same name on every ship. */
    public static final String CONTROLLER_NAME = "ship_avionics_controller";
    /** The bow the position names were given under; null for names saved before it was recorded. */
    private @Nullable Direction namedFor;
    /** The ship's extent, for position names; set on each publish. */
    private @Nullable BoundingBox3ic bounds;

    /** Pure transmission (shafts, cogwheels, gearboxes...): nothing to ask, and a ship has hundreds. */
    public static final TagKey<Block> NOT_BRIDGED = tag("not_bridged");
    /** Named by where on the ship they sit (propellers, engines, burners), e.g. {@code ship_propeller_bearing_aft_port_0}. */
    public static final TagKey<Block> NAMED_BY_POSITION = tag("named_by_position");

    private static TagKey<Block> tag(final String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreateAvionics.MOD_ID, name));
    }

    public ShipBridge(final AvionicsControllerBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public WiredNode getNode() {
        if (this.node == null) {
            this.node = ComputerCraftAPI.createWiredNodeForElement(this);
        }
        return this.node;
    }

    @Override
    public Level getLevel() {
        return this.owner.getLevel();
    }

    @Override
    public Vec3 getPosition() {
        return Vec3.atCenterOf(this.owner.getBlockPos());
    }

    @Override
    public String getSenderID() {
        return "avionics_controller";
    }

    /** Connect to the wired blocks around the controller (cables, modems, computers). */
    public void connectNeighbours() {
        final Level level = this.owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (final Direction side : Direction.values()) {
            final BlockPos pos = this.owner.getBlockPos().relative(side);
            if (!level.isLoaded(pos)) {
                continue;
            }
            final WiredElement neighbour = elementAt.apply(level, pos, side.getOpposite());
            if (neighbour != null) {
                this.getNode().connectTo(neighbour.getNode());
            }
        }
    }

    /** Join the other controllers on the ship, and drop the ones that left it. */
    public void linkPeers(final List<AvionicsControllerBlockEntity> controllers) {
        final Set<WiredNode> now = new HashSet<>();
        for (final AvionicsControllerBlockEntity other : controllers) {
            if (other != this.owner) {
                now.add(other.bridge().getNode());
            }
        }
        for (final WiredNode gone : this.peers) {
            if (!now.contains(gone)) {
                this.getNode().disconnectFrom(gone);
            }
        }
        for (final WiredNode peer : now) {
            this.getNode().connectTo(peer);
        }
        this.peers.clear();
        this.peers.addAll(now);
    }

    /**
     * Publish the ship's peripherals. Only the leader may call this: CC removes a withdrawn name
     * from the whole network whichever node published it, so the leader first makes every other
     * controller {@link #standDown}, then publishes.
     */
    public void publish(final List<ShipScan.Found> found, final BoundingBox3ic bounds) {
        this.bounds = bounds;
        this.update(this.assign(found));
    }

    /** Withdraw everything this controller published; it now stands by and mirrors the leader's names. */
    public void standDown(final Map<BlockPos, String> leaderNames, final @Nullable Direction leaderNamedFor) {
        this.loadNames(leaderNames, leaderNamedFor);
        this.update(Map.of());
    }

    /** Names this controller assigned (as leader) or mirrors (standing by), by block position. */
    public Map<BlockPos, String> names() {
        return this.names;
    }

    /** The bow the position names were given under. */
    public @Nullable Direction namedFor() {
        return this.namedFor;
    }

    private void update(final Map<String, IPeripheral> next) {
        if (!next.equals(this.published)) {
            this.published = next;
            this.getNode().updatePeripherals(next);
            this.owner.setChanged();
        }
    }

    /**
     * Give every bridgeable peripheral a name. A block keeps its name for as long as it stays on
     * the ship; a new one takes the lowest free number for its type. Names carry a {@code ship_}
     * prefix so they can't collide with the names CC gives modem-shared peripherals.
     */
    private Map<String, IPeripheral> assign(final List<ShipScan.Found> found) {
        final Map<BlockEntity, IPeripheral> keep = new HashMap<>();
        final Map<BlockPos, String> kept = new HashMap<>();
        final List<ShipScan.Found> fresh = new ArrayList<>();
        final Set<String> taken = new HashSet<>();
        for (final ShipScan.Found f : found) {
            if (!bridgeable(f)) {
                continue;
            }
            // Reuse the objects CC already has; only a replaced block gets a new one
            keep.put(f.blockEntity(), this.stable.getOrDefault(f.blockEntity(), f.peripheral()));
            final String old = this.names.get(f.pos());
            final String base = this.base(f);
            if (old != null && this.stillFits(f, old, base)) {
                kept.put(f.pos(), old);
                taken.add(old);
            } else {
                fresh.add(f);
            }
        }
        for (final ShipScan.Found f : fresh) {
            final String base = this.base(f);
            int n = 0;
            while (taken.contains(base + "_" + n)) {
                n++;
            }
            final String name = base + "_" + n;
            kept.put(f.pos(), name);
            taken.add(name);
        }
        this.stable.clear();
        this.stable.putAll(keep);
        this.names.clear();
        this.names.putAll(kept);
        this.namedFor = this.owner.forward();

        final Map<String, IPeripheral> out = new LinkedHashMap<>();
        for (final ShipScan.Found f : found) {
            final String name = kept.get(f.pos());
            if (name != null) {
                out.put(name, keep.get(f.blockEntity()));
            }
        }
        // The transponder switch is ship-wide, so the ship has one, whichever controller publishes
        out.put(CONTROLLER_NAME, this.owner.peripheral());
        return out;
    }

    /**
     * The name a peripheral is numbered under. Usually {@code ship_<type>}; peripherals that
     * measure in a direction are named by that direction relative to the controller's facing:
     * velocity sensors by axis ({@code ship_velocity_sensor_forward}, {@code _vertical},
     * {@code _lateral}; an axis, not a side), laser and optical sensors by where the beam points,
     * docking connectors by the side they face ({@code _fore}, {@code _aft}, {@code _port},
     * {@code _starboard}, {@code _up}, {@code _down}).
     */
    private String base(final ShipScan.Found found) {
        final IPeripheral peripheral = found.peripheral();
        final String base = "ship_" + peripheral.getType();
        final Direction forward = this.owner.forward();
        if (peripheral instanceof final VelocitySensorPeripheral velocity) {
            final Direction.Axis axis = Direction.Axis.byName(velocity.getAxis());
            return base + "_" + (axis == Direction.Axis.Y ? "vertical" : axis == forward.getAxis() ? "forward" : "lateral");
        }
        if (found.blockEntity() instanceof final AbstractLaserBlockEntity laser) {
            return base + "_" + side(laser.getDirection(), forward);
        }
        if (found.blockEntity() instanceof final RadarBlockEntity radar) {
            return base + "_" + side(radar.facing(), forward);
        }
        final BlockState state = found.blockEntity().getBlockState();
        if ("docking_connector".equals(peripheral.getType()) && state.hasProperty(BlockStateProperties.FACING)) {
            return base + "_" + side(state.getValue(BlockStateProperties.FACING), forward);
        }
        if (state.is(NAMED_BY_POSITION) && this.bounds != null) {
            return base + zone(found.pos(), this.bounds, forward);
        }
        return base;
    }

    /**
     * Where on the ship a block sits: {@code _fore} or {@code _aft} outside the middle third of
     * its length, then {@code _port} or {@code _starboard} off the centreline. Neutral parts are
     * left out, so a central block gets no zone at all.
     */
    static String zone(final BlockPos pos, final BoundingBox3ic bounds, final Direction forward) {
        final Direction starboard = forward.getClockWise();
        final double cx = (bounds.minX() + bounds.maxX()) / 2.0;
        final double cz = (bounds.minZ() + bounds.maxZ()) / 2.0;
        final double dx = pos.getX() - cx;
        final double dz = pos.getZ() - cz;
        final double along = dx * forward.getStepX() + dz * forward.getStepZ();
        final double across = dx * starboard.getStepX() + dz * starboard.getStepZ();
        final double halfLength = (forward.getAxis() == Direction.Axis.X
                ? bounds.maxX() - bounds.minX() : bounds.maxZ() - bounds.minZ()) / 2.0;
        final StringBuilder zone = new StringBuilder();
        if (along > halfLength / 3) {
            zone.append("_fore");
        } else if (along < -halfLength / 3) {
            zone.append("_aft");
        }
        // Half a block off-centre already counts: on an even-width ship the centreline runs between blocks
        if (across >= 0.5) {
            zone.append("_starboard");
        } else if (across <= -0.5) {
            zone.append("_port");
        }
        return zone.toString();
    }

    /**
     * Whether a block may keep the name it has. Position names stay put when the ship grows (the
     * block hasn't moved, only the hull around it) but are zoned again when the bow changes, since
     * fore, aft, port and starboard now mean other places; every other name must still match the role.
     */
    private boolean stillFits(final ShipScan.Found found, final String old, final String base) {
        if (found.blockEntity().getBlockState().is(NAMED_BY_POSITION)) {
            if (this.namedFor != null && this.namedFor != this.owner.forward()) {
                return false;
            }
            final String type = "ship_" + found.peripheral().getType();
            return old.matches(Pattern.quote(type) + "(_fore|_aft)?(_port|_starboard)?_\\d+");
        }
        return old.startsWith(base + "_") && old.substring(base.length() + 1).matches("\\d+");
    }

    /**
     * Which side of the ship a direction points to, in ship terms with the controller's facing as
     * the bow: fore, aft, port (left), starboard (right), up or down.
     */
    private static String side(final Direction direction, final Direction forward) {
        if (direction == Direction.UP) {
            return "up";
        }
        if (direction == Direction.DOWN) {
            return "down";
        }
        if (direction == forward) {
            return "fore";
        }
        if (direction == forward.getOpposite()) {
            return "aft";
        }
        return direction == forward.getClockWise() ? "starboard" : "port";
    }

    /**
     * Modems, computers and turtles network themselves (bridging them would loop or mirror a
     * computer to itself), a multi-block monitor is bridged once (its origin block), controllers are published once as {@link #CONTROLLER_NAME} (the
     * transponder is ship-wide, so which one leads mustn't change what computers see), and blocks tagged
     * {@code create_avionics:not_bridged} are pure transmission.
     */
    private static boolean bridgeable(final ShipScan.Found found) {
        if (found.blockEntity().getBlockState().is(NOT_BRIDGED)) {
            return false;
        }
        // A multi-block monitor is one screen: bridge its origin block only
        if (found.blockEntity() instanceof final MonitorBlockEntity monitor
                && (monitor.getXIndex() != 0 || monitor.getYIndex() != 0)) {
            return false;
        }
        return switch (found.peripheral().getType()) {
            case "modem", "computer", "turtle", "avionics_controller" -> false;
            default -> true;
        };
    }

    public void loadNames(final Map<BlockPos, String> saved, final @Nullable Direction savedFor) {
        if (!this.names.equals(saved) || this.namedFor != savedFor) {
            this.names.clear();
            this.names.putAll(saved);
            this.namedFor = savedFor;
            this.owner.setChanged();
        }
    }

    /** Leave the network: called when the controller is removed or unloaded. */
    public void remove() {
        if (this.node != null) {
            this.node.remove();
            this.node = null;
        }
        this.published = Map.of();
        this.peers.clear();
        this.stable.clear();
    }
}
