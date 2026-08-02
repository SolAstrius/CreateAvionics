package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.Map;

/**
 * Shared contraption surface for peripherals that drive one — gantry shaft,
 * mechanical piston, mechanical bearing, rope pulley, elevator pulley.
 * Brings a read-only inventory/fluid inspection contract and a descriptive
 * table to all of them without duplicating the {@code @LuaFunction}
 * declarations. Implementors only supply the currently assembled entity, or
 * nil when the block isn't running one right now, via
 * {@link #contraptionEntity()}.
 *
 * <p>Deliberately read-only, unlike a normal CC inventory/fluid peripheral:
 * vanilla Create only ever exposes a moving contraption's storage for actual
 * transfer through a docked Portable Storage/Fluid Interface (see
 * {@code PortableStorageInterfaceMovement}) — a stationary interface block,
 * aligned and connected to a matching one riding the contraption. That
 * stationary block is a normal world block with a real item/fluid handler
 * capability, so once docked it's already a plain CC inventory/fluid_storage
 * peripheral on its own; this surface has no business reimplementing
 * {@code pushItems}/{@code pullItems}/{@code pushFluid}/{@code pullFluid} —
 * doing so would let scripts reach into any moving contraption's storage from
 * anywhere, without the alignment or redstone gating vanilla requires.</p>
 *
 * <p>{@link #size}, {@link #list} and {@link #tanks} report empty/zero when
 * unassembled rather than erroring — a script polling in a loop shouldn't
 * need to special-case "not moving right now". Calls that imply a specific
 * slot throw instead, since there's nothing there to inspect.</p>
 */
public interface ContraptionSurface {

    AbstractContraptionEntity contraptionEntity();

    /**
     * Get everything known about the assembled contraption in one read:
     * whether it's {@code stalled}, its current {@code velocity} (world-frame
     * blocks/tick — a rate of change, not a location, so it's exempt from the
     * local-only rule below), block count, seat count,
     * {@code hasBlockBreakers} (can it destroy blocks in its path, e.g. a saw
     * in cutting mode — relevant context for a stall: is it expected to grind
     * through an obstacle or just sit blocked by one), local bounding box,
     * the local position (an offset from the contraption's own anchor, not a
     * world coordinate) and block id of every block making it up, its active
     * mechanical implements ({@code actors} — drills, saws, harvesters,
     * deployers, mechanical arms, mixers, each with
     * {@code disabled}/{@code stalled}), every seat with whoever's riding it
     * ({@code seats}), every entity being carried along without a seat —
     * standing on an elevator cabin, walking a moving platform
     * ({@code riders}) — every block supporting right-click interaction while
     * riding ({@code interactors}), the actor types currently switched off
     * via the Contraption Controls filter ({@code disabledActorTypes}), and
     * mounted-storage counts. Both {@code seats} and {@code riders} are
     * anonymous by design: an occupant is only {@code isPlayer} and its
     * entity type, never a name or UUID.
     *
     * <p>Anything mounted so it protrudes in the contraption's own direction
     * of travel — a drill facing fore or aft on a gantry carriage, say — will
     * collide with whatever it's moving past on every block of motion, not
     * just once. That reads as a stall repeating at a regular position
     * interval rather than a single jam. On a gantry shaft, {@link
     * GantryShaftPeripheral#getAxis} names that travel axis and shares this
     * method's local coordinate frame exactly, so a block/actor position with
     * only that axis nonzero is the one to suspect.</p>
     *
     * @return The contraption table, or nil plus a reason.
     */
    @LuaFunction(mainThread = true)
    default Object[] getContraption() {
        return ContraptionReadback.describe(contraptionEntity());
    }

    /**
     * Get the number of slots in the contraption's combined inventory — every
     * mounted chest, barrel, depot, etc. summed together. 0 when unassembled.
     *
     * @return The slot count.
     */
    @LuaFunction(mainThread = true)
    default int size() {
        return ContraptionReadback.size(contraptionEntity());
    }

    /**
     * List every non-empty slot in the contraption's combined inventory.
     * Empty when unassembled.
     *
     * @return A map of slot to item detail.
     */
    @LuaFunction(mainThread = true)
    default Map<Integer, Map<String, ?>> list() {
        return ContraptionReadback.list(contraptionEntity());
    }

    /**
     * Get the item in a specific slot of the contraption's combined inventory.
     *
     * @param slot The slot to inspect.
     * @return The item detail, or nil if empty.
     */
    @LuaFunction(mainThread = true)
    default Map<String, ?> getItemDetail(final int slot) throws LuaException {
        return ContraptionReadback.getItemDetail(contraptionEntity(), slot);
    }

    /**
     * Get the maximum number of items a slot in the contraption's combined
     * inventory can hold.
     *
     * @param slot The slot to inspect.
     * @return The item limit.
     */
    @LuaFunction(mainThread = true)
    default long getItemLimit(final int slot) throws LuaException {
        return ContraptionReadback.getItemLimit(contraptionEntity(), slot);
    }

    /**
     * List every tank in the contraption's combined fluid storage. Empty when
     * unassembled.
     *
     * @return A map of tank index to fluid detail.
     */
    @LuaFunction(mainThread = true)
    default Map<Integer, Map<String, ?>> tanks() {
        return ContraptionReadback.tanks(contraptionEntity());
    }
}
