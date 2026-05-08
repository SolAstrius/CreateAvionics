package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;

public class RopeWinchPeripheral extends SimPeripheral<RopeWinchBlockEntity> {

    public RopeWinchPeripheral(final RopeWinchBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "rope_winch";
    }

    // Current rope extension in blocks.
    @LuaFunction
    public final int getLength() {
        return this.blockEntity.getCurrentValue();
    }

    @LuaFunction
    public final int getMaxLength() {
        return this.blockEntity.getMaxValue();
    }

    @LuaFunction
    public final int getMinLength() {
        return this.blockEntity.getMinValue();
    }

    // Per-tick extension delta. Positive extends, negative retracts. Driven
    // by kinetic input speed; the winch has no external setpoint, so to
    // command rope length a script must drive the kinetics (e.g., via an
    // analog_transmission or steering_wheel-coupled gearbox).
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getMovementSpeed();
    }

    @LuaFunction
    public final double getKineticSpeed() {
        return this.blockEntity.getSpeed();
    }
}
