/*
 * Portions of this file are derived from Create
 * (com.simibubi.create.compat.computercraft.implementation.peripherals.SpeedControllerPeripheral),
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

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dan200.computercraft.api.lua.LuaFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Drop-in replacement for Create's {@code Create_RotationSpeedController}
 * peripheral.
 * <p>
 * <b>Preserved from Create (verbatim):</b> {@link #setTargetSpeed} and
 * {@link #getTargetSpeed}. Existing scripts that drive a speed controller
 * keep working unchanged.
 * <p>
 * <b>Added (Avionics):</b> the full kinetic SCADA pack — {@code getSelfId},
 * {@code getSourceId}, {@code getSubnetworkAnchorId}, {@code getNetworkId},
 * {@code getKind} (returns {@code "split_shaft"}), {@code getSpeed},
 * {@code hasSource}, {@code isOverstressed}, {@code getStressImpact},
 * {@code getStressContribution}. The speed controller is a speed-zone boundary
 * (one of the few block kinds that returns {@code "split_shaft"}), so its
 * {@code getSelfId} is the natural anchor id every block downstream of it
 * reports for {@code getSubnetworkAnchorId}.
 *
 * @cc.module Create_RotationSpeedController
 */
public class SpeedControllerPeripheral extends KineticPeripheral<SpeedControllerBlockEntity> {

    private final ScrollValueBehaviour targetSpeed;

    public SpeedControllerPeripheral(final SpeedControllerBlockEntity blockEntity, final ScrollValueBehaviour targetSpeed) {
        super(blockEntity);
        this.targetSpeed = targetSpeed;
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_RotationSpeedController";
    }

    /**
     * Set the controller's target output speed in RPM. Sign sets direction;
     * the controller scales its input shaft to match. Clamped to the in-game
     * scroll-value range (-256..+256 RPM).
     *
     * @param speed The target output speed in RPM.
     */
    @LuaFunction(mainThread = true)
    public final void setTargetSpeed(int speed) {
        this.targetSpeed.setValue(speed);
    }

    /**
     * Get the controller's currently configured target output speed in RPM.
     * Reflects the value last set via {@link #setTargetSpeed} or the in-game
     * scroll wheel — not the live shaft speed (which depends on the input).
     *
     * @return The configured target speed in RPM.
     */
    @LuaFunction
    public final double getTargetSpeed() {
        return this.targetSpeed.getValue();
    }
}
