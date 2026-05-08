package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlock;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftBlockEntity;
import net.minecraft.core.Direction;

public class DirectionalGearshiftPeripheral extends SimPeripheral<DirectionalGearshiftBlockEntity> {

    public DirectionalGearshiftPeripheral(final DirectionalGearshiftBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "directional_gearshift";
    }

    // Source-shaft axis name, or nil when the gearshift has no kinetic input.
    @LuaFunction
    public final String getSourceAxis() {
        final Direction d = this.blockEntity.getSourceFacing();
        return d == null ? null : d.getSerializedName();
    }

    @LuaFunction
    public final boolean hasSource() {
        return this.blockEntity.getSourceFacing() != null;
    }

    @LuaFunction
    public final boolean isLeftPowered() {
        return this.blockEntity.getBlockState().getValue(DirectionalGearshiftBlock.LEFT_POWERED);
    }

    @LuaFunction
    public final boolean isRightPowered() {
        return this.blockEntity.getBlockState().getValue(DirectionalGearshiftBlock.RIGHT_POWERED);
    }

    // Output behavior of non-source faces:
    //   "forward" — left-only powered, output × +1
    //   "reverse" — right-only powered, output × -1
    //   "stop"    — both or neither powered, output × 0
    //   "neutral" — no kinetic source connected
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

    @LuaFunction
    public final float getKineticSpeed() {
        return this.blockEntity.getSpeed();
    }
}
