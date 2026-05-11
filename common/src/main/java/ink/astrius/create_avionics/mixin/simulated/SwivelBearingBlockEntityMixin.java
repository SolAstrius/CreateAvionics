package ink.astrius.create_avionics.mixin.simulated;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity.LockingSetting;
import ink.astrius.create_avionics.api.simulated.SwivelBearingExt;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SwivelBearingBlockEntity.class, remap = false)
public abstract class SwivelBearingBlockEntityMixin extends BlockEntity implements SwivelBearingExt {

    @Shadow private ScrollOptionBehaviour<LockingSetting> lockedDefaultOption;

    private SwivelBearingBlockEntityMixin() {
        super(null, null, null);
    }

    @Override
    public String createAvionics$getLockingMode() {
        return this.lockedDefaultOption.get().name().toLowerCase();
    }

    @Override
    public void createAvionics$setLockingModeOrdinal(final int ordinal) {
        this.lockedDefaultOption.setValue(ordinal);
    }

    @Override
    public boolean createAvionics$isLocking() {
        return this.getBlockState().getValue(BlockStateProperties.POWERED);
    }
}
