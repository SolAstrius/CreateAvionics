package ink.astrius.create_avionics.registry;

import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.compat.create.net.RailModemBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's block entity types. */
public final class AvionicsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAvionics.MOD_ID);

    /**
     * Create's {@code SmartBlockEntity} takes its own type as a
     * constructor argument, so this cannot be a plain constructor
     * reference — the lambda closes over the holder instead, which is
     * only dereferenced once the type is being built.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RailModemBlockEntity>> RAIL_MODEM =
            BLOCK_ENTITIES.register("rail_modem",
                    () -> BlockEntityType.Builder
                            .of(RailModemBlockEntity::new, AvionicsBlocks.RAIL_MODEM.get())
                            .build(null));

    private AvionicsBlockEntities() {
    }
}
