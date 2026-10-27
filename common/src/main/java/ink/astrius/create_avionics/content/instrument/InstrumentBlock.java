package ink.astrius.create_avionics.content.instrument;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An instrument block. Faces the player who placed it; the comparator output follows the dial.
 */
public class InstrumentBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<InstrumentBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Instrument.CODEC.fieldOf("instrument").forGetter(InstrumentBlock::instrument),
            propertiesCodec()
    ).apply(i, InstrumentBlock::create));

    private final Instrument instrument;

    protected InstrumentBlock(final Instrument instrument, final Properties properties) {
        super(properties);
        this.instrument = instrument;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static InstrumentBlock create(final Instrument instrument, final Properties properties) {
        return instrument == Instrument.ALMANAC
                ? new AlmanacBlock(properties)
                : new InstrumentBlock(instrument, properties);
    }

    public Instrument instrument() {
        return this.instrument;
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.instrument.shape(state.getValue(FACING));
    }

    /**
     * Every face can hold an attachment in its centre, so a ComputerCraft wired modem (and torches,
     * levers, ...) can be mounted on any side even though the model doesn't reach the block edges.
     */
    @Override
    protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new InstrumentBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                           final BlockEntityType<T> type) {
        if (type != Instruments.BLOCK_ENTITY.get()) {
            return null;
        }
        final BlockEntityTicker<InstrumentBlockEntity> ticker = level.isClientSide
                ? InstrumentBlockEntity::clientTick
                : InstrumentBlockEntity::serverTick;
        @SuppressWarnings("unchecked") final BlockEntityTicker<T> cast = (BlockEntityTicker<T>) ticker;
        return cast;
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof final InstrumentBlockEntity be ? be.comparatorSignal() : 0;
    }
}
