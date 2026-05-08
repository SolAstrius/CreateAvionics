package ink.astrius.create_avionics.compat.radar.peripherals;

import com.happysg.radar.block.controller.firing.FireControllerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

public class FireControllerPeripheral extends SimPeripheral<FireControllerBlockEntity> {

    public FireControllerPeripheral(final FireControllerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "fire_controller";
    }

    @LuaFunction
    public final boolean isPowered() {
        return this.blockEntity.isPowered();
    }

    @LuaFunction(mainThread = true)
    public final void fireOn() {
        this.blockEntity.setPowered(true);
    }

    @LuaFunction(mainThread = true)
    public final void fireOff() {
        this.blockEntity.setPowered(false);
    }

    @LuaFunction(mainThread = true)
    public final void setPowered(final boolean powered) {
        this.blockEntity.setPowered(powered);
    }

    // Heartbeat. Calling repeatedly while you want fire active refreshes
    // lastCommandTick so the firing pulse doesn't time out.
    @LuaFunction(mainThread = true)
    public final void keepFiring() {
        this.blockEntity.setPowered(true);
    }
}
