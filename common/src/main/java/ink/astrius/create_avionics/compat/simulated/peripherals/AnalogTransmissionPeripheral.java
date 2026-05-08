package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlock;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import ink.astrius.create_avionics.api.simulated.AnalogTransmissionExt;
import net.minecraft.util.Mth;

/**
 * A scriptable variable-ratio gearbox. Exposes the analog signal that selects the
 * ratio (with override semantics that release back to redstone), plus input/output
 * speed, stress, and orientation.
 *
 * @cc.module analog_transmission
 */
public class AnalogTransmissionPeripheral extends SimPeripheral<AnalogTransmissionBlockEntity> {

    public AnalogTransmissionPeripheral(final AnalogTransmissionBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "analog_transmission";
    }

    // --- Control ---

    private AnalogTransmissionExt ext() {
        return (AnalogTransmissionExt) this.blockEntity;
    }

    /**
     * Get the current analog signal driving the transmission ratio.
     *
     * @return The signal, 0..15.
     */
    @LuaFunction
    public int getSignal() {
        return this.ext().getSignal();
    }

    /**
     * Drive the transmission to a new signal and take external control.
     * Flips externallyControlled even when signal == current; calling
     * setSignal(getSignal()) is the documented way to grab control without
     * changing the value. releaseSignal() returns control to redstone.
     * <p>Yields until the next server tick.
     *
     * @param signal The target signal, clamped to 0..15.
     */
    @LuaFunction(mainThread = true)
    public final void setSignal(final int signal) {
        this.ext().setExternallyControlled(true);
        this.ext().applySignal(Mth.clamp(signal, 0, 15));
    }

    /**
     * Release external control and return signal driving to redstone.
     * <p>Yields until the next server tick.
     */
    @LuaFunction(mainThread = true)
    public final void releaseSignal() {
        this.ext().setExternallyControlled(false);
    }

    /**
     * Check whether the transmission is currently under script control.
     *
     * @return True if externally controlled, false if driven by redstone.
     */
    @LuaFunction
    public boolean isExternallyControlled() {
        return this.ext().isExternallyControlled();
    }

    // --- Rotation state ---

    /**
     * Get the current rotation modifier (output:input speed ratio).
     *
     * @return The rotation modifier.
     */
    @LuaFunction
    public double getRotationModifier() {
        return this.blockEntity.getRotationModifier();
    }

    /**
     * Get the input shaft speed.
     *
     * @return The input speed.
     */
    @LuaFunction
    public double getInputSpeed() {
        return this.blockEntity.getSpeed();
    }

    /**
     * Get the output shaft speed.
     *
     * @return The output speed.
     */
    @LuaFunction
    public double getOutputSpeed() {
        return this.blockEntity.getExtraKinetics().getSpeed();
    }

    /**
     * Get the output shaft's theoretical (target) speed.
     *
     * @return The output theoretical speed.
     */
    @LuaFunction
    public double getOutputTheoreticalSpeed() {
        return this.blockEntity.getExtraKinetics().getTheoreticalSpeed();
    }

    // --- Stress ---

    /**
     * Get the stress applied on the input side.
     *
     * @return The applied stress.
     */
    @LuaFunction
    public double getStressApplied() {
        return this.blockEntity.calculateStressApplied();
    }

    /**
     * Get the stress applied on the output side.
     *
     * @return The output applied stress.
     */
    @LuaFunction
    public double getOutputStressApplied() {
        return this.blockEntity.getExtraKinetics().calculateStressApplied();
    }

    // --- Health ---

    /**
     * Check whether the transmission is overstressed.
     *
     * @return True if overstressed.
     */
    @LuaFunction
    public boolean isOverstressed() {
        return this.blockEntity.isOverStressed();
    }

    /**
     * Check whether the transmission is oversaturated.
     *
     * @return True if oversaturated.
     */
    @LuaFunction
    public boolean isOversaturated() {
        return this.ext().isOversaturated();
    }

    // --- Orientation ---

    /**
     * Get the transmission's shaft axis name.
     *
     * @return The axis as a serialized string ("x", "y", or "z").
     */
    @LuaFunction
    public String getAxis() {
        return this.blockEntity.getBlockState().getValue(AnalogTransmissionBlock.AXIS).getSerializedName();
    }
}
