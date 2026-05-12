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
 * <b>Added (Avionics):</b> the full kinetic SCADA pack, inherited from
 * {@link KineticScadaSurface} via {@link KineticPeripheral} — {@code getSelfId},
 * {@code getSourceId}, {@code getSubnetworkAnchorId}, {@code getNetworkId},
 * {@code getKind} (returns {@code "passthrough"} on a Stressometer),
 * {@code getSpeed}, {@code hasSource}, {@code isOverstressed},
 * {@code getStressImpact} (per-block draw, always 0 on a gauge),
 * {@code getStressContribution} (per-block capacity contribution, always 0 on
 * a gauge).
 * <p>
 * <b>Stress methods on this peripheral are network-wide totals.</b>
 * {@link #getStress} and {@link #getStressCapacity} sum across every consumer
 * and source on the network — Create's gauge semantics, preserved. The
 * per-block {@code getStressImpact} / {@code getStressContribution} inherited
 * from the SCADA pack are distinct from these and report 0 on a Stressometer
 * (gauges neither draw nor add stress).
 *
 * @cc.module Create_Stressometer
 */
public class StressGaugePeripheral extends KineticPeripheral<StressGaugeBlockEntity> {

    public StressGaugePeripheral(final StressGaugeBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_Stressometer";
    }

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
}
