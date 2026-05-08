package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;

/**
 * A docking connector. Reports the name of the sub-level it is currently docked to.
 *
 * @cc.module docking_connector
 */
public class DockingConnectorPeripheral extends SimPeripheral<DockingConnectorBlockEntity>{
    public DockingConnectorPeripheral(final DockingConnectorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "docking_connector";
    }

    /**
     * Get the name of the sub-level on the other side of this connector.
     *
     * @return The connected sub-level's name, or an empty string if not docked.
     */
    @LuaFunction
    public String getConnectedName() {
        if (this.blockEntity.otherConnectorPosition != null) {
            final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity.getLevel(), this.blockEntity.otherConnectorPosition);
            if (subLevel != null && subLevel.getName() != null) {
                return subLevel.getName();
            }
        }

        return "";
    }
}
