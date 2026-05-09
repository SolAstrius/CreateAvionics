package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;

/**
 * A torsion spring. Reports the current twist angle and angular limit, and lets
 * scripts adjust the limit while the spring is static.
 *
 * @cc.module torsion_spring
 */
public class TorsionSpringPeripheral extends SimKineticPeripheral<TorsionSpringBlockEntity>{
    public TorsionSpringPeripheral(final TorsionSpringBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "torsion_spring";
    }

    /**
     * Set the spring's angular limit. Only takes effect while the spring is static.
     * <p>Yields until the next server tick.
     *
     * @param limit The new angular limit.
     */
    @LuaFunction(mainThread = true)
    public void setLimit(final int limit) {
        if(this.blockEntity.isSpringStatic()) {
            this.blockEntity.angleInput.setValue(limit);
        }
    }

    /**
     * Get the spring's current twist angle, in degrees.
     *
     * @return The angle in degrees.
     */
    @LuaFunction
    public double getAngle() {
        return this.blockEntity.getAngle();
    }

    /**
     * Get the spring's current twist angle, in radians.
     *
     * @return The angle in radians.
     */
    @LuaFunction
    public double getAngleRad() {
        return Math.toRadians(this.blockEntity.getAngle());
    }

    /**
     * Get the spring's configured angular limit.
     *
     * @return The angular limit.
     */
    @LuaFunction
    public int getLimit() {
        return this.blockEntity.angleInput.getValue();
    }

    /**
     * Check whether the spring is currently running (non-static).
     *
     * @return True if the spring is running.
     */
    @LuaFunction
    public boolean isRunning() {
        return !this.blockEntity.isSpringStatic();
    }

}
