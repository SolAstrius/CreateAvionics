package ink.astrius.create_avionics.api.create;

/**
 * Read-side accessors over the package-private state of
 * {@code SequencedGearshiftBlockEntity}. Mixed into the upstream BE so our
 * peripheral can surface progress, remaining time, and the active instruction
 * without reflection or re-deriving values that already live on the BE.
 */
public interface SequencedGearshiftExt {

    /** True if an instruction is currently running. */
    boolean createAvionics$hasActiveInstruction();

    /**
     * Lowercase enum name of the active instruction, or {@code null} when idle.
     * One of: {@code turn_angle}, {@code turn_distance}, {@code delay},
     * {@code await}, {@code end}.
     */
    String createAvionics$getCurrentInstructionType();

    /** Target value of the active instruction (degrees / blocks / ticks); 0 when idle. */
    int createAvionics$getCurrentInstructionValue();

    /** Speed modifier (-2 BACK_FAST..+2 FORWARD_FAST) of the active instruction; 0 when idle or non-rotational. */
    int createAvionics$getCurrentInstructionSpeedModifier();

    /**
     * Accumulated progress within the active instruction in its native unit
     * (degrees turned, blocks moved, ticks delayed). 0 when idle.
     */
    float createAvionics$getInstructionProgress();

    /** Tick budget for the active instruction. -1 for AWAIT, 0 when idle. */
    int createAvionics$getInstructionDuration();

    /** Ticks elapsed within the active instruction. 0 when idle. */
    int createAvionics$getInstructionTimer();

    /** Total number of instructions in the queue (including the trailing END). */
    int createAvionics$getInstructionCount();
}
