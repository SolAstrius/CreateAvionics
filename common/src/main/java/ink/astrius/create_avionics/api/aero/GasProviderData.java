package ink.astrius.create_avionics.api.aero;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;

public interface GasProviderData extends BlockEntityLiftingGasProvider {

    int getSignalStrength();

    int getTargetAmount();

    void setTargetAmount(int amount);

    default double getBoilerEfficiency() {
        return 1.0;
    }
}
