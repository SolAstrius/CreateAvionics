package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;

/**
 * A nameplate that exposes its displayed name for read and write.
 *
 * @cc.module name_plate
 */
public class NamePlatePeripheral extends SimPeripheral<NameplateBlockEntity>{
    public NamePlatePeripheral(final NameplateBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "name_plate";
    }

    /**
     * Set the nameplate's displayed name.
     * <p>Yields until the next server tick.
     *
     * @param newName The new name.
     */
    @LuaFunction(mainThread = true)
    public void setName(final String newName) {
        this.blockEntity.setName(newName, true, null);
    }

    /**
     * Get the nameplate's displayed name.
     *
     * @return The current name.
     */
    @LuaFunction
    public String getName() {
        return this.blockEntity.getName();
    }
}
