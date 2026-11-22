package ink.astrius.create_avionics.content.controller;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemLocalPeripheral;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemPeripheral;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/**
 * The avionics controller's transponder. The transponder is the ship's: switching any controller
 * switches every controller on that ship. While it is on, a radar that hits this ship reads its
 * name; while it is off, the ship shows up as an unidentified contact.
 *
 * @cc.module avionics_controller
 */
public class AvionicsControllerPeripheral extends WiredModemPeripheral {

    private final AvionicsControllerBlockEntity blockEntity;

    public AvionicsControllerPeripheral(
        final AvionicsControllerBlockEntity blockEntity
    ) {
        super(
            new ModemState(),
            blockEntity.bridge(),
            new WiredModemLocalPeripheral(side -> null),
            blockEntity
        );

        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "avionics_controller";
    }

    @Override
    public Vec3 getPosition() {
        return Vec3.atCenterOf(this.blockEntity.getBlockPos());
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

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("peripheral_hub");
    }

    // The controller is also published on the ship network.
    // Don't recursively attach its modem when it is discovered as a remote peripheral.
    // Don't attach other computers
    @Override
    public void attach(final IComputerAccess computer) {
        if (!ShipBridge.CONTROLLER_NAME.equals(computer.getAttachmentName())) {
            super.attach(computer);
        }
    }

    @Override
    public void detach(final IComputerAccess computer) {
        if (!ShipBridge.CONTROLLER_NAME.equals(computer.getAttachmentName())) {
            super.detach(computer);
        }
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
