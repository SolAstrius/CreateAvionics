package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity;

/**
 * A modulating linked receiver. Reports the distance to the nearest paired link.
 *
 * @cc.module modulating_link
 */
public class ModulatingLinkPeripheral extends SimPeripheral<ModulatingLinkedReceiverBlockEntity>{
    public ModulatingLinkPeripheral(final ModulatingLinkedReceiverBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "modulating_link";
    }

    /**
     * Get the distance to the closest paired link.
     *
     * @return The distance.
     */
    @LuaFunction
    public double getClosestDistance() {
        return this.blockEntity.getDistanceToClosest();
    }
}
