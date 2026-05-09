package ink.astrius.create_avionics.api.create;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.contraptions.IControlContraption.RotationMode;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

/**
 * Mixin-supplied accessors over Create's {@code MechanicalBearingBlockEntity}.
 * Lets our peripheral read the bearing's package-private movement-mode option
 * and the computer behaviour we inject into the BE's behaviours list.
 */
public interface MechanicalBearingExt {

    ScrollOptionBehaviour<RotationMode> createAvionics$movementMode();

    AbstractComputerBehaviour createAvionics$getComputerBehaviour();
}
