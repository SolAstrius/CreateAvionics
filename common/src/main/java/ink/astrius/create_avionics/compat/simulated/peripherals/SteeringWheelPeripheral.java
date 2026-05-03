package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;

public class SteeringWheelPeripheral extends SimPeripheral<SteeringWheelBlockEntity> {

    public SteeringWheelPeripheral(final SteeringWheelBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "steering_wheel";
    }

    @LuaFunction
    public boolean isHeld() {
        return this.blockEntity.held;
    }

    // Current visible wheel angle, degrees. Bounded by ±getMaxAngle().
    @LuaFunction
    public float getAngle() {
        return this.blockEntity.getAngle();
    }

    @LuaFunction
    public double getAngleRad() {
        return Math.toRadians(this.blockEntity.getAngle());
    }

    // Angle the pilot is currently commanding (what they're steering toward).
    @LuaFunction
    public float getTargetAngle() {
        return this.blockEntity.targetAngle;
    }

    @LuaFunction
    public double getTargetAngleRad() {
        return Math.toRadians(this.blockEntity.targetAngle);
    }

    // Maximum deflection in each direction, degrees. Set by the block's scroll value (1..360).
    @LuaFunction
    public int getMaxAngle() {
        return this.blockEntity.angleInput.getValue();
    }

    // Normalized to [-1, +1]. Convenient for mixing pilot input into autopilot commands.
    @LuaFunction
    public float getNormalizedAngle() {
        final int max = this.blockEntity.angleInput.getValue();
        return max == 0 ? 0 : this.blockEntity.getAngle() / (float) max;
    }
}
