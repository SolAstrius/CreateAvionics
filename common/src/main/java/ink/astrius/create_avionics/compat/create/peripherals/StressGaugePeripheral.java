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
 * <b>Important — {@link #getStressCapacity} semantics differ here:</b> on the
 * Stressometer this method returns the <em>network total</em> capacity (Create's
 * gauge behavior), <em>not</em> the per-block contribution that the same
 * method name returns on every other kinetic peripheral in this addon
 * (propellers, bearings, gearshifts, motors, etc.). Likewise {@link #getStress}
 * is the network total, not a per-block value. The Stressometer is the only
 * peripheral that exposes network-wide stress totals; everywhere else
 * {@code getStressCapacity} / {@code getStressImpact} mean a single block's
 * own contribution / draw. Per-block stress methods are intentionally absent
 * here (they would always be 0 on a gauge and would collide with the
 * network-total methods).
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
    public final float getStress() {
        return this.blockEntity.getNetworkStress();
    }

    /**
     * Get the current total stress capacity of this block's kinetic network.
     * <p>
     * <b>Note on semantics:</b> on the Stressometer this returns the
     * <em>network's total</em> capacity (sum of every source's contribution).
     * On every other kinetic peripheral in this addon, the same method name
     * returns the <em>per-block</em> contribution. The Stressometer is the
     * exception by design — its job is exposing network totals.
     *
     * @return The network's total stress capacity.
     */
    @LuaFunction
    public final float getStressCapacity() {
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
    // Per-block getStressImpact / getStressCapacity intentionally omitted;
    // both are always 0 for a gauge, and the latter would shadow the
    // network-total method above.

    @LuaFunction
    public final String getSelfId() {
        return KineticReadback.selfId(this.blockEntity);
    }

    @LuaFunction
    public final String getSourceId() {
        return KineticReadback.sourceId(this.blockEntity);
    }

    @LuaFunction
    public final String getSubnetworkAnchorId() {
        return KineticReadback.subnetworkAnchorId(this.blockEntity);
    }

    @LuaFunction
    public final String getNetworkId() {
        return KineticReadback.networkId(this.blockEntity);
    }

    @LuaFunction
    public final String getKind() {
        return KineticReadback.kindOf(this.blockEntity);
    }

    @LuaFunction
    public final double getSpeed() {
        return this.blockEntity.getSpeed();
    }

    @LuaFunction
    public final boolean hasSource() {
        return this.blockEntity.hasSource();
    }

    @LuaFunction
    public final boolean isOverstressed() {
        return this.blockEntity.isOverStressed();
    }
}
