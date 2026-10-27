package ink.astrius.create_avionics.content.controller;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Keeps track of the ship the controller is on: rescans it for sensors every couple of seconds.
 * The channels are for gauges, which can't combine sensor readings themselves; computers get
 * the sensors directly instead, through the controller's wired bridge, and do their own maths.
 */
public class AvionicsControllerBlockEntity extends BlockEntity {

    public static final int SCAN_INTERVAL = 40;

    private final ShipBridge bridge = new ShipBridge(this);
    private @Nullable Direction bow;
    private long bowSetAt = Long.MIN_VALUE;
    private @Nullable UUID placer;
    private @Nullable AvionicsControllerPeripheral peripheral;
    /** Scan on the first tick after loading. */
    private boolean rescanSoon = true;
    private @Nullable UUID ship;
    private List<ShipScan.Found> found = List.of();
    private volatile ChannelMap channels = new ChannelMap(List.of(), Direction.NORTH);

    public AvionicsControllerBlockEntity(final BlockPos pos, final BlockState state) {
        super(Controllers.BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * The ship's bow, in the ship's own frame. It belongs to the ship, not to this controller: the
     * first controller placed on a ship sets it to its facing, later ones adopt it, and a wrench
     * moves it on purpose. Every controller keeps a copy with the time it was set, and the newest
     * wins, so losing any one controller changes nothing.
     */
    public Direction forward() {
        return this.bow != null ? this.bow : this.facing();
    }

    public Direction facing() {
        return this.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /** Make this controller's facing the ship's bow (the wrench action). */
    public void takeBow() {
        if (this.level == null) {
            return;
        }
        this.bow = this.facing();
        this.bowSetAt = this.level.getGameTime();
        this.setChanged();
        this.rescanSoon();
        if (this.ship != null) {
            for (final AvionicsControllerBlockEntity other : ShipControllers.on(this.ship)) {
                other.rescanSoon();
            }
        }
    }

    public boolean transponderOn() {
        return this.getBlockState().getValue(AvionicsControllerBlock.TRANSPONDER);
    }

    /**
     * Switch the transponder on every controller on this ship (or just this one on the ground),
     * so a redundant controller can't keep broadcasting by accident.
     */
    public void setShipTransponder(final boolean on) {
        if (this.level == null) {
            return;
        }
        final List<AvionicsControllerBlockEntity> all = this.ship == null ? List.of(this) : ShipControllers.on(this.ship);
        for (final AvionicsControllerBlockEntity c : all.isEmpty() ? List.of(this) : all) {
            final BlockState state = c.getBlockState();
            if (state.getValue(AvionicsControllerBlock.TRANSPONDER) != on) {
                this.level.setBlock(c.getBlockPos(), state.setValue(AvionicsControllerBlock.TRANSPONDER, on), Block.UPDATE_ALL);
            }
        }
    }

    public AvionicsControllerPeripheral peripheral() {
        if (this.peripheral == null) {
            this.peripheral = new AvionicsControllerPeripheral(this);
        }
        return this.peripheral;
    }

    /** Remember who placed this, to tell them whether it set the bow or followed it. */
    public void placedBy(final UUID player) {
        this.placer = player;
    }

    public @Nullable UUID ship() {
        return this.ship;
    }

    public List<ShipScan.Found> found() {
        return this.found;
    }

    public List<Channel> channels() {
        return this.channels.channels();
    }

    /** A channel's current value, or empty if the ship can't measure it right now. */
    public OptionalDouble read(final Channel channel) {
        return this.channels.read(channel);
    }

    public ShipBridge bridge() {
        return this.bridge;
    }

    /** Rescan on the next tick instead of waiting, e.g. after a neighbour or peer changed. */
    public void rescanSoon() {
        this.rescanSoon = true;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final AvionicsControllerBlockEntity be) {
        if (be.rescanSoon || (level.getGameTime() + pos.hashCode()) % SCAN_INTERVAL == 0) {
            be.rescanSoon = false;
            be.rescan(level);
        }
    }

    private void rescan(final Level level) {
        final SubLevel subLevel = ShipScan.shipOf(this);
        final UUID now = subLevel == null || subLevel.isRemoved() ? null : subLevel.getUniqueId();
        if (this.ship != null && !this.ship.equals(now)) {
            this.leaveShip();
        }
        this.ship = now;
        this.found = now == null ? List.of() : ShipScan.scan(level, subLevel);
        this.channels = new ChannelMap(this.found, this.forward());

        this.bridge.connectNeighbours();
        if (now == null) {
            this.bridge.linkPeers(List.of());
            this.bridge.standDown(Map.of());
            return;
        }
        ShipControllers.join(now, this);
        final List<AvionicsControllerBlockEntity> peers = ShipControllers.on(now);
        this.bridge.linkPeers(peers);
        this.agreeOnBow(level, peers);
        final AvionicsControllerBlockEntity leader = peers.get(0);
        if (leader != this) {
            // The leader publishes; make sure this one isn't, and keep a copy of its names
            this.bridge.standDown(leader.bridge().names());
            return;
        }
        // A controller that just became leader starts from the names the ship already uses
        if (this.bridge.names().isEmpty()) {
            for (final AvionicsControllerBlockEntity other : peers) {
                if (other != this && !other.bridge().names().isEmpty()) {
                    this.bridge.loadNames(other.bridge().names());
                    break;
                }
            }
        }
        // CC drops a withdrawn name network-wide, so the others step down before this publishes
        for (final AvionicsControllerBlockEntity other : peers) {
            if (other != this) {
                other.bridge().standDown(this.bridge.names());
            }
        }
        this.bridge.publish(this.found, subLevel.getPlot().getBoundingBox());
        for (final AvionicsControllerBlockEntity other : peers) {
            if (other != this) {
                other.bridge().loadNames(this.bridge.names());
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        final ListTag names = new ListTag();
        this.bridge.names().forEach((pos, name) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            entry.putString("Name", name);
            names.add(entry);
        });
        tag.put("PeripheralNames", names);
        if (this.bow != null) {
            tag.putString("Bow", this.bow.getSerializedName());
            tag.putLong("BowSetAt", this.bowSetAt);
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final Map<BlockPos, String> names = new HashMap<>();
        for (final Tag t : tag.getList("PeripheralNames", Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) t;
            names.put(BlockPos.of(entry.getLong("Pos")), entry.getString("Name"));
        }
        this.bridge.loadNames(names);
        this.bow = tag.contains("Bow") ? Direction.byName(tag.getString("Bow")) : null;
        this.bowSetAt = tag.contains("BowSetAt") ? tag.getLong("BowSetAt") : Long.MIN_VALUE;
    }

    /**
     * Adopt the newest bow any controller on the ship knows. If none has one, this controller is
     * the first and sets it. Then point the arrow on top at it and tell whoever placed this.
     */
    private void agreeOnBow(final Level level, final List<AvionicsControllerBlockEntity> peers) {
        AvionicsControllerBlockEntity newest = null;
        for (final AvionicsControllerBlockEntity c : peers) {
            if (c.bow != null && (newest == null || c.bowSetAt > newest.bowSetAt)) {
                newest = c;
            }
        }
        final boolean setsIt = newest == null;
        if (setsIt) {
            this.bow = this.facing();
            this.bowSetAt = level.getGameTime();
            this.setChanged();
        } else if (newest != this && (this.bow != newest.bow || this.bowSetAt != newest.bowSetAt)) {
            this.bow = newest.bow;
            this.bowSetAt = newest.bowSetAt;
            this.setChanged();
        }
        final BlockState state = this.getBlockState();
        final AvionicsControllerBlock.BowSide side = AvionicsControllerBlock.BowSide.of(this.forward(), this.facing());
        if (state.getValue(AvionicsControllerBlock.BOW) != side) {
            level.setBlock(this.worldPosition, state.setValue(AvionicsControllerBlock.BOW, side), Block.UPDATE_CLIENTS);
        }
        if (this.placer != null) {
            final Player player = level.getPlayerByUUID(this.placer);
            if (player != null) {
                player.displayClientMessage(Component.translatable(setsIt
                        ? "create_avionics.controller.bow_set" : "create_avionics.controller.bow_followed"), true);
            }
            this.placer = null;
        }
    }

    private void leaveShip() {
        if (this.ship == null) {
            return;
        }
        ShipControllers.leave(this.ship, this);
        // The next controller in line takes over publishing straight away
        for (final AvionicsControllerBlockEntity other : ShipControllers.on(this.ship)) {
            other.rescanSoon();
        }
    }

    /** Also called when the chunk unloads. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            this.leaveShip();
            this.bridge.remove();
        }
    }

}
