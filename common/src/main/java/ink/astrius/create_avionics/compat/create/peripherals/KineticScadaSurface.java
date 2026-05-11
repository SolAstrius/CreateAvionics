package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * Shared SCADA surface for kinetic peripherals. Brings the topology / speed /
 * stress pack to both
 * {@link KineticPeripheral} (Create-side, atop {@code SyncedPeripheral}) and
 * {@code SimKineticPeripheral} (Avionics-side, atop {@code SimPeripheral})
 * without duplicating the @LuaFunction declarations. Implementors only supply
 * the wrapped block entity via {@link #scadaBlockEntity()}.
 *
 * <p>The bare-block-entity case (kinetic blocks with no Avionics or Create
 * peripheral, e.g. encased shafts) is covered separately by
 * {@link ink.astrius.create_avionics.compat.simulated.peripherals.generic.KineticSource},
 * a {@code GenericSource} that CC instantiates as a fallback peripheral.</p>
 */
public interface KineticScadaSurface {

    KineticBlockEntity scadaBlockEntity();

    /**
     * Get this block's id. Other peripherals' {@link #getSourceId} or
     * {@link #getSubnetworkAnchorId} return this same id when they refer to
     * this block.
     *
     * @return The block's id.
     */
    @LuaFunction
    default String getSelfId() {
        return KineticReadback.selfId(scadaBlockEntity());
    }

    /**
     * Get the id of the block immediately driving this one, or nil if this
     * block has no source.
     *
     * @return The parent's id, or nil.
     */
    @LuaFunction
    default String getSourceId() {
        return KineticReadback.sourceId(scadaBlockEntity());
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
    default String getSubnetworkAnchorId() {
        return KineticReadback.subnetworkAnchorId(scadaBlockEntity());
    }

    /**
     * Get the id of this block's kinetic network. Same value for every block
     * on the same network regardless of how many speed zones lie between
     * them. Nil if this block isn't on a network.
     *
     * @return The network id, or nil.
     */
    @LuaFunction
    default String getNetworkId() {
        return KineticReadback.networkId(scadaBlockEntity());
    }

    /**
     * Get this block's role on the kinetic graph: one of {@code "generator"},
     * {@code "split_shaft"}, {@code "consumer"}, or {@code "passthrough"}.
     *
     * @return The role string.
     */
    @LuaFunction
    default String getKind() {
        return KineticReadback.kindOf(scadaBlockEntity());
    }

    /**
     * Get the local rotational speed at this block.
     * Signed; same value across a speed zone, changes across a split-shaft.
     *
     * @return The local speed.
     */
    @LuaFunction
    default double getSpeed() {
        return scadaBlockEntity().getSpeed();
    }

    /**
     * Check whether this block is connected to a kinetic source.
     *
     * @return True if a source is connected.
     */
    @LuaFunction
    default boolean hasSource() {
        return scadaBlockEntity().hasSource();
    }

    /**
     * Check whether the block's network is overstressed.
     *
     * @return True if overstressed.
     */
    @LuaFunction
    default boolean isOverstressed() {
        return scadaBlockEntity().isOverStressed();
    }

    /**
     * Get the stress impact of this block on its network — how much stress
     * it draws while running. Speed-dependent; zero for sources and pure
     * conduit blocks. Matches the "Stress Impact" value shown by goggles.
     *
     * @return The stress impact.
     */
    @LuaFunction
    default double getStressImpact() {
        return scadaBlockEntity().calculateStressApplied();
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
    default double getStressContribution() {
        return scadaBlockEntity().calculateAddedStressCapacity();
    }
}
