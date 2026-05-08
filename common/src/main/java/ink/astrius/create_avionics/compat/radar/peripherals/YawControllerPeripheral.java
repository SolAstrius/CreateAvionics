package ink.astrius.create_avionics.compat.radar.peripherals;

import com.happysg.radar.block.controller.yaw.AutoYawControllerBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

public class YawControllerPeripheral extends SimPeripheral<AutoYawControllerBlockEntity> {

    public YawControllerPeripheral(final AutoYawControllerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "yaw_controller";
    }

    // Manual aim. WeaponFiringControl will keep overriding this if the controller is
    // still in auto mode — call stopAuto() first to take and keep manual control.
    @LuaFunction(mainThread = true)
    public final void setAngle(final double angle) {
        this.blockEntity.setTargetAngle((float) angle);
    }

    @LuaFunction
    public final double getAngle() {
        return this.blockEntity.getTargetAngle();
    }

    @LuaFunction(mainThread = true)
    public final void stopAuto() {
        this.blockEntity.stopAuto();
    }
}
