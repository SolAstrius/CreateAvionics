package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.api.aero.GasProviderData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.DefaultLiftingGas;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasHolder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.SteamLiftingGas;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The shared additional type "gas_provider" lets scripts target every heater
// regardless of block kind.
public class GasProviderPeripheral<T extends SmartBlockEntity> extends SimPeripheral<T> {

    private final String typeName;

    public GasProviderPeripheral(final T blockEntity, final String typeName) {
        super(blockEntity);
        this.typeName = typeName;
    }

    @Override
    public String getType() {
        return this.typeName;
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return Set.of("gas_provider");
    }

    private GasProviderData data() {
        return (GasProviderData) this.blockEntity;
    }

    // Output = target × signal / 15 (burner), or target × efficiency × signal / 15 (vent).
    @LuaFunction
    public final double getGasOutput() {
        return this.data().getGasOutput();
    }

    @LuaFunction
    public final boolean isActive() {
        return this.data().canOutputGas();
    }

    @LuaFunction
    public final int getSignalStrength() {
        return this.data().getSignalStrength();
    }

    @LuaFunction
    public final String getGasType() {
        final LiftingGasType t = this.data().getLiftingGasType();
        if (t instanceof SteamLiftingGas) return "steam";
        if (t instanceof DefaultLiftingGas) return "default";
        return "unknown";
    }

    @LuaFunction
    public final int getTargetAmount() {
        return this.data().getTargetAmount();
    }

    // Clamped internally to the scroll-value's min/max.
    @LuaFunction
    public final void setTargetAmount(final int amount) {
        this.data().setTargetAmount(amount);
    }

    // 0..1. Burners are always 1.0; vents track boiler heat.
    @LuaFunction
    public final double getBoilerEfficiency() {
        return this.data().getBoilerEfficiency();
    }

    @LuaFunction
    public final boolean hasBalloon() {
        return this.data().getBalloon() != null;
    }

    @LuaFunction
    public final int getBalloonCapacity() {
        final Balloon b = this.data().getBalloon();
        return b != null ? b.getCapacity() : 0;
    }

    @LuaFunction
    public final double getBalloonFilledVolume() {
        final Balloon b = this.data().getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalFilledVolume() : 0.0;
    }

    @LuaFunction
    public final double getBalloonTargetVolume() {
        final Balloon b = this.data().getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalTargetVolume() : 0.0;
    }

    @LuaFunction
    public final double getBalloonVolumeChange() {
        final Balloon b = this.data().getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalVolumeChange() : 0.0;
    }

    @LuaFunction
    public final double getBalloonLift() {
        final Balloon b = this.data().getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalLift() : 0.0;
    }

    @LuaFunction
    public final float getBalloonHeight() {
        final Balloon b = this.data().getBalloon();
        return b != null ? b.getHeight() : 0f;
    }

    @LuaFunction
    public final List<Map<String, Object>> getBalloonGasMix() {
        final Balloon b = this.data().getBalloon();
        if (!(b instanceof final ServerBalloon sb)) return List.of();
        final List<Map<String, Object>> out = new ArrayList<>();
        for (final LiftingGasHolder h : sb.getLiftingGasHolders()) {
            final LiftingGasType t = h.type();
            final String id = (t instanceof SteamLiftingGas) ? "steam"
                            : (t instanceof DefaultLiftingGas) ? "default"
                            : "unknown";
            out.add(Map.of("type", id, "amount", h.data().amount));
        }
        return out;
    }
}
