package ink.astrius.create_avionics.compat.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A peripheral that composes a primary {@link IPeripheral} with extra methods
 * harvested from CC's {@link GenericPeripheralProvider}. The generic pass
 * covers both {@code GenericSource} implementations whose first-parameter type
 * matches the BE (e.g. {@code KineticSource}) and methods reached via
 * registered {@code ComponentLookup}s (e.g. {@code InventoryMethods} on the
 * BE's {@code IItemHandler} capability).
 *
 * <p>Primary methods win on name collision; {@link #getType()} and
 * {@link #getTarget()} delegate to the primary; {@link #getAdditionalTypes()}
 * is the union.</p>
 *
 * <p>Created on-demand by {@code PeripheralAccessMixin} when the BE is
 * registered via {@link PeripheralComposition}. Returns {@code null} from
 * {@link #build} when the generic pass contributes no new methods, so callers
 * can fall back to the primary unchanged.</p>
 */
public final class ComposingPeripheral implements IDynamicPeripheral {

    private final IPeripheral primary;
    private final List<Entry> entries;
    private final String[] methodNames;
    private final Set<String> additionalTypes;
    private final Object target;

    private ComposingPeripheral(final IPeripheral primary, final List<Entry> entries, final Set<String> additionalTypes, final Object target) {
        this.primary = primary;
        this.entries = entries;
        this.methodNames = entries.stream().map(Entry::name).toArray(String[]::new);
        this.additionalTypes = additionalTypes;
        this.target = target;
    }

    public static @Nullable ComposingPeripheral build(
        final IPeripheral primary,
        final BlockEntity blockEntity,
        final ServerLevel level,
        final BlockPos pos,
        final Direction side,
        final MethodSupplier<PeripheralMethod> supplier,
        final GenericPeripheralProvider provider
    ) {
        final List<Entry> entries = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        final Set<String> additionalTypes = new HashSet<>(primary.getAdditionalTypes());

        // Primary first: those methods win on collision.
        supplier.forEachMethod(primary, (obj, name, method, info) -> {
            if (seen.add(name)) entries.add(new Entry(obj, name, method));
        });
        final int primaryCount = entries.size();

        // Generic pass: GenericSources matching the BE plus all registered
        // ComponentLookups (capability-backed targets like IItemHandler).
        provider.forEachMethod(supplier, level, pos, side, blockEntity, (obj, name, method, info) -> {
            if (seen.add(name)) entries.add(new Entry(obj, name, method));
        });

        if (entries.size() == primaryCount) return null;

        return new ComposingPeripheral(primary, entries, additionalTypes, blockEntity);
    }

    @Override
    public String[] getMethodNames() {
        return this.methodNames;
    }

    @Override
    public MethodResult callMethod(final IComputerAccess computer, final ILuaContext context, final int method, final IArguments arguments) throws LuaException {
        final Entry e = this.entries.get(method);
        return e.method.apply(e.target, context, computer, arguments);
    }

    @Override
    public String getType() {
        return this.primary.getType();
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return this.additionalTypes;
    }

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public boolean equals(final @Nullable IPeripheral other) {
        if (other == this) return true;
        if (!(other instanceof final ComposingPeripheral c)) return false;
        return this.target.equals(c.target)
            && this.primary.equals(c.primary)
            && Arrays.equals(this.methodNames, c.methodNames);
    }

    private record Entry(Object target, String name, PeripheralMethod method) {
    }
}
