package ink.astrius.create_avionics.registry;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.create.net.RailModemBlock;
import ink.astrius.create_avionics.compat.create.net.RailModemPoint;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's blocks.
 *
 * <p>This is the first content Create: Avionics registers at all —
 * everything else it does is a mixin into an upstream block entity plus a
 * peripheral surface. The rail modem needs a real block because it is a
 * thing a player places on a track, not a capability grafted onto
 * something Create already placed.</p>
 */
public final class AvionicsBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateAvionics.MOD_ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateAvionics.MOD_ID);

    public static final DeferredBlock<RailModemBlock> RAIL_MODEM = BLOCKS.register(
            "rail_modem",
            () -> new RailModemBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    /**
     * The rail modem's item.
     *
     * <p>A {@link TrackTargetingBlockItem} rather than a plain
     * {@link BlockItem}: that is what gives it Create's own
     * click-a-rail-to-target-it placement, including the overlap checks
     * that stop two edge points landing on the same spot. Reimplementing
     * that would mean a second, subtly different placement UX for no
     * reason.</p>
     */
    public static final DeferredItem<BlockItem> RAIL_MODEM_ITEM = ITEMS.register(
            "rail_modem",
            () -> new TrackTargetingBlockItem(
                    RAIL_MODEM.get(), new Item.Properties(), edgePointType()));

    private AvionicsBlocks() {
    }

    /**
     * Force {@link RailModemPoint}'s registration to run.
     *
     * <p>{@code EdgePointType.register} happens in that class's static
     * initialiser, and Create looks types up by id when deserialising a
     * graph. Touching it from here guarantees it has happened before any
     * saved point is read back, rather than depending on whatever else
     * might have loaded the class first.</p>
     */
    private static EdgePointType<RailModemPoint> edgePointType() {
        return RailModemPoint.TYPE;
    }

    /** @return The block registered under the given id, for datagen and tests. */
    public static Block block() {
        return RAIL_MODEM.get();
    }
}
