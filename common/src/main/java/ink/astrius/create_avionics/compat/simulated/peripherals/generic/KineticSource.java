package ink.astrius.create_avionics.compat.simulated.peripherals.generic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.create.peripherals.KineticReadback;

/**
 * Shared peripheral for kinetic blocks meant to mimic the base class for Avionics peripherals.
 * Provides the kinetic SCADA pack — block id, parent id, speed-zone
 * anchor id, network id, role, local speed, stress, health — so concrete
 * peripherals only add their block-specific surface.
 *
 * <p>Ids are opaque tokens for equality comparison across peripherals.
 * Network totals are not exposed; that's the Stressometer's role.</p>
 *
 * @cc.module kinetic_provider
 */
public class KineticSource implements GenericSource {
    @Override
    public String id() {
        return "kinetic";
    }

    /**
     * Get this block's id. See {@code KineticPeripheral#getSelfId}.
     *
     * @return The block's id.
     */
    @LuaFunction
    public final String getSelfId(KineticBlockEntity be) {
        return KineticReadback.selfId(be);
    }

    /**
     * Get the id of the block immediately driving this one, or nil.
     *
     * @return The parent's id, or nil.
     */
    @LuaFunction
    public final String getSourceId(KineticBlockEntity be) {
        return KineticReadback.sourceId(be);
    }

    /**
     * Get the id of this block's speed-zone anchor.
     *
     * @return The anchor's id, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getSubnetworkAnchorId(KineticBlockEntity be) {
        return KineticReadback.subnetworkAnchorId(be);
    }

    /**
     * Get the id of this block's kinetic network.
     *
     * @return The network id, or nil.
     */
    @LuaFunction
    public final String getNetworkId(KineticBlockEntity be) {
        return KineticReadback.networkId(be);
    }

    /**
     * Get this block's role on the kinetic graph: one of {@code "generator"},
     * {@code "split_shaft"}, {@code "consumer"}, or {@code "passthrough"}.
     *
     * @return The role string.
     */
    @LuaFunction
    public final String getKind(KineticBlockEntity be) {
        return KineticReadback.kindOf(be);
    }

    /**
     * Get the local rotational speed at this block.
     *
     * @return The local speed.
     */
    @LuaFunction
    public final double getSpeed(KineticBlockEntity be) {
        return be.getSpeed();
    }

    /**
     * Check whether this block is connected to a kinetic source.
     *
     * @return True if a source is connected.
     */
    @LuaFunction
    public final boolean hasSource(KineticBlockEntity be) {
        return be.hasSource();
    }

    /**
     * Check whether the block's network is overstressed.
     *
     * @return True if overstressed.
     */
    @LuaFunction
    public final boolean isOverstressed(KineticBlockEntity be) {
        return be.isOverStressed();
    }

    /**
     * Get the stress impact of this block on its network — how much stress
     * it draws while running. Matches the "Stress Impact" value shown by
     * goggles.
     *
     * @return The stress impact.
     */
    @LuaFunction
    public final double getStressImpact(KineticBlockEntity be) {
        return be.calculateStressApplied();
    }

    /**
     * Get the stress capacity this block adds to its network. Non-zero for
     * sources only.
     * <p>
     * <b>Caveat:</b> on the {@code Create_Stressometer} peripheral the
     * same-named method returns the <em>network total</em> capacity instead
     * (preserving Create's gauge semantics). Stressometer is the only place
     * this method's meaning differs.
     *
     * @return The added stress capacity (per-block contribution).
     */
    @LuaFunction
    public final double getStressCapacity(KineticBlockEntity be) {
        return be.calculateAddedStressCapacity();
    }
}
