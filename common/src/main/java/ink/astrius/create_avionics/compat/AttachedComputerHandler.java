package ink.astrius.create_avionics.compat;

import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import org.jspecify.annotations.Nullable;

public class AttachedComputerHandler {

    private final AttachedComputerSet attachedComputers = new AttachedComputerSet();

    public void attach(final IComputerAccess computer) {
        this.attachedComputers.add(computer);
    }

    public void detach(final IComputerAccess computer) {
        this.attachedComputers.remove(computer);
    }

    public void queueEvent(final String event, @Nullable final Object... args) {
        this.attachedComputers.queueEvent(event, args);
    }
}
