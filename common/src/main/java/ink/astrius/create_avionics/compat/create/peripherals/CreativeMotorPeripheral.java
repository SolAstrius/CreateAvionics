/*
 * Portions of this file are derived from Create
 * (com.simibubi.create.compat.computercraft.implementation.peripherals.CreativeMotorPeripheral),
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

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dan200.computercraft.api.lua.LuaFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Drop-in replacement for Create's {@code Create_CreativeMotor} peripheral.
 * <p>
 * <b>Preserved from Create (verbatim):</b> {@link #setGeneratedSpeed} and
 * {@link #getGeneratedSpeed}. Existing scripts that drive a motor keep
 * working unchanged.
 * <p>
 * <b>Added (Avionics):</b> the full kinetic SCADA pack — {@code getSelfId},
 * {@code getSourceId}, {@code getSubnetworkAnchorId}, {@code getNetworkId},
 * {@code getKind} (returns {@code "generator"}), {@code getSpeed},
 * {@code hasSource}, {@code isOverstressed}, {@code getStressImpact} (0
 * — motors don't draw stress), {@code getStressCapacity} (the per-block
 * capacity the motor contributes; non-zero while running). The motor is
 * typically the network root, and its {@code getSelfId} is the
 * {@code getNetworkId} every block downstream of it reports.
 *
 * @cc.module Create_CreativeMotor
 */
public class CreativeMotorPeripheral extends KineticPeripheral<CreativeMotorBlockEntity> {

    private final ScrollValueBehaviour generatedSpeed;

    public CreativeMotorPeripheral(final CreativeMotorBlockEntity blockEntity, final ScrollValueBehaviour generatedSpeed) {
        super(blockEntity);
        this.generatedSpeed = generatedSpeed;
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_CreativeMotor";
    }

    @LuaFunction(mainThread = true)
    public final void setGeneratedSpeed(int speed) {
        this.generatedSpeed.setValue(speed);
    }

    @LuaFunction
    public final float getGeneratedSpeed() {
        return this.generatedSpeed.getValue();
    }
}
