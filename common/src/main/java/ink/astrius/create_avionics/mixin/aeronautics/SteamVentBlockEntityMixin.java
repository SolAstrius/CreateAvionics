package ink.astrius.create_avionics.mixin.aeronautics;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import ink.astrius.create_avionics.api.aero.GasProviderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SteamVentBlockEntity.class, remap = false)
public abstract class SteamVentBlockEntityMixin implements GasProviderData {

    @Shadow protected ScrollValueBehaviour steamAmountBehaviour;
    @Shadow private double efficiency;
    @Shadow protected int signalStrength;

    @Override
    public int getSignalStrength() {
        return this.signalStrength;
    }

    @Override
    public int getTargetAmount() {
        return this.steamAmountBehaviour.getValue();
    }

    @Override
    public void setTargetAmount(final int amount) {
        this.steamAmountBehaviour.setValue(amount);
    }

    @Override
    public double getBoilerEfficiency() {
        return this.efficiency;
    }
}
