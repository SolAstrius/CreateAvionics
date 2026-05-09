package ink.astrius.create_avionics.api.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;

/**
 * Mixin-supplied accessor exposing the {@code ComputerBehaviour} we inject
 * into Create's {@code GantryShaftBlockEntity}. Returns nil for non-gantry
 * kinetic blocks, since the mixin only attaches the behaviour conditionally.
 */
public interface GantryShaftExt {

    AbstractComputerBehaviour createAvionics$getComputerBehaviour();
}
