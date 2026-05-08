package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A fuel-burning engine. Reports remaining burn time, fuel/superheat state,
 * lit state, and the kinetic output it generates.
 *
 * @cc.module portable_engine
 */
public class PortableEnginePeripheral extends SimPeripheral<PortableEngineBlockEntity> {

    public PortableEnginePeripheral(final PortableEngineBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "portable_engine";
    }

    // --- Fuel state ---

    /**
     * Get the burn time remaining on the active fuel item.
     * Burn time remaining on the currently-burning fuel item, in ticks. While
     * lit, decrements by exactly 1 per game tick (20/s) regardless of signal,
     * speed, stress, or superheating. Do not finite-difference this to
     * "measure" a fuel rate: the rate is a game constant, and sampling across
     * a refill event from the inventory will jump the value upward and make
     * the naive rate look wrong.
     *
     * @return The active fuel's remaining burn time in ticks.
     */
    @LuaFunction
    public int getBurnTime() {
        return this.blockEntity.getCurrentBurnTime();
    }

    /**
     * Get the total burn time across the active fuel plus the inventory.
     * Total burn time across the currently-burning fuel plus the remaining
     * stack in the inventory, in ticks. Divide by 20 for seconds of
     * endurance; this is the right field to use for "how long until I run
     * out" rather than getBurnTime(), which only covers the active item.
     *
     * @return The total remaining burn time in ticks.
     */
    @LuaFunction
    public int getTotalBurnTime() {
        return this.blockEntity.getTotalBurnTime();
    }

    /**
     * Check whether the engine is superheated.
     *
     * @return True if superheated.
     */
    @LuaFunction
    public boolean isSuperHeated() {
        return this.blockEntity.isSuperHeated();
    }

    /**
     * Check whether the active fuel is infinite.
     *
     * @return True if the active fuel is infinite.
     */
    @LuaFunction
    public boolean isCurrentFuelInfinite() {
        return this.blockEntity.isCurrentFuelInfinite();
    }

    /**
     * Check whether the total fuel supply is infinite.
     *
     * @return True if the total fuel is infinite.
     */
    @LuaFunction
    public boolean isTotalFuelInfinite() {
        return this.blockEntity.isTotalFuelInfinite();
    }

    // --- Engine state ---

    /**
     * Check whether the engine is currently lit.
     *
     * @return True if lit.
     */
    @LuaFunction
    public boolean isLit() {
        return this.blockEntity.getBlockState().getValue(BlockStateProperties.LIT);
    }

    // --- Kinetic output ---

    /**
     * Get the engine's current output shaft speed.
     *
     * @return The kinetic speed.
     */
    @LuaFunction
    public double getSpeed() {
        return this.blockEntity.getSpeed();
    }

    /**
     * Get the engine's generated speed.
     *
     * @return The generated speed.
     */
    @LuaFunction
    public double getGeneratedSpeed() {
        return this.blockEntity.getGeneratedSpeed();
    }

    /**
     * Get the stress capacity the engine adds to the network.
     *
     * @return The added stress capacity.
     */
    @LuaFunction
    public double getStressCapacity() {
        return this.blockEntity.calculateAddedStressCapacity();
    }
}
