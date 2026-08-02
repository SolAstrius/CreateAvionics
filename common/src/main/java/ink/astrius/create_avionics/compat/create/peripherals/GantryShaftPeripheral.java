package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.create.peripherals.GantryRail.Carriage;
import ink.astrius.create_avionics.compat.create.peripherals.GantryRail.Rail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A gantry shaft — the rail along which a gantry carriage slides. The natural
 * place to wire kinetic input, and the right peripheral for the whole gantry:
 * the carriage block is consumed into a contraption entity the moment the
 * gantry moves, so a shaft is the only mount point that survives operation.
 *
 * <p>Driving programmatically goes via an upstream
 * {@code Create_SequencedGearshift}'s {@code move(distance)}, just like for
 * pistons. This peripheral deliberately exposes no setpoint — a gantry is
 * positioned by asking the drive for a distance and then observing the rail,
 * not by commanding a coordinate.</p>
 *
 * <p>Positions are rail indices measured from the {@code start} shaft:
 * whole numbers while parked, fractional while moving. That origin shifts if a
 * player extends the rail at the start end, so store waypoints in world space
 * via {@link #getRailStart} and convert.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *   <li>{@code gantry_departed(position)} — the carriage assembled and began moving</li>
 *   <li>{@code gantry_arrived(position)} — it stopped and became a block again</li>
 *   <li>{@code gantry_stalled(position)} — it is blocked and will not proceed</li>
 *   <li>{@code gantry_assembly_failed(error)} — an assembly attempt failed</li>
 * </ul>
 *
 * @cc.module Create_GantryShaft
 */
public class GantryShaftPeripheral extends KineticPeripheral<GantryShaftBlockEntity> implements ContraptionSurface {

    /** Rail state is polled for edges this often; a full rail scan is not free. */
    private static final int EVENT_POLL_TICKS = 5;

    private String lastState;
    private int tickCounter;

    public GantryShaftPeripheral(final GantryShaftBlockEntity blockEntity) {
        super(blockEntity);
    }

    @NotNull
    @Override
    public String getType() {
        return "Create_GantryShaft";
    }

    // --- Per-shaft state ---

    /**
     * Get this shaft's role in the rail.
     * One of {@code start}, {@code middle}, {@code end}, {@code single}.
     *
     * @return The part string.
     */
    @LuaFunction
    public final String getPart() {
        return this.blockEntity.getBlockState().getValue(GantryShaftBlock.PART).getSerializedName();
    }

    /**
     * Get the rail's axis.
     *
     * <p>Gantry contraptions never rotate — {@code applyRotation} is a hard
     * no-op — so this is also the coordinate axis {@link #getContraption}'s
     * {@code blocks}/{@code actors} positions live in, with no transform
     * needed to compare them. A block or actor whose position has this axis
     * nonzero and both other axes at 0 sits directly in line with the rail:
     * since a rail is a contiguous run of shaft blocks immediately beside the
     * carriage's whole path, anything protruding on this axis will collide
     * with a shaft block every time the carriage advances — visible as a
     * stall repeating roughly once per block, not a one-off jam. Off-axis
     * placement (as the carriage's own attachment point already is) is safe.
     *
     * @return The axis as {@code "x"}, {@code "y"}, or {@code "z"}.
     */
    @LuaFunction
    public final String getAxis() {
        return this.facing().getAxis().getSerializedName();
    }

    /**
     * Check whether this shaft is currently powered (redstone).
     *
     * <p>Powering does <em>not</em> reverse the carriage. A powered gantry
     * shaft stops translating its carriage and instead transmits rotation into
     * the carriage's output shaft. A powered rail will neither start a carriage
     * ({@link #canAssembleOn} is false) nor keep one moving — an in-flight
     * contraption disassembles where it stands.</p>
     *
     * <p>Power spreads along the whole rail: powering any shaft powers every
     * same-facing shaft on the axis.</p>
     *
     * @return True if powered.
     */
    @LuaFunction
    public final boolean isPowered() {
        return this.blockEntity.getBlockState().getValue(GantryShaftBlock.POWERED);
    }

    /**
     * Get the linear speed at which a carriage on this rail will move.
     *
     * <p>Signed: positive moves the carriage along the rail's facing direction,
     * negative moves it opposite. Derived from kinetic input alone — redstone
     * is not a factor, see {@link #isPowered}. 0 when the shaft is stalled or
     * has no kinetic source.</p>
     *
     * <p>Clamped upstream to ±0.49 blocks per tick, so a sufficiently fast
     * gantry saturates and this stops tracking {@code getSpeed()}.</p>
     *
     * @return The movement speed in blocks per tick.
     */
    @LuaFunction
    public final double getMovementSpeed() {
        return this.blockEntity.getPinionMovementSpeed();
    }

    /**
     * Check whether a carriage may currently assemble and move on this shaft.
     *
     * <p>Answers for <em>this</em> shaft, not the rail, and the two ends
     * disagree by design: a {@code start} shaft needs positive movement speed,
     * an {@code end} shaft negative, a {@code middle} shaft any non-zero, and a
     * {@code single} shaft can never assemble at all. Always false while the
     * rail is powered.</p>
     *
     * @return True if assembly conditions are met here.
     */
    @LuaFunction
    public final boolean canAssembleOn() {
        return this.blockEntity.canAssembleOn();
    }

    // --- Rail topology ---

    /**
     * Get the total length of the rail this shaft is part of (number of
     * contiguous same-facing shaft blocks along the rail axis).
     *
     * @return The rail length in blocks.
     */
    @LuaFunction(mainThread = true)
    public final int getRailLength() {
        return this.rail().length();
    }

    /**
     * Get this shaft's index along the rail, counted from the {@code start}
     * end. The {@code start} shaft is index 0; the {@code end} shaft is
     * {@code getRailLength() - 1}.
     *
     * @return The 0-based index.
     */
    @LuaFunction(mainThread = true)
    public final int getRailIndex() {
        return this.rail().index();
    }

    /**
     * Get the world position of the rail's {@code start} shaft — the origin
     * every rail index is measured from.
     *
     * <p>Exposed so positions can be anchored in world space: the origin moves
     * if a player extends the rail at the start end, which would silently shift
     * every index recorded before.</p>
     *
     * @return A table with {@code x}, {@code y} and {@code z}.
     */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getRailStart() {
        final BlockPos start = this.rail().start();
        final Map<String, Object> out = new HashMap<>(3);
        out.put("x", start.getX());
        out.put("y", start.getY());
        out.put("z", start.getZ());
        return out;
    }

    // --- Carriage ---

    /**
     * Get everything known about this rail's carriage in one read.
     *
     * <p>Preferred over the individual getters in a control loop: all fields
     * come from a single observation, so they cannot disagree the way separate
     * polls straddling a tick boundary can.</p>
     *
     * <p>Fields: {@code position} (rail index, fractional while moving),
     * {@code state} (see {@link #getState}), {@code stalled}, {@code moving},
     * and where applicable {@code id}, {@code error} and {@code remaining}.</p>
     *
     * @return The carriage table, or nil plus a reason.
     */
    @LuaFunction(mainThread = true)
    public final Object[] getCarriage() {
        final Carriage carriage = this.carriage();
        if (carriage == null) return new Object[]{null, "no carriage on this rail"};

        final Map<String, Object> out = new HashMap<>(7);
        out.put("position", carriage.position());
        out.put("state", carriage.state().serialized());
        out.put("moving", carriage.state() == GantryRail.State.MOVING);
        out.put("stalled", carriage.state() == GantryRail.State.STALLED);
        if (carriage.blockPos() != null) out.put("id", KineticReadback.idOf(carriage.blockPos()));
        if (carriage.error() != null) out.put("error", carriage.error());
        if (carriage.remaining() != null) out.put("remaining", carriage.remaining());
        return new Object[]{out};
    }

    /**
     * Get the rail's state.
     *
     * <p>One of {@code empty} (no carriage), {@code parked}, {@code moving},
     * {@code stalled} (assembled but blocked), or {@code failed} (present, but
     * its last assembly attempt errored — see {@link #getLastAssemblyError}).</p>
     *
     * @return The state string.
     */
    @LuaFunction(mainThread = true)
    public final String getState() {
        return this.state();
    }

    /**
     * Get the carriage's index along the rail, or nil if there is none.
     *
     * <p>Same units as {@link #getRailIndex} — 0 at the {@code start} end,
     * {@code getRailLength() - 1} at the {@code end}. Fractional while the
     * carriage is moving, so compare with a tolerance rather than for
     * equality.</p>
     *
     * @return The carriage's rail index, or nil.
     */
    @LuaFunction(mainThread = true)
    public final Double getCarriagePosition() {
        final Carriage carriage = this.carriage();
        return carriage == null ? null : carriage.position();
    }

    /**
     * Check whether a carriage is currently on this rail, parked or moving.
     *
     * @return True if a carriage is found.
     */
    @LuaFunction(mainThread = true)
    public final boolean hasCarriage() {
        return this.carriage() != null;
    }

    /**
     * Get the id of the carriage block attached to this rail, or nil.
     *
     * <p>Same opaque-token flavor as {@code getSelfId} on a peripheral wrapping
     * the carriage. Nil while the gantry is moving — there is no carriage block
     * then, only a contraption entity.</p>
     *
     * @return The carriage's id, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getCarriageId() {
        final Carriage carriage = this.carriage();
        if (carriage == null || carriage.blockPos() == null) return null;
        return KineticReadback.idOf(carriage.blockPos());
    }

    /**
     * Check whether this rail's carriage is currently an assembled contraption
     * — that is, moving or stalled rather than sitting as a block.
     *
     * @return True if assembled.
     */
    @LuaFunction(mainThread = true)
    public final boolean isAssembled() {
        return GantryRail.movingCarriage(this.level(), this.rail()) != null;
    }

    /**
     * Check whether this rail's carriage is stalled — assembled, but blocked by
     * the world or another contraption and making no progress.
     *
     * @return True if stalled.
     */
    @LuaFunction(mainThread = true)
    public final boolean isStalled() {
        final GantryContraptionEntity entity = GantryRail.movingCarriage(this.level(), this.rail());
        return entity != null && entity.isStalled();
    }

    /** The assembled carriage, or nil while parked — see {@link ContraptionSurface}. */
    @Override
    public GantryContraptionEntity contraptionEntity() {
        return GantryRail.movingCarriage(this.level(), this.rail());
    }

    /**
     * Get how far an in-flight sequenced movement still has to travel, in
     * blocks, or nil if the carriage is not moving under a sequenced
     * instruction.
     *
     * <p>This is the budget a {@code Create_SequencedGearshift}'s
     * {@code move(distance)} handed the carriage on assembly, decremented as it
     * travels.</p>
     *
     * @return The remaining distance in blocks, or nil.
     */
    @LuaFunction(mainThread = true)
    public final Double getRemainingMovement() {
        final Carriage carriage = this.carriage();
        return carriage == null ? null : carriage.remaining();
    }

    /**
     * Get the carriage's last assembly error message, or nil if its last
     * attempt succeeded, none has been made, or there is no carriage.
     *
     * <p>Same text the goggles show on a carriage whose last assembly
     * failed.</p>
     *
     * @return The error message, or nil.
     */
    @LuaFunction(mainThread = true)
    public final String getLastAssemblyError() {
        final Carriage carriage = this.carriage();
        return carriage == null ? null : carriage.error();
    }

    /**
     * Stop the carriage immediately, dropping its contraption back into the
     * world where it stands.
     *
     * <p>The emergency stop a Lua program otherwise cannot express — the only
     * alternative is cutting rotation upstream and waiting. This does not move
     * the carriage back; it disassembles in place, mid-rail if need be.</p>
     *
     * @return True, or nil plus a reason if there was nothing to stop.
     */
    @LuaFunction(mainThread = true)
    public final Object[] disassemble() {
        final GantryContraptionEntity entity = GantryRail.movingCarriage(this.level(), this.rail());
        if (entity == null) return new Object[]{null, "no assembled carriage on this rail"};
        entity.disassemble();
        return new Object[]{true};
    }

    // --- Events ---

    /**
     * Poll the rail for state changes and queue the corresponding events.
     * Called from the block entity's tick; see
     * {@code KineticBlockEntityComputerHookMixin}.
     */
    public void tickComputerEvents() {
        if (++this.tickCounter < EVENT_POLL_TICKS) return;
        this.tickCounter = 0;

        final Carriage carriage = this.carriage();
        final String state = carriage == null ? "empty" : carriage.state().serialized();
        final String previous = this.lastState;
        if (state.equals(previous)) return;
        this.lastState = state;

        // The first observation establishes a baseline; it is not an edge.
        if (previous == null) return;

        switch (state) {
            case "moving" -> queueEvent("gantry_departed", carriage.position());
            case "stalled" -> queueEvent("gantry_stalled", carriage.position());
            case "failed" -> queueEvent("gantry_assembly_failed", carriage.error());
            case "parked" -> {
                if (previous.equals("moving") || previous.equals("stalled")) {
                    queueEvent("gantry_arrived", carriage.position());
                }
            }
            default -> {
            }
        }
    }

    // --- Helpers ---

    private Rail rail() {
        return GantryRail.of(this.blockEntity);
    }

    private Carriage carriage() {
        return GantryRail.carriage(this.level(), this.rail());
    }

    private String state() {
        final Carriage carriage = this.carriage();
        return carriage == null ? "empty" : carriage.state().serialized();
    }

    private Level level() {
        return this.blockEntity.getLevel();
    }

    private Direction facing() {
        return this.blockEntity.getBlockState().getValue(GantryShaftBlock.FACING);
    }
}
