package ink.astrius.create_avionics.content.controller;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * The avionics controller. Its facing marks the ship's nose: place it pointing forward.
 */
public class AvionicsControllerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<AvionicsControllerBlock> CODEC = simpleCodec(AvionicsControllerBlock::new);

    public AvionicsControllerBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Points the way the player is looking, so placing it while facing the bow marks the nose. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new AvionicsControllerBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                           final BlockEntityType<T> type) {
        if (level.isClientSide || type != Controllers.BLOCK_ENTITY.get()) {
            return null;
        }
        final BlockEntityTicker<AvionicsControllerBlockEntity> ticker = AvionicsControllerBlockEntity::serverTick;
        @SuppressWarnings("unchecked") final BlockEntityTicker<T> cast = (BlockEntityTicker<T>) ticker;
        return cast;
    }
}
