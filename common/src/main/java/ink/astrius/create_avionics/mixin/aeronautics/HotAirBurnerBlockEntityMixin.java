package ink.astrius.create_avionics.mixin.aeronautics;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity;
import ink.astrius.create_avionics.api.aero.GasProviderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HotAirBurnerBlockEntity.class, remap = false)
public abstract class HotAirBurnerBlockEntityMixin implements GasProviderData {

    @Shadow protected ScrollValueBehaviour hotAirAmountBehaviour;

    @Override
    public int getTargetAmount() {
        return this.hotAirAmountBehaviour.getValue();
    }

    @Override
    public void setTargetAmount(final int amount) {
        this.hotAirAmountBehaviour.setValue(amount);
    }
}
