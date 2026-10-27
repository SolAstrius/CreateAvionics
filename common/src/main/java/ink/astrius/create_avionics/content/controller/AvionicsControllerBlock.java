package ink.astrius.create_avionics.content.controller;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The avionics controller. The first one placed on a ship sets the ship's bow to the way it
 * faces; the arrow on top of every controller points at the bow, and a wrench moves the bow to
 * that controller's new facing.
 */
public class AvionicsControllerBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {

    /** Where the ship's bow is, seen from this controller: drives the arrow on top. */
    public enum BowSide implements StringRepresentable {
        FORE, STARBOARD, AFT, PORT;

        public static BowSide of(final Direction bow, final Direction facing) {
            if (bow == facing) {
                return FORE;
            }
            if (bow == facing.getOpposite()) {
                return AFT;
            }
            return bow == facing.getClockWise() ? STARBOARD : PORT;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<BowSide> BOW = EnumProperty.create("bow", BowSide.class);

    public static final MapCodec<AvionicsControllerBlock> CODEC = simpleCodec(AvionicsControllerBlock::new);

    public AvionicsControllerBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BOW, BowSide.FORE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BOW);
    }

    /** Points the way the player is looking, so placing it while facing the bow marks the nose. */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            final @Nullable LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof final Player player
                && level.getBlockEntity(pos) instanceof final AvionicsControllerBlockEntity be) {
            be.placedBy(player.getUUID());
        }
    }

    /** Turn the controller and make its new facing the ship's bow. Sneak-wrench picks it up as usual. */
    @Override
    public InteractionResult onWrenched(final BlockState state, final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final BlockPos pos = context.getClickedPos();
        level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof final AvionicsControllerBlockEntity be) {
            be.takeBow();
        }
        IWrenchable.playRotateSound(level, pos);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable("create_avionics.controller.bow_changed"), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** A cable or modem placed next to the controller connects straight away. */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbour,
                                   final BlockPos neighbourPos, final boolean moving) {
        super.neighborChanged(state, level, pos, neighbour, neighbourPos, moving);
        if (level.getBlockEntity(pos) instanceof final AvionicsControllerBlockEntity be) {
            be.rescanSoon();
        }
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
