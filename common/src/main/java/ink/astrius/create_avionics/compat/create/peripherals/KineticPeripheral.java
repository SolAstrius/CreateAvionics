package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * Base class for Avionics peripherals (Create-side) that wrap a Create kinetic
 * block. Provides the kinetic SCADA pack — block id, parent id, speed-zone
 * anchor id, network id, role, local speed, stress, health — so concrete
 * peripherals only add their block-specific surface.
 *
 * <p>Ids are opaque tokens for equality comparison across peripherals.
 * Network totals are not exposed; that's the Stressometer's role.</p>
 */
public abstract class KineticPeripheral<T extends KineticBlockEntity> extends SyncedPeripheral<T> {

    public KineticPeripheral(final T blockEntity) {
        super(blockEntity);
    }

    /**
     * Get this block's id. Other peripherals' {@link #getSourceId} or
     * {@link #getSubnetworkAnchorId} return this same id when they refer to
     * this block.
     *
     * @return The block's id.
     */
    @LuaFunction
    public final String getSelfId() {
        return KineticReadback.selfId(this.blockEntity);
    }

    /**
     * Get the id of the block immediately driving this one, or nil if this
     * block has no source.
     *
     * @return The parent's id, or nil.
     */
    @LuaFunction
    public final String getSourceId() {
        return KineticReadback.sourceId(this.blockEntity);
    }

    /**
     * Get the id of this block's speed-zone anchor — the gearshift / clutch /
     * speed controller / generator that defines the start of this speed zone.
     * Two blocks share an anchor iff they're in the same speed zone. A
     * generator or split-shaft returns its own {@link #getSelfId}.
     *
     * @return The anchor block's id, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getSubnetworkAnchorId() {
        return KineticReadback.subnetworkAnchorId(this.blockEntity);
    }

    /**
     * Get the id of this block's kinetic network. Same value for every block
     * on the same network regardless of how many speed zones lie between
     * them. Nil if this block isn't on a network.
     *
     * @return The network id, or nil.
     */
    @LuaFunction
    public final String getNetworkId() {
        return KineticReadback.networkId(this.blockEntity);
    }

    /**
     * Get this block's role on the kinetic graph: one of {@code "generator"},
     * {@code "split_shaft"}, {@code "consumer"}, or {@code "passthrough"}.
     *
     * @return The role string.
     */
    @LuaFunction
    public final String getKind() {
        return KineticReadback.kindOf(this.blockEntity);
    }

    /**
     * Get the local rotational speed at this block.
     * Signed; same value across a speed zone, changes across a split-shaft.
     *
     * @return The local speed.
     */
    @LuaFunction
    public final double getSpeed() {
        return this.blockEntity.getSpeed();
    }

    /**
     * Check whether this block is connected to a kinetic source.
     *
     * @return True if a source is connected.
     */
    @LuaFunction
    public final boolean hasSource() {
        return this.blockEntity.hasSource();
    }

    /**
     * Check whether the block's network is overstressed.
     *
     * @return True if overstressed.
     */
    @LuaFunction
    public final boolean isOverstressed() {
        return this.blockEntity.isOverStressed();
    }

    /**
     * Get the stress impact of this block on its network — how much stress
     * it draws while running. Speed-dependent; zero for sources and pure
     * conduit blocks. Matches the "Stress Impact" value shown by goggles.
     *
     * @return The stress impact.
     */
    @LuaFunction
    public final double getStressImpact() {
        return this.blockEntity.calculateStressApplied();
    }

    /**
     * Get this block's contribution to its network's stress capacity. Non-zero
     * for sources only. Parallel to {@link #getStressImpact} (per-block draw)
     * and distinct from {@code Create_Stressometer#getStressCapacity}, which
     * reports the network total.
     *
     * @return The per-block stress contribution.
     */
    @LuaFunction
    public final double getStressContribution() {
        return this.blockEntity.calculateAddedStressCapacity();
    }
}
