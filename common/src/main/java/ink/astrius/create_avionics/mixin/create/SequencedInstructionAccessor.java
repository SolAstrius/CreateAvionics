package ink.astrius.create_avionics.mixin.create;

import com.simibubi.create.content.kinetics.transmission.sequencer.Instruction;
import com.simibubi.create.content.kinetics.transmission.sequencer.InstructionSpeedModifiers;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Instruction.class, remap = false)
public interface SequencedInstructionAccessor {

    @Accessor("instruction")
    SequencerInstructions createAvionics$instruction();

    @Accessor("speedModifier")
    InstructionSpeedModifiers createAvionics$speedModifier();

    @Accessor("value")
    int createAvionics$value();
}
