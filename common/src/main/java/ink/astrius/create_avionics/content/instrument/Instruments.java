package ink.astrius.create_avionics.content.instrument;

import ink.astrius.create_avionics.CreateAvionics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registers the instrument blocks, their items and the shared block entity type.
 */
public final class Instruments {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateAvionics.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAvionics.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAvionics.MOD_ID);

    public static final Map<Instrument, DeferredBlock<InstrumentBlock>> BLOCKS_BY_INSTRUMENT = new EnumMap<>(Instrument.class);

    static {
        for (final Instrument instrument : Instrument.values()) {
            final DeferredBlock<InstrumentBlock> block = BLOCKS.register(instrument.id(),
                    () -> InstrumentBlock.create(instrument, properties(instrument)));
            ITEMS.register(instrument.id(), () -> new BlockItem(block.get(), new Item.Properties()));
            BLOCKS_BY_INSTRUMENT.put(instrument, block);
        }
    }

    @SuppressWarnings("DataFlowIssue") // Block entity types are never used with a datafixer
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstrumentBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("instrument", () -> BlockEntityType.Builder.of(InstrumentBlockEntity::new,
                    BLOCKS_BY_INSTRUMENT.values().stream().map(DeferredBlock::get).toArray(InstrumentBlock[]::new)
            ).build(null));

    private Instruments() {
    }

    /** The deferred registers, for the loader to attach to its mod event bus. */
    public static List<DeferredRegister<?>> registers() {
        return List.of(BLOCKS, ITEMS, BLOCK_ENTITIES);
    }

    private static BlockBehaviour.Properties properties(final Instrument instrument) {
        final boolean wooden = instrument == Instrument.ALMANAC;
        return BlockBehaviour.Properties.of()
                .mapColor(wooden ? MapColor.WOOD : instrument == Instrument.BAROMETER ? MapColor.GOLD : MapColor.STONE)
                .strength(1.5f)
                .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                .noOcclusion();
    }

}
