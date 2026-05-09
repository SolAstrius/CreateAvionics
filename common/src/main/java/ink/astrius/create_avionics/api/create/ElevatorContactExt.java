package ink.astrius.create_avionics.api.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;

/**
 * Mixin-supplied accessor for the {@code ComputerBehaviour} we inject into
 * Create's {@code ElevatorContactBlockEntity}.
 */
public interface ElevatorContactExt {

    AbstractComputerBehaviour createAvionics$getComputerBehaviour();
}
