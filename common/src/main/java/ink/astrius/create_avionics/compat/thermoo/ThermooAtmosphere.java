package ink.astrius.create_avionics.compat.thermoo;

import com.github.thedeathlycow.thermoo.api.environment.EnvironmentLookup;
import com.github.thedeathlycow.thermoo.api.environment.component.EnvironmentComponentTypes;
import com.github.thedeathlycow.thermoo.api.util.TemperatureRecord;
import com.github.thedeathlycow.thermoo.api.util.TemperatureUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.Level;

import java.util.OptionalDouble;

/**
 * Reads Thermoo's environment at a position. Only referenced when Thermoo is loaded.
 */
public final class ThermooAtmosphere {

    private ThermooAtmosphere() {
    }

    public static OptionalDouble temperature(final Level level, final BlockPos pos) {
        final TemperatureRecord record = components(level, pos).get(EnvironmentComponentTypes.TEMPERATURE);
        return record == null ? OptionalDouble.empty() : OptionalDouble.of(record.valueInUnit(TemperatureUnit.CELSIUS));
    }

    public static OptionalDouble humidity(final Level level, final BlockPos pos) {
        final Double rh = components(level, pos).get(EnvironmentComponentTypes.RELATIVE_HUMIDITY);
        return rh == null ? OptionalDouble.empty() : OptionalDouble.of(rh);
    }

    private static DataComponentMap components(final Level level, final BlockPos pos) {
        return EnvironmentLookup.getInstance().findEnvironmentComponents(level, pos);
    }
}
