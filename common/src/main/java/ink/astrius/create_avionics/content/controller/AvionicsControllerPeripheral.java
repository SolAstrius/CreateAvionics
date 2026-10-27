package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

/**
 * The avionics controller's transponder. The transponder is the ship's: switching any controller
 * switches every controller on that ship. While it is on, a radar that hits this ship reads its
 * name; while it is off, the ship shows up as an unidentified contact.
 *
 * @cc.module avionics_controller
 */
public class AvionicsControllerPeripheral implements IPeripheral {

    private final AvionicsControllerBlockEntity blockEntity;

    public AvionicsControllerPeripheral(final AvionicsControllerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "avionics_controller";
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other instanceof final AvionicsControllerPeripheral o && o.blockEntity == this.blockEntity;
    }

    /**
     * Check whether the ship's transponder is on.
     *
     * @return True if it is transmitting.
     */
    @LuaFunction
    public final boolean isTransponderOn() {
        return this.blockEntity.transponderOn();
    }

    /**
     * Switch the ship's transponder on or off.
     *
     * @param on True to transmit, false for radio silence.
     */
    @LuaFunction(mainThread = true)
    public final void setTransponder(final boolean on) {
        this.blockEntity.setShipTransponder(on);
    }
}
