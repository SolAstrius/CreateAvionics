package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.redstone.directional_receiver.DirectionalLinkedReceiverBlockEntity;

/**
 * A directional linked receiver. Reports the bearing toward the nearest paired link.
 *
 * @cc.module directional_link
 */
public class DirectionalLinkPeripheral extends SimPeripheral<DirectionalLinkedReceiverBlockEntity>{
    public DirectionalLinkPeripheral(final DirectionalLinkedReceiverBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "directional_link";
    }

    /**
     * Get the angle to the closest paired link, in degrees.
     *
     * @return The angle in degrees.
     */
    @LuaFunction
    public double getClosestAngle() {
        return Math.toDegrees(this.blockEntity.getAngleToClosestLink());
    }

    /**
     * Get the angle to the closest paired link, in radians.
     *
     * @return The angle in radians.
     */
    @LuaFunction
    public double getClosestAngleRad() {
        return this.blockEntity.getAngleToClosestLink();
    }
}
