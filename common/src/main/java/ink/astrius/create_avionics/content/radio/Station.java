package ink.astrius.create_avionics.content.radio;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Something on the air: it always receives, and may send its ship's report. */
public interface Station {

    /** What one ship sends in one interval, from where, and how loud. */
    record Broadcast(UUID ship, Vec3 from, ShipReport report, double powerDbm) {
    }

    /** The ship this station is on, or null on the ground. Stations don't hear their own ship. */
    @Nullable UUID ship();

    /** The antenna's world position. */
    Vec3 antenna();

    Receiver receiver();

    /** This interval's broadcast, or null if this station isn't the one sending for its ship. */
    @Nullable Broadcast broadcast();

    boolean isRemoved();
}
