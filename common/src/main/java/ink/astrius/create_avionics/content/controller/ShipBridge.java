package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
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
 * in: the same peripheral objects, under {@code type_N} names numbered by position on the ship,
 * so every controller on the ship would publish the same names.
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

    /** Publish the ship's peripherals, or nothing when another controller is publishing. */
    public void publish(final List<ShipScan.Found> found, final boolean leader) {
        final Map<String, IPeripheral> next = leader ? this.names(found) : Map.of();
        if (!next.equals(this.published)) {
            this.published = next;
            this.getNode().updatePeripherals(next);
        }
    }

    private Map<String, IPeripheral> names(final List<ShipScan.Found> found) {
        // Reuse the objects CC already has; only a replaced block gets a new one
        final Map<BlockEntity, IPeripheral> keep = new HashMap<>();
        final Map<String, List<IPeripheral>> byType = new LinkedHashMap<>();
        for (final ShipScan.Found f : found) {
            // A ship's own modems already make their own networks; bridging them would loop
            if ("modem".equals(f.peripheral().getType())) {
                continue;
            }
            final IPeripheral peripheral = this.stable.getOrDefault(f.blockEntity(), f.peripheral());
            keep.put(f.blockEntity(), peripheral);
            byType.computeIfAbsent(peripheral.getType(), t -> new ArrayList<>()).add(peripheral);
        }
        this.stable.clear();
        this.stable.putAll(keep);
        final Map<String, IPeripheral> names = new LinkedHashMap<>();
        byType.forEach((type, list) -> {
            for (int i = 0; i < list.size(); i++) {
                names.put(type + "_" + i, list.get(i));
            }
        });
        return names;
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
