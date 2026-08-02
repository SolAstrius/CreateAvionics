package ink.astrius.create_avionics.compat.create.net;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import ink.astrius.create_avionics.registry.AvionicsBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Rail Modem: a station on the rail medium.
 *
 * <p>Placed against track the same way Create's own signals and observers
 * are — its block item is a {@code TrackTargetingBlockItem}, so clicking
 * a rail selects that rail and this block lands beside it already bound
 * to the right point on the graph.</p>
 *
 * <p>{@link #LIT} is cosmetic: it tracks recent traffic so a player can
 * see at a glance which modems are actually carrying anything, the way a
 * link light does on real network hardware. Nothing reads it.</p>
 */
public class RailModemBlock extends Block implements IBE<RailModemBlockEntity>, IWrenchable {

    /** True briefly after this modem transmits or receives. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public RailModemBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(LIT));
    }

    @Override
    public Class<RailModemBlockEntity> getBlockEntityClass() {
        return RailModemBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RailModemBlockEntity> getBlockEntityType() {
        return AvionicsBlockEntities.RAIL_MODEM.get();
    }

    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
                         final BlockState newState, final boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }
}
