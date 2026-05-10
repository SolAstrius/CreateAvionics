package ink.astrius.create_avionics.compat.offroad.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import ink.astrius.create_avionics.api.offroad.WheelMountExt;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimKineticPeripheral;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * One wheel of a vehicle. On top of the kinetic SCADA surface, exposes
 * independent script overrides for steering and braking (each replacing
 * the corresponding redstone read), plus suspension and tire reads.
 * <p>To drive a 4-wheeled vehicle from one computer: assemble all four
 * mounts as separate peripherals (e.g. via {@code peripheral.find} or
 * named modems) and call {@link #setSteering} / {@link #setBrake} on
 * each with the same input. Drive force itself comes from the Create
 * kinetic network feeding the axles — pair with {@code speed_controller}.
 *
 * @cc.module wheel_mount
 */
public class WheelMountPeripheral extends SimKineticPeripheral<WheelMountBlockEntity> {

    public WheelMountPeripheral(final WheelMountBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "wheel_mount";
    }

    private WheelMountExt ext() {
        return (WheelMountExt) this.blockEntity;
    }

    // --- Steering ---

    /**
     * Install a steering override. Replaces the side-redstone read so the
     * wheel turns by {@code value × 30°} regardless of redstone.
     * <p>Calling {@link #setSteering} on every wheel of a vehicle with the
     * same value gives parallel steering. For real Ackermann geometry the
     * caller computes per-wheel angles and writes them individually.
     *
     * @param value Steering input in [-1, 1]. Out-of-range values are clamped.
     */
    @LuaFunction(mainThread = true)
    public final void setSteering(final double value) {
        final double clamped = Mth.clamp(value, -1.0, 1.0);
        this.ext().setSteeringOverride((int) Math.round(clamped * 15.0));
    }

    /** Release the steering override; the wheel resumes reading side redstone. */
    @LuaFunction(mainThread = true)
    public final void clearSteering() {
        this.ext().clearSteeringOverride();
    }

    /**
     * @return The currently-applied steering input in [-1, 1]. When override
     *         is active, this is the script value; otherwise it is the live
     *         redstone differential normalized to the same range.
     */
    @LuaFunction
    public final double getSteering() {
        return this.ext().getSteeringSignalValue() / 15.0;
    }

    /**
     * @return Current steering angle in degrees, computed from the active
     *         signal. Maxes at ±30° (the wheel's hard-clamped range).
     */
    @LuaFunction
    public final double getSteeringAngle() {
        return -(this.ext().getSteeringSignalValue() / 15.0) * 45.0 * (30.0 / 45.0);
    }

    /** @return True when the steering input is currently script-driven. */
    @LuaFunction
    public final boolean isSteeringOverridden() {
        return this.ext().isSteeringOverridden();
    }

    // --- Brake ---

    /**
     * Install a brake override. Replaces the top-of-block redstone read on
     * both server-side physics and the client-side visual.
     *
     * @param value Brake strength in [0, 1]. Out-of-range values are clamped.
     */
    @LuaFunction(mainThread = true)
    public final void setBrake(final double value) {
        final double clamped = Mth.clamp(value, 0.0, 1.0);
        this.ext().setBrakeOverride((int) Math.round(clamped * 15.0));
    }

    /** Release the brake override; the wheel resumes reading top-redstone. */
    @LuaFunction(mainThread = true)
    public final void clearBrake() {
        this.ext().clearBrakeOverride();
    }

    /** @return The currently-applied brake strength in [0, 1]. */
    @LuaFunction
    public final double getBrake() {
        return this.ext().getBrakeSignalValue() / 15.0;
    }

    /** @return True when the brake input is currently script-driven. */
    @LuaFunction
    public final boolean isBrakeOverridden() {
        return this.ext().isBrakeOverridden();
    }

    // --- Tire / state ---

    /** @return True when a tire item is installed in the mount. */
    @LuaFunction
    public final boolean hasTire() {
        return this.tire() != null;
    }

    /**
     * @return The radius of the installed tire, in blocks; {@code 0} when
     *         no tire is installed.
     */
    @LuaFunction
    public final double getTireRadius() {
        final TireLike tire = this.tire();
        return tire == null ? 0.0 : tire.radius();
    }

    /**
     * @return Live suspension extension distance. Larger values mean the
     *         wheel hangs lower below its mount; small (and clamped to
     *         {@code 0.5} when no tire is loaded) means compressed against
     *         the chassis.
     */
    @LuaFunction
    public final double getExtension() {
        return this.ext().getExtension();
    }

    /** @return Wheel's angular velocity in radians per tick. */
    @LuaFunction
    public final double getAngularVelocity() {
        return this.ext().getAngularVelocity();
    }

    /**
     * @return Friction coefficient of the block currently under the wheel.
     *         {@code 1.0} when airborne or on a "normal" block; lower on
     *         ice and similar low-friction surfaces.
     */
    @LuaFunction
    public final double getTouchingFriction() {
        return this.ext().getTouchingFriction();
    }

    /**
     * @return True when the wheel is detached from terrain (suspension
     *         travel exceeded). A lifted wheel has no traction.
     */
    @LuaFunction
    public final boolean isLiftedUp() {
        return this.ext().isLiftedUp();
    }

    private TireLike tire() {
        final ItemStack stack = this.blockEntity.getHeldItem();
        return stack.isEmpty() ? null : stack.get(OffroadDataComponents.TIRE);
    }
}
