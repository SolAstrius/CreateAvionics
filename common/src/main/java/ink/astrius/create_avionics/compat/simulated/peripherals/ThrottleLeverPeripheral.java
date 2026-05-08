package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import net.minecraft.util.Mth;

public class ThrottleLeverPeripheral extends SimPeripheral<ThrottleLeverBlockEntity> {

    public ThrottleLeverPeripheral(final ThrottleLeverBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "throttle_lever";
    }

    // Returns the lever state 0..15. Equals the analog redstone signal the
    // block emits before any block-state inversion is applied.
    @LuaFunction
    public final int getState() {
        return this.blockEntity.getState();
    }

    // setSignal mirrors the upstream public setter: drives the state, plays
    // the click sound, and sendData. Unlike analog_transmission there is no
    // externallyControlled flag — a player can still change the lever.
    @LuaFunction(mainThread = true)
    public final void setSignal(final int signal) {
        this.blockEntity.setSignal(Mth.clamp(signal, 0, 15));
    }
}
