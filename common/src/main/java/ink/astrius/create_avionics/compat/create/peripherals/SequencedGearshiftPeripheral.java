/*
 * Portions of this file are derived from Create
 * (com.simibubi.create.compat.computercraft.implementation.peripherals.SequencedGearshiftPeripheral),
 * licensed under the MIT License.
 *
 * MIT License
 *
 * Copyright (c) The Create Team / The Creators of Create
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ink.astrius.create_avionics.compat.create.peripherals;

import com.simibubi.create.content.kinetics.transmission.sequencer.Instruction;
import com.simibubi.create.content.kinetics.transmission.sequencer.InstructionSpeedModifiers;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.api.create.SequencedGearshiftExt;
import ink.astrius.create_avionics.mixin.create.SequencedInstructionAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 * Drop-in replacement for Create's {@code Create_SequencedGearshift} peripheral.
 * <p>
 * <b>Preserved from Create (verbatim):</b> {@link #rotate}, {@link #move},
 * and {@link #isRunning}. Existing scripts that drive a gearshift keep
 * working unchanged.
 * <p>
 * <b>Added (Avionics) — queue programming:</b> {@link #getInstructions},
 * {@link #setInstructions}, {@link #start}, {@link #stop}. Mirrors the in-game
 * GUI's 5-row sequence editor.
 * <p>
 * <b>Added (Avionics) — instruction readback:</b> {@link #getRotationModifier},
 * {@link #getProgress}, {@link #getRemainingTicks},
 * {@link #getCurrentInstruction}. Pace autopilot moves against the active
 * instruction.
 * <p>
 * <b>Added (Avionics) — kinetic SCADA pack:</b> {@code getSelfId},
 * {@code getSourceId}, {@code getSubnetworkAnchorId}, {@code getNetworkId},
 * {@code getKind} (returns {@code "split_shaft"}), {@code getSpeed},
 * {@code hasSource}, {@code isOverstressed}, {@code getStressImpact},
 * {@code getStressContribution}. Inherited from {@code KineticScadaSurface}
 * via {@code KineticPeripheral}; not declared here. The gearshift is a
 * speed-zone boundary, so its {@code getSelfId} is the anchor id every block
 * downstream reports for {@code getSubnetworkAnchorId}.
 * <p>
 * Type stays {@code Create_SequencedGearshift} — atypical for Avionics (which
 * uses snake_case) but required for script compatibility.
 *
 * @cc.module Create_SequencedGearshift
 */
public class SequencedGearshiftPeripheral extends com.simibubi.create.compat.computercraft.implementation.peripherals.SequencedGearshiftPeripheral {

    private static final int MAX_INSTRUCTIONS = 5;

    public SequencedGearshiftPeripheral(final SequencedGearshiftBlockEntity blockEntity) {
        super(blockEntity);
    }

    // --- Avionics: queue programming ---

    /**
     * Get the full instruction queue.
     * <p>
     * Returns up to 5 entries (the in-game GUI's
     * capacity). The last entry is always {@code end}. Each entry is a table:
     * <ul>
     *   <li>{@code type}: one of {@code turn_angle}, {@code turn_distance},
     *       {@code delay}, {@code await}, {@code end}</li>
     *   <li>{@code value}: configured target — degrees / blocks / ticks
     *       depending on type. 1 for types without a value parameter.</li>
     *   <li>{@code speed_modifier}: -2..+2 for rotational instructions,
     *       FORWARD (1) otherwise.</li>
     * </ul>
     *
     * @return The instruction list.
     */
    @LuaFunction
    public final List<Map<String, Object>> getInstructions() {
        final Vector<Instruction> v = this.blockEntity.getInstructions();
        final List<Map<String, Object>> out = new ArrayList<>(v.size());
        for (final Instruction i : v) {
            out.add(toLuaTable(i));
        }
        return out;
    }

    /**
     * Replace the instruction queue.
     * <p>
     * Accepts a 1-indexed Lua list of up to 5 entries.
     * Each entry is a table with:
     * <ul>
     *   <li>{@code type} (required): {@code turn_angle}, {@code turn_distance},
     *       {@code delay}, {@code await}, or {@code end}</li>
     *   <li>{@code value} (required for {@code turn_angle} / {@code turn_distance}
     *       / {@code delay}): clamped to the type's range
     *       ({@code turn_angle}: 1..360°, {@code turn_distance}: 1..128 blocks,
     *       {@code delay}: 1..600 ticks)</li>
     *   <li>{@code speed_modifier} (optional, rotational only): -2..+2,
     *       defaults to FORWARD (1)</li>
     * </ul>
     * <p>
     * Halts any running sequence ({@code run(-1)}) before applying. Auto-appends
     * an {@code end} terminator if the last entry isn't already {@code end}.
     * After this call, use {@link #start} to begin executing the new queue.
     *
     * @param instructions A list of instruction tables.
     */
    @LuaFunction(mainThread = true)
    public final void setInstructions(final Map<?, ?> instructions) throws LuaException {
        final List<Instruction> parsed = new ArrayList<>(MAX_INSTRUCTIONS);
        for (int i = 1; i <= MAX_INSTRUCTIONS + 1; i++) {
            final Object entry = instructions.get((double) i);
            if (entry == null) break;
            if (!(entry instanceof final Map<?, ?> m)) {
                throw new LuaException("instructions[" + i + "] must be a table");
            }
            parsed.add(parseInstruction(m, i));
        }
        if (parsed.isEmpty()) throw new LuaException("must provide at least 1 instruction");
        if (parsed.size() > MAX_INSTRUCTIONS) {
            throw new LuaException("at most " + MAX_INSTRUCTIONS + " instructions allowed");
        }

        // Ensure trailing END
        final Instruction last = parsed.get(parsed.size() - 1);
        final SequencerInstructions lastType = ((SequencedInstructionAccessor) (Object) last).createAvionics$instruction();
        if (lastType != SequencerInstructions.END) {
            if (parsed.size() == MAX_INSTRUCTIONS) {
                throw new LuaException("queue is full and last instruction is not 'end'");
            }
            parsed.add(new Instruction(SequencerInstructions.END));
        }

        this.blockEntity.run(-1);
        final Vector<Instruction> v = this.blockEntity.getInstructions();
        v.clear();
        v.addAll(parsed);
        this.blockEntity.sendData();
    }

    /**
     * Start executing the instruction queue from index 0.
     * Equivalent to a redstone rising edge on a non-computer-attached gearshift.
     * No-op if the gearshift has zero kinetic input.
     */
    @LuaFunction(mainThread = true)
    public final void start() {
        this.blockEntity.run(0);
    }

    /**
     * Halt any running sequence and return to idle.
     */
    @LuaFunction(mainThread = true)
    public final void stop() {
        this.blockEntity.run(-1);
    }

    // --- Avionics: SCADA readback ---

    private SequencedGearshiftExt ext() {
        return (SequencedGearshiftExt) this.blockEntity;
    }

    /**
     * Get the current rotation modifier (output:input ratio).
     * 0 when idle. Sign and magnitude track the active instruction's speed
     * modifier ({@code +2 = FORWARD_FAST}, {@code -1 = BACK}, etc.).
     *
     * @return The rotation modifier.
     */
    @LuaFunction
    public final int getRotationModifier() {
        return this.blockEntity.getModifier();
    }

    /**
     * Get the active instruction's progress as a fraction in [0, 1].
     * Computed as {@code progress / value} in the instruction's native unit
     * (degrees for {@code turn_angle}, blocks for {@code turn_distance}, ticks
     * for {@code delay}). Returns 0 when idle. Returns 0 for {@code await}
     * (which has no fixed duration). May briefly read &gt; 1 because the
     * sequencer overshoots by ~2 ticks before advancing.
     *
     * @return The progress fraction.
     */
    @LuaFunction
    public final double getProgress() {
        if (!this.ext().createAvionics$hasActiveInstruction()) return 0.0;
        final int target = this.ext().createAvionics$getCurrentInstructionValue();
        if (target <= 0) return 0.0;
        return this.ext().createAvionics$getInstructionProgress() / (double) target;
    }

    /**
     * Get the number of ticks remaining on the active instruction.
     * Returns 0 when idle. Returns -1 for {@code await} (waits indefinitely
     * for a redstone pulse). Note that the underlying duration is recomputed
     * whenever the kinetic speed changes mid-instruction, so this value can
     * jump if the input shaft speeds up or slows down.
     *
     * @return The remaining ticks.
     */
    @LuaFunction
    public final int getRemainingTicks() {
        if (!this.ext().createAvionics$hasActiveInstruction()) return 0;
        final int duration = this.ext().createAvionics$getInstructionDuration();
        if (duration < 0) return -1;
        return Math.max(0, duration - this.ext().createAvionics$getInstructionTimer());
    }

    /**
     * Get the active instruction as a table, or nil when idle.
     * Same shape as one entry in {@link #getInstructions}: {@code type},
     * {@code value}, {@code speed_modifier}.
     *
     * @return The current instruction table, or nil.
     */
    @LuaFunction
    public final Map<String, Object> getCurrentInstruction() {
        if (!this.ext().createAvionics$hasActiveInstruction()) return null;
        final Map<String, Object> out = new HashMap<>();
        out.put("type", this.ext().createAvionics$getCurrentInstructionType());
        out.put("value", this.ext().createAvionics$getCurrentInstructionValue());
        out.put("speed_modifier", this.ext().createAvionics$getCurrentInstructionSpeedModifier());
        return out;
    }

    // --- Helpers ---

    private static Map<String, Object> toLuaTable(final Instruction i) {
        final SequencedInstructionAccessor acc = (SequencedInstructionAccessor) (Object) i;
        final Map<String, Object> out = new HashMap<>();
        out.put("type", acc.createAvionics$instruction().name().toLowerCase(Locale.ROOT));
        out.put("value", acc.createAvionics$value());
        out.put("speed_modifier", modifierToInt(acc.createAvionics$speedModifier()));
        return out;
    }

    private static int modifierToInt(final InstructionSpeedModifiers m) {
        return switch (m) {
            case FORWARD_FAST -> 2;
            case FORWARD -> 1;
            case BACK -> -1;
            case BACK_FAST -> -2;
        };
    }

    private static Instruction parseInstruction(final Map<?, ?> m, final int idx) throws LuaException {
        final Object typeObj = m.get("type");
        if (!(typeObj instanceof final String typeStr)) {
            throw new LuaException("instructions[" + idx + "].type must be a string");
        }
        final SequencerInstructions type;
        try {
            type = SequencerInstructions.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            throw new LuaException("instructions[" + idx + "].type: unknown type '" + typeStr + "'");
        }

        int value = type.hasValueParameter ? type.defaultValue : 1;
        if (type.hasValueParameter) {
            final Object valueObj = m.get("value");
            if (valueObj == null) {
                throw new LuaException("instructions[" + idx + "].value is required for type '" + typeStr + "'");
            }
            if (!(valueObj instanceof final Number n)) {
                throw new LuaException("instructions[" + idx + "].value must be a number");
            }
            value = n.intValue();
            if (value < 1 || value > type.maxValue) {
                throw new LuaException("instructions[" + idx + "].value must be in [1, " + type.maxValue + "] for '" + typeStr + "'");
            }
        }

        InstructionSpeedModifiers mod = InstructionSpeedModifiers.FORWARD;
        if (type.hasSpeedParameter) {
            final Object modObj = m.get("speed_modifier");
            if (modObj != null) {
                if (!(modObj instanceof final Number n)) {
                    throw new LuaException("instructions[" + idx + "].speed_modifier must be a number");
                }
                final int mi = n.intValue();
                if (mi != -2 && mi != -1 && mi != 1 && mi != 2) {
                    throw new LuaException("instructions[" + idx + "].speed_modifier must be -2, -1, 1, or 2");
                }
                mod = InstructionSpeedModifiers.getByModifier(mi);
            }
        }

        return new Instruction(type, mod, value);
    }
}
