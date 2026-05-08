package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

import java.util.Set;

// Shared peripheral for all small propeller variants (wooden, andesite, smart).
// Primary type follows the block id; the additional "propeller" type lets
// scripts target every variant uniformly.
public class PropellerPeripheral<T extends BasePropellerBlockEntity> extends SimPeripheral<T> {

    private final String typeName;

    public PropellerPeripheral(final T blockEntity, final String typeName) {
        super(blockEntity);
        this.typeName = typeName;
    }

    @Override
    public String getType() {
        return this.typeName;
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("propeller");
    }

    @LuaFunction
    public final String getAxis() {
        return this.blockEntity.getBlockDirection().getSerializedName();
    }

    @LuaFunction
    public final float getKineticSpeed() {
        return this.blockEntity.getSpeed();
    }

    // Smoothed angular speed used for visuals; lags getKineticSpeed by ~0.15
    // exponential lerp.
    @LuaFunction
    public final float getRotationSpeed() {
        return this.blockEntity.rotationSpeed;
    }

    // Direction-independent thrust (config-driven × current speed). Sign
    // tracks the kinetic input.
    @LuaFunction
    public final double getThrust() {
        return this.blockEntity.getThrust();
    }

    @LuaFunction
    public final double getAirflow() {
        return this.blockEntity.getAirflow();
    }

    @LuaFunction
    public final boolean isActive() {
        return this.blockEntity.isActive();
    }
}
