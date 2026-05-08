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
    @LuaFunction
    public boolean isAssembled() {
        return this.getMassData() != null;
    }

    /**
     * Get the assembled sub-level's total mass.
     *
     * @return The mass, or 0 if not assembled.
     */
    @LuaFunction
    public double getMass() {
        final MassData data = this.getMassData();
        return data == null ? 0.0 : data.getMass();
    }

    /**
     * Get the assembled sub-level's center of mass.
     *
     * @return A three-element list {x, y, z}.
     */
    @LuaFunction
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
    @LuaFunction
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

    private MassData getMassData() {
        final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity);
        if (!(subLevel instanceof final ServerSubLevel serverSubLevel)) return null;
        return serverSubLevel.getMassTracker();
    }
}
