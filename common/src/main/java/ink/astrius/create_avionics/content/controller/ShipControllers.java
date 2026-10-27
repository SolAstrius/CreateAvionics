package ink.astrius.create_avionics.content.controller;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The controllers on each ship, server side. Every controller works; the one at the lowest
 * position publishes the ship's peripherals and the others stand by, so when it is destroyed
 * the next takes over with the same peripheral names.
 */
public final class ShipControllers {

    private static final Map<UUID, Set<AvionicsControllerBlockEntity>> SHIPS = new HashMap<>();

    private ShipControllers() {
    }

    public static synchronized void join(final UUID ship, final AvionicsControllerBlockEntity controller) {
        SHIPS.computeIfAbsent(ship, s -> new LinkedHashSet<>()).add(controller);
    }

    public static synchronized void leave(final UUID ship, final AvionicsControllerBlockEntity controller) {
        final Set<AvionicsControllerBlockEntity> set = SHIPS.get(ship);
        if (set != null) {
            set.remove(controller);
            if (set.isEmpty()) {
                SHIPS.remove(ship);
            }
        }
    }

    /**
     * What a ship's transponder says, or empty if no controller on it is transmitting: the ship's
     * name, or a short code from its id when it has none.
     */
    public static synchronized Optional<String> transponder(final UUID ship, final @Nullable String name) {
        for (final AvionicsControllerBlockEntity c : SHIPS.getOrDefault(ship, Set.of())) {
            if (!c.isRemoved() && c.getBlockState().getValue(AvionicsControllerBlock.TRANSPONDER)) {
                return Optional.of(name != null && !name.isBlank()
                        ? name : ship.toString().substring(0, 6).toUpperCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }

    /** Live controllers on the ship, the publishing one first. */
    public static synchronized List<AvionicsControllerBlockEntity> on(final UUID ship) {
        final List<AvionicsControllerBlockEntity> list = new ArrayList<>();
        for (final AvionicsControllerBlockEntity c : SHIPS.getOrDefault(ship, Set.of())) {
            if (!c.isRemoved()) {
                list.add(c);
            }
        }
        list.sort(Comparator.comparingLong(c -> c.getBlockPos().asLong()));
        return list;
    }
}
