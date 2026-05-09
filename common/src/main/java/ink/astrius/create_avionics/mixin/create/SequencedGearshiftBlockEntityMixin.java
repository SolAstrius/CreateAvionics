package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.content.kinetics.transmission.sequencer.Instruction;
import com.simibubi.create.content.kinetics.transmission.sequencer.InstructionSpeedModifiers;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import ink.astrius.create_avionics.api.create.SequencedGearshiftExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Locale;
import java.util.Vector;

@Mixin(value = SequencedGearshiftBlockEntity.class, remap = false)
public abstract class SequencedGearshiftBlockEntityMixin implements SequencedGearshiftExt {

    @Accessor("instructions")
    abstract Vector<Instruction> createAvionics$instructions();

    @Accessor("currentInstruction")
    abstract int createAvionics$currentInstruction();

    @Accessor("currentInstructionDuration")
    abstract int createAvionics$currentInstructionDuration();

    @Accessor("currentInstructionProgress")
    abstract float createAvionics$currentInstructionProgress();

    @Accessor("timer")
    abstract int createAvionics$timer();

    @Override
    public boolean createAvionics$hasActiveInstruction() {
        return this.createAvionics$currentInstruction() != -1;
    }

    @Override
    public String createAvionics$getCurrentInstructionType() {
        final Instruction i = this.activeInstruction();
        return i == null ? null : ((SequencedInstructionAccessor) (Object) i).createAvionics$instruction().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public int createAvionics$getCurrentInstructionValue() {
        final Instruction i = this.activeInstruction();
        return i == null ? 0 : ((SequencedInstructionAccessor) (Object) i).createAvionics$value();
    }

    @Override
    public int createAvionics$getCurrentInstructionSpeedModifier() {
        final Instruction i = this.activeInstruction();
        if (i == null) return 0;
        final InstructionSpeedModifiers m = ((SequencedInstructionAccessor) (Object) i).createAvionics$speedModifier();
        return switch (m) {
            case FORWARD_FAST -> 2;
            case FORWARD -> 1;
            case BACK -> -1;
            case BACK_FAST -> -2;
        };
    }

    @Override
    public float createAvionics$getInstructionProgress() {
        return this.createAvionics$hasActiveInstruction() ? this.createAvionics$currentInstructionProgress() : 0f;
    }

    @Override
    public int createAvionics$getInstructionDuration() {
        return this.createAvionics$hasActiveInstruction() ? this.createAvionics$currentInstructionDuration() : 0;
    }

    @Override
    public int createAvionics$getInstructionTimer() {
        return this.createAvionics$hasActiveInstruction() ? this.createAvionics$timer() : 0;
    }

    @Override
    public int createAvionics$getInstructionCount() {
        final Vector<Instruction> v = this.createAvionics$instructions();
        return v == null ? 0 : v.size();
    }

    private Instruction activeInstruction() {
        if (!this.createAvionics$hasActiveInstruction()) return null;
        final Vector<Instruction> v = this.createAvionics$instructions();
        final int idx = this.createAvionics$currentInstruction();
        return idx >= 0 && idx < v.size() ? v.get(idx) : null;
    }
}
