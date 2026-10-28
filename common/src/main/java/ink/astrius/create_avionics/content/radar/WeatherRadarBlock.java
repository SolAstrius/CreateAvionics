package ink.astrius.create_avionics.content.radar;

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
 * The weather radar. Its front (radial 1) is the way the player faced when placing it.
 */
public class WeatherRadarBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<WeatherRadarBlock> CODEC = simpleCodec(WeatherRadarBlock::new);

    public WeatherRadarBlock(final Properties properties) {
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

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new WeatherRadarBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                           final BlockEntityType<T> type) {
        if (level.isClientSide || type != Radars.WEATHER_BLOCK_ENTITY.get()) {
            return null;
        }
        final BlockEntityTicker<WeatherRadarBlockEntity> ticker = WeatherRadarBlockEntity::serverTick;
        @SuppressWarnings("unchecked") final BlockEntityTicker<T> cast = (BlockEntityTicker<T>) ticker;
        return cast;
    }
}
