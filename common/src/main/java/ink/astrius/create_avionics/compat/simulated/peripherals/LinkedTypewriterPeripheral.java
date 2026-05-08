package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import ink.astrius.create_avionics.api.simulated.LinkedTypewriterExt;

import java.util.List;

/**
 * A typewriter that forwards keypresses to attached computers and exposes
 * the currently held keys.
 *
 * @cc.module linked_typewriter
 */
public class LinkedTypewriterPeripheral extends SimPeripheral<LinkedTypewriterBlockEntity> {

    public LinkedTypewriterPeripheral(final LinkedTypewriterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "linked_typewriter";
    }

    @Override
    public void attach(IComputerAccess computer) {
        ((LinkedTypewriterExt) this.blockEntity).getComputerHandler().attach(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        ((LinkedTypewriterExt) this.blockEntity).getComputerHandler().detach(computer);
    }

    /**
     * Get the list of currently pressed key codes.
     *
     * @return A list of key codes.
     */
    @LuaFunction
    public final List<Integer> getPressedKeyCodes() {
        return this.blockEntity.getPressedKeys();
    }
}
