package ink.astrius.create_avionics.compat.aeronautics.peripherals.generic;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasHolder;
import ink.astrius.create_avionics.api.aero.GasProviderData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared peripheral for gas-output blocks (burners, vents) that fill balloons.
 * Reports gas output, signal, gas type, target amount, and balloon state. The
 * shared additional type "gas_provider" lets scripts target every heater
 * regardless of block kind.
 *
 * @cc.module gas_provider
 */
public class GasProviderGenericSource implements GenericSource {
    @Override
    public String id() {
        return "gas_provider";
    }

    /**
     * Get the current gas output rate.
     * Output = target × signal / 15 (burner), or target × efficiency × signal / 15 (vent).
     * Added to the attached balloon's target volume every game tick.
     *
     * @return The gas output rate in m³ per tick (multiply by 20 for m³/s).
     */
    @LuaFunction
    public final double getGasOutput(GasProviderData data) {
        return data.getGasOutput();
    }

    /**
     * Check whether the provider can currently output gas.
     *
     * @return True if active.
     */
    @LuaFunction
    public final boolean isActive(GasProviderData data) {
        return data.canOutputGas();
    }

    /**
     * Get the redstone signal strength driving output.
     *
     * @return The signal strength, 0..15.
     */
    @LuaFunction
    public final int getSignalStrength(GasProviderData data) {
        return data.getSignalStrength();
    }

    /**
     * Get the id of the gas this provider produces.
     *
     * @return The gas type id ("steam", "default", or "unknown").
     */
    @LuaFunction
    public final String getGasType(GasProviderData data) {
        return data.getLiftingGasType().getClass().getSimpleName();
    }

    /**
     * Get the configured target gas amount.
     *
     * @return The target amount.
     */
    @LuaFunction
    public final int getTargetAmount(GasProviderData data) {
        return data.getTargetAmount();
    }

    /**
     * Set the target gas amount. Clamped internally to the scroll-value's min/max.
     *
     * @param amount The new target amount.
     */
    @LuaFunction(mainThread = true)
    public final void setTargetAmount(GasProviderData data, final int amount) {
        data.setTargetAmount(amount);
    }

    /**
     * Get the boiler efficiency. 0..1. Burners are always 1.0; vents track boiler heat.
     *
     * @return The boiler efficiency in [0, 1].
     */
    @LuaFunction
    public final double getBoilerEfficiency(GasProviderData data) {
        return data.getBoilerEfficiency();
    }

    /**
     * Check whether a balloon is currently attached.
     *
     * @return True if a balloon is present.
     */
    @LuaFunction
    public final boolean hasBalloon(GasProviderData data) {
        return data.getBalloon() != null;
    }

    /**
     * Get the attached balloon's capacity.
     *
     * @return The balloon's capacity, or 0 if none.
     */
    @LuaFunction
    public final int getBalloonCapacity(GasProviderData data) {
        final Balloon b = data.getBalloon();
        return b != null ? b.getCapacity() : 0;
    }

    /**
     * Get the balloon's currently filled volume.
     *
     * @return The filled volume, or 0 if no server-side balloon.
     */
    @LuaFunction
    public final double getBalloonFilledVolume(GasProviderData data) {
        final Balloon b = data.getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalFilledVolume() : 0.0;
    }

    /**
     * Get the balloon's target volume.
     *
     * @return The target volume, or 0 if no server-side balloon.
     */
    @LuaFunction
    public final double getBalloonTargetVolume(GasProviderData data) {
        final Balloon b = data.getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalTargetVolume() : 0.0;
    }

    /**
     * Get the per-tick volume change of the balloon.
     *
     * @return The signed volume change, or 0 if no server-side balloon.
     */
    @LuaFunction
    public final double getBalloonVolumeChange(GasProviderData data) {
        final Balloon b = data.getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalVolumeChange() : 0.0;
    }

    /**
     * Get the balloon's lift force.
     *
     * @return The lift, or 0 if no server-side balloon.
     */
    @LuaFunction
    public final double getBalloonLift(GasProviderData data) {
        final Balloon b = data.getBalloon();
        return (b instanceof final ServerBalloon sb) ? sb.getTotalLift() : 0.0;
    }

    /**
     * Get the balloon's height.
     *
     * @return The height, or 0 if no balloon.
     */
    @LuaFunction
    public final double getBalloonHeight(GasProviderData data) {
        final Balloon b = data.getBalloon();
        return b != null ? b.getHeight() : 0.0;
    }

    /**
     * Get the balloon's gas mix as a list of {type, amount} entries.
     *
     * @return A list of gas mix entries.
     */
    @LuaFunction
    public final List<Map<String, Object>> getBalloonGasMix(GasProviderData data) {
        final Balloon b = data.getBalloon();
        if (!(b instanceof final ServerBalloon sb)) return List.of();
        final List<Map<String, Object>> out = new ArrayList<>();
        for (final LiftingGasHolder h : sb.getLiftingGasHolders()) {
            out.add(Map.of("type", h.type().getClass().getSimpleName(), "amount", h.data().amount));
        }
        return out;
    }
}
