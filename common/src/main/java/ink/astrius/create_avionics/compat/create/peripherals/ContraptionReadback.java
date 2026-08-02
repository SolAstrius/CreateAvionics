package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything Lua can learn about an assembled contraption, shared by every
 * peripheral that drives one (gantry shaft, piston, bearing, pulley,
 * elevator). Read-only: see {@link ContraptionSurface} for why transfer isn't
 * exposed here.
 *
 * <p>Item and fluid listing delegate to CC:Tweaked's own generic
 * {@code InventoryMethods} / {@code FluidMethods} rather than reimplementing
 * slot semantics: a contraption's combined storage
 * ({@code MountedStorageManager#getAllItems}/{@code #getFluids}) is a plain
 * {@code IItemHandler}/{@code IFluidHandler}, so {@link #list}/{@link #tanks}
 * report the same shape a script would get from an ordinary CC inventory
 * peripheral.</p>
 */
public final class ContraptionReadback {

    private static final InventoryMethods ITEMS = new InventoryMethods();
    private static final FluidMethods FLUIDS = new FluidMethods();

    private ContraptionReadback() {
    }

    /** Everything known about the assembled contraption, or nil plus a reason. */
    public static Object[] describe(final AbstractContraptionEntity entity) {
        if (entity == null) return new Object[]{null, "not assembled"};
        final Contraption c = entity.getContraption();

        final Map<String, Object> out = new HashMap<>();
        out.put("stalled", entity.isStalled());
        out.put("velocity", vecOf(entity.getDeltaMovement()));
        out.put("blockCount", c.getBlocks().size());
        out.put("seatCount", c.getSeats().size());
        out.put("hasBlockBreakers", c.containsBlockBreakers());
        out.put("bounds", boundsOf(c.bounds));
        out.put("seats", seatsOf(entity, c));
        out.put("riders", ridersOf(entity));
        out.put("blocks", blocksOf(c));
        out.put("actors", actorsOf(c));
        out.put("interactors", interactorsOf(c));
        out.put("disabledActorTypes", disabledActorTypesOf(c));
        out.put("itemStorageCount", c.getStorage().getAllItemStorages().size());
        out.put("fluidStorageCount", c.getStorage().getFluids().storages.size());
        return new Object[]{out};
    }

    /** A {@link Vec3} as a Lua table — used for {@code velocity}, in world-frame blocks/tick. */
    private static Map<String, Object> vecOf(final Vec3 v) {
        final Map<String, Object> out = new HashMap<>(3);
        out.put("x", v.x);
        out.put("y", v.y);
        out.put("z", v.z);
        return out;
    }

    private static Map<String, Object> boundsOf(final AABB bounds) {
        final Map<String, Object> out = new HashMap<>(6);
        out.put("minX", bounds.minX);
        out.put("minY", bounds.minY);
        out.put("minZ", bounds.minZ);
        out.put("maxX", bounds.maxX);
        out.put("maxY", bounds.maxY);
        out.put("maxZ", bounds.maxZ);
        return out;
    }

    /**
     * Position and block id of every block making up the contraption.
     *
     * <p>{@code pos} is local — an offset from the contraption's own anchor,
     * not a world coordinate. It's the same frame {@link Contraption#getBlocks}
     * itself uses, stable across a move; a world position would have to be
     * recomputed (rotation and all) on every read and go stale the instant
     * the contraption ticks again.</p>
     */
    private static List<Map<String, Object>> blocksOf(final Contraption c) {
        final List<Map<String, Object>> out = new ArrayList<>(c.getBlocks().size());
        for (final Map.Entry<BlockPos, StructureBlockInfo> e : c.getBlocks().entrySet()) {
            final StructureBlockInfo info = e.getValue();

            final Map<String, Object> block = new HashMap<>(3);
            block.put("id", BuiltInRegistries.BLOCK.getKey(info.state().getBlock()).toString());
            block.put("pos", posOf(e.getKey()));
            block.put("hasBlockEntity", info.nbt() != null);
            out.add(block);
        }
        return out;
    }

    /**
     * The contraption's active mechanical implements — blocks with a
     * {@code MovementBehaviour} (drills, saws, harvesters, deployers,
     * mechanical arms, mixers...), as opposed to inert structural blocks.
     *
     * <p>No speed or stress figure is included: a block's kinetic network
     * connection is severed the instant it's captured into a contraption
     * ({@code KineticBlockEntity#remove} calls {@code detachKinetics()}), so
     * there is no live stress/capacity to report — Create itself doesn't
     * track one past that point. {@code disabled}/{@code stalled} are the
     * actual live state Create keeps per actor via its
     * {@code MovementContext}.</p>
     */
    private static List<Map<String, Object>> actorsOf(final Contraption c) {
        final List<Map<String, Object>> out = new ArrayList<>(c.getActors().size());
        for (final MutablePair<StructureBlockInfo, MovementContext> actor : c.getActors()) {
            final StructureBlockInfo info = actor.getLeft();
            final MovementContext ctx = actor.getRight();

            final Map<String, Object> a = new HashMap<>(4);
            a.put("id", BuiltInRegistries.BLOCK.getKey(info.state().getBlock()).toString());
            a.put("pos", posOf(info.pos()));
            a.put("disabled", ctx.disabled);
            a.put("stalled", ctx.stall);
            out.add(a);
        }
        return out;
    }

    /**
     * Local position of every block on the contraption that supports
     * right-click interaction while riding — a Contraption Controls panel,
     * for instance — distinct from {@link #seatsOf a seat}. No further detail
     * about what kind of interaction: {@code MovingInteractionBehaviour} is a
     * behavior object, not data, so there's nothing more structured to report
     * than "something interactable is here."
     */
    private static List<Map<String, Object>> interactorsOf(final Contraption c) {
        final List<Map<String, Object>> out = new ArrayList<>(c.getInteractors().size());
        for (final BlockPos pos : c.getInteractors().keySet()) {
            out.add(posOf(pos));
        }
        return out;
    }

    /**
     * Actor types currently switched off via the contraption's Contraption
     * Controls filter ({@code Contraption#isActorTypeDisabled}), as item
     * registry ids (e.g. {@code "create:mechanical_drill"}) rather than the
     * raw {@code ItemStack} filter entries Create itself uses.
     */
    private static List<String> disabledActorTypesOf(final Contraption c) {
        final List<String> out = new ArrayList<>();
        for (final ItemStack stack : c.getDisabledActors()) {
            out.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return out;
    }

    /**
     * Every seat on the contraption, with whoever's riding it. Anonymous by
     * design: {@code occupant} is only {@code isPlayer} (a player, not which
     * one) and {@code type} (the entity type registry id, e.g.
     * {@code "minecraft:cow"}) — no name, no UUID. Absent entirely when the
     * seat is empty.
     */
    private static List<Map<String, Object>> seatsOf(final AbstractContraptionEntity entity, final Contraption c) {
        final Map<UUID, Integer> seatMapping = c.getSeatMapping();
        final Map<Integer, Entity> occupantBySeat = new HashMap<>();
        for (final Entity passenger : entity.getPassengers()) {
            final Integer seatIndex = seatMapping.get(passenger.getUUID());
            if (seatIndex != null) occupantBySeat.put(seatIndex, passenger);
        }

        final List<BlockPos> seats = c.getSeats();
        final List<Map<String, Object>> out = new ArrayList<>(seats.size());
        for (int i = 0; i < seats.size(); i++) {
            final Map<String, Object> seat = new HashMap<>(2);
            seat.put("pos", posOf(seats.get(i)));

            final Entity occupant = occupantBySeat.get(i);
            if (occupant != null) {
                final Map<String, Object> o = new HashMap<>(2);
                o.put("isPlayer", occupant instanceof Player);
                o.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(occupant.getType()).toString());
                seat.put("occupant", o);
            }
            out.add(seat);
        }
        return out;
    }

    /**
     * Entities currently being carried by physical contact — standing on an
     * elevator cabin, walking along a moving gantry platform — as opposed to
     * riding a designated {@link #seatsOf seat}. Distinct and
     * non-overlapping: a seated passenger is mounted via vanilla's normal
     * rider system ({@code AbstractContraptionEntity#getPassengers}); a
     * rider here never mounted anything, Create is just pushing them along
     * every tick via collision and tracking them in the entity's own
     * {@code collidingEntities} map to do it. Same anonymized shape as a
     * seat occupant ({@code isPlayer} + entity type, no name or UUID), plus
     * a local position via {@code ContraptionCollider#worldToLocalPos}.
     */
    private static List<Map<String, Object>> ridersOf(final AbstractContraptionEntity entity) {
        final List<Map<String, Object>> out = new ArrayList<>(entity.collidingEntities.size());
        for (final Entity rider : entity.collidingEntities.keySet()) {
            final BlockPos localPos = BlockPos.containing(
                    ContraptionCollider.worldToLocalPos(rider.position(), entity));

            final Map<String, Object> r = new HashMap<>(3);
            r.put("isPlayer", rider instanceof Player);
            r.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(rider.getType()).toString());
            r.put("pos", posOf(localPos));
            out.add(r);
        }
        return out;
    }

    /** A local {@link BlockPos} as a Lua table — see {@link #blocksOf}. */
    private static Map<String, Object> posOf(final BlockPos pos) {
        final Map<String, Object> out = new HashMap<>(3);
        out.put("x", pos.getX());
        out.put("y", pos.getY());
        out.put("z", pos.getZ());
        return out;
    }

    // --- Items ---

    public static int size(final AbstractContraptionEntity entity) {
        return entity == null ? 0 : ITEMS.size(items(entity));
    }

    public static Map<Integer, Map<String, ?>> list(final AbstractContraptionEntity entity) {
        return entity == null ? Map.of() : ITEMS.list(items(entity));
    }

    public static Map<String, ?> getItemDetail(final AbstractContraptionEntity entity, final int slot) throws LuaException {
        requireAssembled(entity);
        return ITEMS.getItemDetail(items(entity), slot);
    }

    public static long getItemLimit(final AbstractContraptionEntity entity, final int slot) throws LuaException {
        requireAssembled(entity);
        return ITEMS.getItemLimit(items(entity), slot);
    }

    // --- Fluids ---

    public static Map<Integer, Map<String, ?>> tanks(final AbstractContraptionEntity entity) {
        return entity == null ? Map.of() : FLUIDS.tanks(fluids(entity));
    }

    private static void requireAssembled(final AbstractContraptionEntity entity) throws LuaException {
        if (entity == null) throw new LuaException("not assembled");
    }

    private static IItemHandler items(final AbstractContraptionEntity entity) {
        return entity.getContraption().getStorage().getAllItems();
    }

    private static IFluidHandler fluids(final AbstractContraptionEntity entity) {
        return entity.getContraption().getStorage().getFluids();
    }
}
