/*
 * Portions of this file are derived from Create
 * (com.simibubi.create.compat.computercraft.implementation.peripherals.SpeedGaugePeripheral),
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
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Drop-in replacement for Create's {@code Create_Speedometer} peripheral.
 * <p>
 * <b>Preserved from Create:</b> {@code getSpeed} (same value and Lua
 * semantics as Create's implementation — both call the underlying block's
 * speed; the value is inherited from the kinetic SCADA pack here) and the
 * {@code speed_change} event fired on kinetic state changes.
 * <p>
 * <b>Added (Avionics):</b> the full kinetic SCADA pack — {@code getSelfId},
 * {@code getSourceId}, {@code getSubnetworkAnchorId}, {@code getNetworkId},
 * {@code getKind} (returns {@code "passthrough"} on a Speedometer),
 * {@code hasSource}, {@code isOverstressed}, plus per-block
 * {@code getStressImpact} / {@code getStressCapacity} (both 0 on a gauge,
 * harmless). Speedometer becomes a topology probe in addition to its
 * traditional role.
 *
 * @cc.module Create_Speedometer
 */
public class SpeedGaugePeripheral extends KineticPeripheral<SpeedGaugeBlockEntity> {

    public SpeedGaugePeripheral(final SpeedGaugeBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_Speedometer";
    }

    @Override
    public void prepareComputerEvent(@NotNull final ComputerEvent event) {
        if (event instanceof final KineticsChangeEvent kce) {
            queueEvent("speed_change", kce.overStressed ? 0 : kce.speed);
        }
    }
}
