package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import net.minecraft.util.Mth;

/**
 * A 16-position physical lever. Its state is the analog redstone signal it
 * emits (before any block-state inversion); writing it both updates the lever
 * and plays the click sound.
 *
 * @cc.module throttle_lever
 */
public class ThrottleLeverPeripheral extends SimPeripheral<ThrottleLeverBlockEntity> {

    public ThrottleLeverPeripheral(final ThrottleLeverBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "throttle_lever";
    }

    /**
     * Get the current lever state.
     *
     * @return The lever state, 0..15.
     */
    @LuaFunction
    public final int getState() {
        return this.blockEntity.getState();
    }

    /**
     * Drive the lever to a new state. A player can still change it afterwards —
     * unlike {@code analog_transmission} there is no externally-controlled flag.
     *
     * @param signal The target state, clamped to 0..15.
     */
    @LuaFunction(mainThread = true)
    public final void setSignal(final int signal) {
        this.blockEntity.setSignal(Mth.clamp(signal, 0, 15));
    }
}
