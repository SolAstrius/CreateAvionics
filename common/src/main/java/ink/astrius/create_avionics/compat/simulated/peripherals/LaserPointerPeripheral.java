package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;

/**
 * A pointable laser. Reports its facing, firing state, redstone output,
 * range, and color; color and rainbow mode are scriptable.
 *
 * @cc.module laser_pointer
 */
public class LaserPointerPeripheral extends SimPeripheral<LaserPointerBlockEntity> {

    public LaserPointerPeripheral(final LaserPointerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "laser_pointer";
    }

    /**
     * Get the laser's facing direction.
     *
     * @return The serialized direction name.
     */
    @LuaFunction
    public final String getAxis() {
        return this.blockEntity.getDirection().getSerializedName();
    }

    /**
     * Check whether the laser is currently firing.
     *
     * @return True if firing.
     */
    @LuaFunction
    public final boolean isFiring() {
        return this.blockEntity.shouldCast();
    }

    /**
     * Get the output redstone power.
     *
     * @return Output redstone power 0..15 (post-inversion).
     */
    @LuaFunction
    public final int getPower() {
        return this.blockEntity.getPower();
    }

    /**
     * Get the laser's effective range.
     *
     * @return The laser range.
     */
    @LuaFunction
    public final double getRange() {
        return this.blockEntity.getLaserRange();
    }

    /**
     * Get the laser color.
     *
     * @return ARGB-packed color int as used by the block.
     */
    @LuaFunction
    public final int getColor() {
        return this.blockEntity.getLaserColor();
    }

    /**
     * Set the laser color.
     *
     * @param color ARGB-packed color int.
     */
    @LuaFunction(mainThread = true)
    public final void setColor(final int color) {
        this.blockEntity.setLaserColor(color);
    }

    /**
     * Check whether rainbow mode is enabled.
     *
     * @return True if rainbow mode is on.
     */
    @LuaFunction
    public final boolean isRainbow() {
        return this.blockEntity.isRainbow();
    }

    /**
     * Toggle rainbow color mode.
     *
     * @param rainbow True to enable rainbow mode.
     */
    @LuaFunction(mainThread = true)
    public final void setRainbow(final boolean rainbow) {
        this.blockEntity.setRainbow(rainbow);
    }
}
