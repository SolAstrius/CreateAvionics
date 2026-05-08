package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PortableEnginePeripheral extends SimPeripheral<PortableEngineBlockEntity> {

    public PortableEnginePeripheral(final PortableEngineBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "portable_engine";
    }

    // --- Fuel state ---

    // Burn time remaining on the currently-burning fuel item, in ticks. While
    // lit, decrements by exactly 1 per game tick (20/s) regardless of signal,
    // speed, stress, or superheating. Do not finite-difference this to
    // "measure" a fuel rate: the rate is a game constant, and sampling across
    // a refill event from the inventory will jump the value upward and make
    // the naive rate look wrong.
    @LuaFunction
    public int getBurnTime() {
        return this.blockEntity.getCurrentBurnTime();
    }

    // Total burn time across the currently-burning fuel plus the remaining
    // stack in the inventory, in ticks. Divide by 20 for seconds of
    // endurance; this is the right field to use for "how long until I run
    // out" rather than getBurnTime(), which only covers the active item.
    @LuaFunction
    public int getTotalBurnTime() {
        return this.blockEntity.getTotalBurnTime();
    }

    @LuaFunction
    public boolean isSuperHeated() {
        return this.blockEntity.isSuperHeated();
    }

    @LuaFunction
    public boolean isCurrentFuelInfinite() {
        return this.blockEntity.isCurrentFuelInfinite();
    }

    @LuaFunction
    public boolean isTotalFuelInfinite() {
        return this.blockEntity.isTotalFuelInfinite();
    }

    // --- Engine state ---

    @LuaFunction
    public boolean isLit() {
        return this.blockEntity.getBlockState().getValue(BlockStateProperties.LIT);
    }

    // --- Kinetic output ---

    @LuaFunction
    public double getSpeed() {
        return this.blockEntity.getSpeed();
    }

    @LuaFunction
    public double getGeneratedSpeed() {
        return this.blockEntity.getGeneratedSpeed();
    }

    @LuaFunction
    public double getStressCapacity() {
        return this.blockEntity.calculateAddedStressCapacity();
    }
}
