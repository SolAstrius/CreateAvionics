package ink.astrius.create_avionics.api.simulated;

/**
 * Mixin-supplied accessors for swivel bearing state that's private on the
 * upstream block entity.
 */
public interface SwivelBearingExt {

    /**
     * @return The current {@code LockingSetting} as its lowercase enum name
     * (one of {@code "locked_always"}, {@code "locked_default"},
     * {@code "unlocked_default"}, {@code "unlocked_always"}).
     */
    String createAvionics$getLockingMode();

    /**
     * Set the locking mode by ordinal. 0 = LOCKED_ALWAYS, 1 = LOCKED_DEFAULT,
     * 2 = UNLOCKED_DEFAULT, 3 = UNLOCKED_ALWAYS. Caller is responsible for
     * resolving names to ordinals.
     */
    void createAvionics$setLockingModeOrdinal(int ordinal);

    /**
     * @return True if the bearing's constraint is currently asserting a lock
     * (its block state's POWERED is true).
     */
    boolean createAvionics$isLocking();
}
