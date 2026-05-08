package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;

public class LaserPointerPeripheral extends SimPeripheral<LaserPointerBlockEntity> {

    public LaserPointerPeripheral(final LaserPointerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "laser_pointer";
    }

    @LuaFunction
    public final String getAxis() {
        return this.blockEntity.getDirection().getSerializedName();
    }

    @LuaFunction
    public final boolean isFiring() {
        return this.blockEntity.shouldCast();
    }

    // Output redstone power 0..15 (post-inversion).
    @LuaFunction
    public final int getPower() {
        return this.blockEntity.getPower();
    }

    @LuaFunction
    public final float getRange() {
        return this.blockEntity.getLaserRange();
    }

    // ARGB-packed color int as used by the block.
    @LuaFunction
    public final int getColor() {
        return this.blockEntity.getLaserColor();
    }

    @LuaFunction(mainThread = true)
    public final void setColor(final int color) {
        this.blockEntity.setLaserColor(color);
    }

    @LuaFunction
    public final boolean isRainbow() {
        return this.blockEntity.isRainbow();
    }

    @LuaFunction(mainThread = true)
    public final void setRainbow(final boolean rainbow) {
        this.blockEntity.setRainbow(rainbow);
    }
}
