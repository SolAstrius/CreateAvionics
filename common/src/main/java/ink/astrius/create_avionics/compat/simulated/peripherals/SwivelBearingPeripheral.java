package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;

/**
 * A swivel bearing. Reports the target angle that the bearing is steering toward.
 *
 * @cc.module swivel_bearing
 */
public class SwivelBearingPeripheral extends SimPeripheral<SwivelBearingBlockEntity> {

    public SwivelBearingPeripheral(final SwivelBearingBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "swivel_bearing";
    }

    /**
     * Get the bearing's target angle, in degrees.
     *
     * @return The target angle in degrees.
     */
    @LuaFunction
    public double getTargetAngle() {
        return this.blockEntity.getTargetAngleDegrees();
    }

    /**
     * Get the bearing's target angle, in radians.
     *
     * @return The target angle in radians.
     */
    @LuaFunction
    public double getTargetAngleRad() {
        return Math.toRadians(this.blockEntity.getTargetAngleDegrees());
    }
}
