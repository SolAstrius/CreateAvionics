package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.VelocitySensorPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The controller's place on a ComputerCraft wired network. Cables and modems connect to it like
 * to any wired block, and the network then sees the ship's peripherals as if each were cabled
 * in: the same peripheral objects and types, under {@code ship_<type>_<n>} names that stay with
 * each block. Velocity sensors are named by role instead, e.g. {@code ship_velocity_sensor_forward}.
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
    public void publish(final List<ShipScan.Found> found) {
        this.update(this.assign(found));
    }

    /** Withdraw everything this controller published; it now stands by and mirrors the leader's names. */
    public void standDown(final Map<BlockPos, String> leaderNames) {
        this.names.clear();
        this.names.putAll(leaderNames);
        this.update(Map.of());
    }

    /** Names this controller assigned (as leader) or mirrors (standing by), by block position. */
    public Map<BlockPos, String> names() {
        return this.names;
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
            if (!bridgeable(f.peripheral())) {
                continue;
            }
            // Reuse the objects CC already has; only a replaced block gets a new one
            keep.put(f.blockEntity(), this.stable.getOrDefault(f.blockEntity(), f.peripheral()));
            final String old = this.names.get(f.pos());
            final String base = this.base(f.peripheral());
            if (old != null && (old.equals(base) || old.startsWith(base + "_"))) {
                kept.put(f.pos(), old);
                taken.add(old);
            } else {
                fresh.add(f);
            }
        }
        for (final ShipScan.Found f : fresh) {
            final String base = this.base(f.peripheral());
            final boolean role = f.peripheral() instanceof VelocitySensorPeripheral;
            int n = 0;
            while (taken.contains(numbered(base, n, role))) {
                n++;
            }
            final String name = numbered(base, n, role);
            kept.put(f.pos(), name);
            taken.add(name);
        }
        this.stable.clear();
        this.stable.putAll(keep);
        this.names.clear();
        this.names.putAll(kept);

        final Map<String, IPeripheral> out = new LinkedHashMap<>();
        for (final ShipScan.Found f : found) {
            final String name = kept.get(f.pos());
            if (name != null) {
                out.put(name, keep.get(f.blockEntity()));
            }
        }
        return out;
    }

    /**
     * The name a peripheral is numbered under: {@code ship_<type>}, except velocity sensors, which
     * are named by the axis they measure relative to the controller's facing:
     * {@code ship_velocity_sensor_forward}, {@code _vertical} or {@code _lateral}.
     */
    private String base(final IPeripheral peripheral) {
        if (peripheral instanceof final VelocitySensorPeripheral velocity) {
            final Direction.Axis axis = Direction.Axis.byName(velocity.getAxis());
            final Direction forward = this.owner.forward();
            final String role = axis == Direction.Axis.Y ? "vertical" : axis == forward.getAxis() ? "forward" : "lateral";
            return "ship_" + peripheral.getType() + "_" + role;
        }
        return "ship_" + peripheral.getType();
    }

    /** Role names start bare ({@code _forward}, then {@code _forward_1}); others count from {@code _0}. */
    private static String numbered(final String base, final int n, final boolean role) {
        return role && n == 0 ? base : base + "_" + n;
    }

    /** Modems, computers and turtles network themselves; bridging them would loop or mirror a computer to itself. */
    private static boolean bridgeable(final IPeripheral peripheral) {
        return switch (peripheral.getType()) {
            case "modem", "computer", "turtle" -> false;
            default -> true;
        };
    }

    public void loadNames(final Map<BlockPos, String> saved) {
        this.names.clear();
        this.names.putAll(saved);
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
