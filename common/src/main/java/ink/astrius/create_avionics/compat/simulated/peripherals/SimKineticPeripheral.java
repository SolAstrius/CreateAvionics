package ink.astrius.create_avionics.compat.simulated.peripherals;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.create.peripherals.KineticReadback;

/**
 * Base class for Avionics-side peripherals that wrap a Create kinetic block.
 * Mirror of {@code KineticPeripheral} on the Create side, on top of
 * {@link SimPeripheral} instead of Create's {@code SyncedPeripheral}. Both
 * delegate to the same {@link KineticReadback} helper so the kinetic SCADA
 * pack is identical wherever it appears.
 */
public abstract class SimKineticPeripheral<T extends KineticBlockEntity> extends SimPeripheral<T> {

    public SimKineticPeripheral(final T blockEntity) {
        super(blockEntity);
    }

    /**
     * Get this block's id. See {@code KineticPeripheral#getSelfId}.
     *
     * @return The block's id.
     */
    @LuaFunction
    public final String getSelfId() {
        return KineticReadback.selfId(this.blockEntity);
    }

    /**
     * Get the id of the block immediately driving this one, or nil.
     *
     * @return The parent's id, or nil.
     */
    @LuaFunction
    public final String getSourceId() {
        return KineticReadback.sourceId(this.blockEntity);
    }

    /**
     * Get the id of this block's speed-zone anchor.
     *
     * @return The anchor's id, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getSubnetworkAnchorId() {
        return KineticReadback.subnetworkAnchorId(this.blockEntity);
    }

    /**
     * Get the id of this block's kinetic network.
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
     * it draws while running. Matches the "Stress Impact" value shown by
     * goggles.
     *
     * @return The stress impact.
     */
    @LuaFunction
    public final double getStressImpact() {
        return this.blockEntity.calculateStressApplied();
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
    public final double getStressCapacity() {
        return this.blockEntity.calculateAddedStressCapacity();
    }
}
