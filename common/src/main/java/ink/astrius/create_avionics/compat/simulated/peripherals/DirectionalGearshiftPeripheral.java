package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlockEntity;
import net.minecraft.core.Direction;

/**
 * A redstone-controlled gearshift with separate left and right power inputs that
 * select between forward, reverse, and stop on the non-source faces.
 *
 * @cc.module directional_gearshift
 */
public class DirectionalGearshiftPeripheral extends SimPeripheral<DirectionalGearshiftBlockEntity> {

    public DirectionalGearshiftPeripheral(final DirectionalGearshiftBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "directional_gearshift";
    }

    /**
     * Source-shaft axis name, or nil when the gearshift has no kinetic input.
     *
     * @return The serialized axis name, or nil.
     */
    @LuaFunction
    public final String getSourceAxis() {
        final Direction d = this.blockEntity.getSourceFacing();
        return d == null ? null : d.getSerializedName();
    }

    /**
     * Check whether the gearshift has a kinetic source connected.
     *
     * @return True if a source is connected.
     */
    @LuaFunction
    public final boolean hasSource() {
        return this.blockEntity.getSourceFacing() != null;
    }

    /**
     * Check whether the left input is powered.
     *
     * @return True if left is powered.
     */
    @LuaFunction
    public final boolean isLeftPowered() {
        return this.blockEntity.getBlockState().getValue(DirectionalGearshiftBlock.LEFT_POWERED);
    }

    /**
     * Check whether the right input is powered.
     *
     * @return True if right is powered.
     */
    @LuaFunction
    public final boolean isRightPowered() {
        return this.blockEntity.getBlockState().getValue(DirectionalGearshiftBlock.RIGHT_POWERED);
    }

    /**
     * Get the gearshift's current mode.
     * Output behavior of non-source faces:
     *   "forward" — left-only powered, output × +1
     *   "reverse" — right-only powered, output × -1
     *   "stop"    — both or neither powered, output × 0
     *   "neutral" — no kinetic source connected
     *
     * @return The mode string.
     */
    @LuaFunction
    public final String getMode() {
        if (this.blockEntity.getSourceFacing() == null) return "neutral";
        final boolean l = this.isLeftPowered();
        final boolean r = this.isRightPowered();
        if (l && r) return "stop";
        if (l) return "forward";
        if (r) return "reverse";
        return "stop";
    }

    /**
     * Get the input shaft kinetic speed.
     *
     * @return The kinetic speed.
     */
    @LuaFunction
    public final double getKineticSpeed() {
        return this.blockEntity.getSpeed();
    }
}
