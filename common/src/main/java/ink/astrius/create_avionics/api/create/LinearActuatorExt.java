package ink.astrius.create_avionics.api.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.contraptions.IControlContraption.MovementMode;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

/**
 * Mixin-supplied accessors over Create's {@code LinearActuatorBlockEntity}
 * (parent of mechanical piston, gantry carriage, elevator pulley, rope
 * pulley). Exposes the package-private movement-mode option and the computer
 * behaviour we conditionally inject into peripheraled subclasses.
 */
public interface LinearActuatorExt {

    ScrollOptionBehaviour<MovementMode> createAvionics$movementMode();

    AbstractComputerBehaviour createAvionics$getComputerBehaviour();
}
