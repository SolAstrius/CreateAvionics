/*
 * Portions of this file are derived from Create
 * (com.simibubi.create.compat.computercraft.implementation.peripherals.StressGaugePeripheral),
 * licensed under the MIT License.
 *
 * MIT License
 *
 * Copyright (c) The Create Team / The Creators of Create
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.compat.computercraft.events.ComputerEvent;
import com.simibubi.create.compat.computercraft.events.KineticsChangeEvent;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Drop-in replacement for Create's {@code Create_Stressometer} peripheral.
 * <p>
 * <b>Preserved from Create (verbatim):</b> {@link #getStress},
 * {@link #getStressCapacity}, and the {@code overstressed} / {@code stress_change}
 * events. Existing scripts that rely on these names and semantics keep working.
 * <p>
 * <b>Added (Avionics):</b> the kinetic SCADA topology fields — {@link #getSelfId},
 * {@link #getSourceId}, {@link #getSubnetworkAnchorId}, {@link #getNetworkId},
 * {@link #getKind}, {@link #getSpeed}, {@link #hasSource},
 * {@link #isOverstressed}.
 * <p>
 * <b>Stress methods on this peripheral are network-wide totals.</b>
 * {@link #getStress} and {@link #getStressCapacity} sum across every consumer
 * and source on the network — Create's gauge semantics, preserved. Every other
 * kinetic peripheral in this addon (propellers, bearings, gearshifts, motors,
 * etc.) instead exposes per-block values under the distinct names
 * {@code getStressImpact} (draw) and {@code getStressContribution} (capacity
 * added). Per-block methods are intentionally absent on the Stressometer —
 * they would always be 0 on a gauge.
 *
 * @cc.module Create_Stressometer
 */
public class StressGaugePeripheral extends SyncedPeripheral<StressGaugeBlockEntity> {

    public StressGaugePeripheral(final StressGaugeBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_Stressometer";
    }

    // --- Inherited Create surface (verbatim) ---

    /**
     * Get the current total stress demand on this block's kinetic network.
     * <p>
     * This is a <em>network-wide</em> total, summed across every consumer on
     * the network — not this block's own draw. Stressometers are the only
     * peripherals that expose network totals; on any other kinetic peripheral
     * the per-block stress is {@code getStressImpact}.
     *
     * @return The network's total stress demand.
     */
    @LuaFunction
    public final double getStress() {
        return this.blockEntity.getNetworkStress();
    }

    /**
     * Get the current total stress capacity of this block's kinetic network —
     * sum of every source's contribution. Distinct from the per-block
     * {@code getStressContribution} exposed on every other kinetic peripheral
     * in this addon, which reports a single block's own contribution.
     *
     * @return The network's total stress capacity.
     */
    @LuaFunction
    public final double getStressCapacity() {
        return this.blockEntity.getNetworkCapacity();
    }

    @Override
    public void prepareComputerEvent(@NotNull final ComputerEvent event) {
        if (event instanceof final KineticsChangeEvent kce) {
            if (kce.overStressed)
                queueEvent("overstressed");
            else
                queueEvent("stress_change", kce.stress, kce.capacity);
        }
    }

    // --- Avionics: kinetic SCADA pack ---
    // Per-block getStressImpact / getStressContribution intentionally omitted;
    // both are always 0 for a gauge, and exposing them here would only add noise.

    /**
     * Get this block's opaque self-id within the kinetic SCADA topology.
     * Stable identity for cross-peripheral grouping; equality-comparable
     * with {@code getSourceId} / {@code getNetworkId} on neighbours.
     *
     * @return The self-id.
     */
    @LuaFunction
    public final String getSelfId() {
        return KineticReadback.selfId(this.blockEntity);
    }

    /**
     * Get the id of the block whose rotation this gauge is reading from
     * (the upstream kinetic source one hop away). Nil when no source.
     *
     * @return The source id, or nil.
     */
    @LuaFunction
    public final String getSourceId() {
        return KineticReadback.sourceId(this.blockEntity);
    }

    /**
     * Get the id of the speed-zone anchor this gauge belongs to. Every block
     * within a single speed-zone reports the same anchor — useful for
     * grouping peripherals into rotation-coherent regions.
     * <p>Anchor resolution walks the kinetic graph through neighbouring
     * block entities, so it must run on the server thread.
     *
     * @return The subnetwork anchor id.
     */
    @LuaFunction(mainThread = true)
    public final String getSubnetworkAnchorId() {
        return KineticReadback.subnetworkAnchorId(this.blockEntity);
    }

    /**
     * Get the id of the entire kinetic network this gauge is part of. All
     * peripherals on the same network — across all speed zones — share this
     * id.
     *
     * @return The network id.
     */
    @LuaFunction
    public final String getNetworkId() {
        return KineticReadback.networkId(this.blockEntity);
    }

    /**
     * Get the SCADA kind classifier for this block. Stressometers report
     * {@code "passthrough"} (they read from the network without modifying
     * speed or topology).
     *
     * @return The kind classifier.
     */
    @LuaFunction
    public final String getKind() {
        return KineticReadback.kindOf(this.blockEntity);
    }

    /**
     * Get the gauge's current shaft speed in RPM (signed). Same value Create
     * exposes natively.
     *
     * @return The current speed in RPM.
     */
    @LuaFunction
    public final double getSpeed() {
        return this.blockEntity.getSpeed();
    }

    /**
     * Check whether the gauge currently has a kinetic source feeding it.
     * False on a disconnected gauge or one in a stalled network.
     *
     * @return True if a source is present.
     */
    @LuaFunction
    public final boolean hasSource() {
        return this.blockEntity.hasSource();
    }

    /**
     * Check whether the kinetic network this gauge is on is overstressed.
     * Mirrors the {@code overstressed} event but pollable on demand.
     *
     * @return True if overstressed.
     */
    @LuaFunction
    public final boolean isOverstressed() {
        return this.blockEntity.isOverStressed();
    }
}
