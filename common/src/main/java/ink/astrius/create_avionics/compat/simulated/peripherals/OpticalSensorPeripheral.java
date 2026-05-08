package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * A forward-facing optical rangefinder. Reports whether and what it has hit,
 * the hit distance, and a scriptable max range.
 *
 * @cc.module optical_sensor
 */
public class OpticalSensorPeripheral extends SimPeripheral<OpticalSensorBlockEntity> {

    public OpticalSensorPeripheral(final OpticalSensorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "optical_sensor";
    }

    /**
     * Check whether the sensor's beam is currently hitting a block.
     *
     * @return True if a block is hit.
     */
    @LuaFunction
    public boolean hasHit() {
        return this.blockEntity.hasHit();
    }

    /**
     * Get the distance to the hit block.
     *
     * @return The hit distance.
     */
    @LuaFunction
    public double getDistance() {
        return this.blockEntity.getHitBlockDistance();
    }

    /**
     * Get the registry id of the hit block.
     *
     * @return The hit block's registry id.
     */
    @LuaFunction
    public String getBlock() {
        return BuiltInRegistries.BLOCK.getKey(this.blockEntity.getHitBlock()).toString();
    }

    /**
     * Get the sensor's current max range.
     *
     * @return The range.
     */
    @LuaFunction
    public double getRange() {
        return this.blockEntity.getLaserRange();
    }

    /**
     * Set the sensor's max range.
     * <p>Yields until the next server tick.
     *
     * @param blocks The new range in blocks.
     */
    @LuaFunction(mainThread = true)
    public final void setRange(final int blocks) {
        this.blockEntity.setRange(blocks);
    }
}
