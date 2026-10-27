package ink.astrius.create_avionics.content.controller;

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

import java.util.List;

/**
 * Registers the avionics controller.
 */
public final class Controllers {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateAvionics.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAvionics.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAvionics.MOD_ID);

    public static final DeferredBlock<AvionicsControllerBlock> BLOCK = BLOCKS.register("avionics_controller",
            () -> new AvionicsControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    static {
        ITEMS.register("avionics_controller", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    }

    @SuppressWarnings("DataFlowIssue") // Block entity types are never used with a datafixer
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AvionicsControllerBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("avionics_controller",
                    () -> BlockEntityType.Builder.of(AvionicsControllerBlockEntity::new, BLOCK.get()).build(null));

    private Controllers() {
    }

    /** The deferred registers, for the loader to attach to its mod event bus. */
    public static List<DeferredRegister<?>> registers() {
        return List.of(BLOCKS, ITEMS, BLOCK_ENTITIES);
    }
}
