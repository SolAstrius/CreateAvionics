package ink.astrius.create_avionics.api.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;

/**
 * Mixin-supplied accessor for the {@code ComputerBehaviour} we inject into
 * Create's {@code ElevatorPulleyBlockEntity}. The elevator pulley overrides
 * {@code addBehaviours} without calling super, so the linear-actuator hook
 * doesn't reach it — a dedicated mixin attaches the behaviour here.
 */
public interface ElevatorPulleyExt {

    AbstractComputerBehaviour createAvionics$getComputerBehaviour();
}
