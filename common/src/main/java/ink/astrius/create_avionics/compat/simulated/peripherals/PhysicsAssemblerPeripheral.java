package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import org.joml.Matrix3dc;
import org.joml.Vector3dc;

import java.util.List;

/**
 * A physics assembler. Reports whether the host sub-level is assembled and
 * exposes its mass, center of mass, and inertia tensor.
 *
 * @cc.module physics_assembler
 */
public class PhysicsAssemblerPeripheral extends SimPeripheral<PhysicsAssemblerBlockEntity> {

    public PhysicsAssemblerPeripheral(final PhysicsAssemblerBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "physics_assembler";
    }

    /**
     * Check whether the host sub-level is currently assembled.
     *
     * @return True if assembled.
     */
    @LuaFunction(mainThread = true)
    public boolean isAssembled() {
        return this.getMassData() != null;
    }

    /**
     * Get the assembled sub-level's total mass.
     *
     * @return The mass, or 0 if not assembled.
     */
    @LuaFunction(mainThread = true)
    public double getMass() {
        final MassData data = this.getMassData();
        return data == null ? 0.0 : data.getMass();
    }

    /**
     * Get the assembled sub-level's center of mass.
     *
     * @return A three-element list {x, y, z}.
     */
    @LuaFunction(mainThread = true)
    public List<Double> getCenterOfMass() {
        final MassData data = this.getMassData();
        if (data == null) return List.of(0.0, 0.0, 0.0);
        final Vector3dc com = data.getCenterOfMass();
        return List.of(com.x(), com.y(), com.z());
    }

    /**
     * Get the assembled sub-level's inertia tensor.
     * Row-major 3x3: [Ixx, Ixy, Ixz, Iyx, Iyy, Iyz, Izx, Izy, Izz]. Symmetric, so
     * off-diagonal pairs (Ixy vs Iyx etc.) are equal; ordering between them doesn't matter.
     *
     * @return A nine-element list of tensor entries.
     */
    @LuaFunction(mainThread = true)
    public List<Double> getInertiaTensor() {
        final MassData data = this.getMassData();
        if (data == null) return List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        final Matrix3dc m = data.getInertiaTensor();
        return List.of(
                m.m00(), m.m10(), m.m20(),
                m.m01(), m.m11(), m.m21(),
                m.m02(), m.m12(), m.m22()
        );
    }

    /**
     * Get the UUID of the sub-level this assembler belongs to, or nil if the
     * assembler isn't currently sitting on a sub-level (e.g. placed on
     * stationary ground). Stable for the life of the contraption.
     *
     * @return The sub-level UUID string, or nil.
     */
    @LuaFunction(mainThread = true)
    public String getSubLevelId() {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity);
        return subLevel == null ? null : subLevel.getUniqueId().toString();
    }

    /**
     * Get the display name of the sub-level this assembler belongs to, or
     * nil if there's no sub-level or the sub-level has no name set.
     *
     * @return The sub-level name, or nil.
     */
    @LuaFunction(mainThread = true)
    public String getSubLevelName() {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity);
        return subLevel == null ? null : subLevel.getName();
    }

    private MassData getMassData() {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity);
        if (!(subLevel instanceof final ServerSubLevel serverSubLevel)) return null;
        return serverSubLevel.getMassTracker();
    }
}
