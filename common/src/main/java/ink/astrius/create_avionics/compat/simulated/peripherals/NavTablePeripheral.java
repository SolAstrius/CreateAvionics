package ink.astrius.create_avionics.compat.simulated.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimRegistries;
import ink.astrius.create_avionics.api.simulated.NavigationTargetExt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;

/**
 * A navigation table. Reports the resolved target's bearing, distance, closure
 * rate, vertical offset, and the host sub-level's orientation/heading.
 *
 * @cc.module navigation_table
 */
public class NavTablePeripheral extends SimPeripheral<NavTableBlockEntity> {

    public NavTablePeripheral(final NavTableBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "navigation_table";
    }

    /**
     * Check whether the nav table has resolved a live target.
     * hasTarget reports whether the nav table has resolved a live target
     * (currentTarget). getTargetType / getTargetMetadata describe the held
     * nav-table item itself — these can be populated even when no target is
     * locked yet, so hasTarget() == false does not imply getTargetType() == nil.
     *
     * @return True if a live target is resolved.
     */
    @LuaFunction
    public boolean hasTarget() {
        return this.blockEntity.currentTarget != null;
    }

    /**
     * Get the registry id of the held nav-table item type.
     *
     * @return The target type id, or nil if no item is held.
     */
    @LuaFunction
    public String getTargetType() {
        final NavigationTarget nti = this.blockEntity.getNavTableItem();
        if (nti == null) {
            return null;
        }
        final ResourceLocation key = SimRegistries.NAVIGATION_TARGET.getKey(nti);
        return key != null ? key.toString() : null;
    }

    /**
     * Get item-specific metadata for the held nav-table item.
     * <p>
     * Each nav-target type decides its own keys and value types — there is no
     * fixed schema, and what's in the map depends entirely on which item is
     * held. Use {@link #getTargetType} to branch before reading keys. Returns
     * {@code {}} (empty map, not nil) when no item is held or the held item's
     * type exposes no metadata.
     * <p>
     * Built-in target types and the keys they expose:
     * <table>
     *   <caption>Per-target metadata schema</caption>
     *   <tr><th>{@code getTargetType()}</th><th>Keys</th><th>Notes</th></tr>
     *   <tr>
     *     <td>{@code simulated:compass}</td>
     *     <td>{@code kind} ("lodestone" | "spawn"),
     *         {@code sublevel_id} (string, lodestone only)</td>
     *     <td>"spawn" is the fallback used when the compass isn't bound to a
     *         tracked sub-level; {@code sublevel_id} is the tracker UUID.</td>
     *   </tr>
     *   <tr>
     *     <td>{@code simulated:recovery_compass}</td>
     *     <td>{@code placer_uuid} (string, optional)</td>
     *     <td>UUID of the player whose death location the compass tracks.
     *         Absent until a player has placed the compass into the table.</td>
     *   </tr>
     *   <tr>
     *     <td>{@code simulated:map}</td>
     *     <td>{@code map_id} (number, optional)</td>
     *     <td>Numeric id of the underlying map item. Absent if the held stack
     *         carries no MAP_ID data component (e.g. a blank map).</td>
     *   </tr>
     *   <tr>
     *     <td>{@code simulated:magnet}</td>
     *     <td>—</td>
     *     <td>Static target (10 blocks north of the table); no useful
     *         metadata, returns {@code {}}.</td>
     *   </tr>
     * </table>
     * <p>
     * Compat target types from upstream Simulated (e.g.
     * {@code simulated:explorers_compass}, {@code simulated:natures_compass})
     * register only when their host mod is present and currently expose no
     * metadata. Third-party {@code NavigationTarget} implementations can
     * publish their own keys by implementing
     * {@code NavigationTargetExt#getPeripheralMetadata} via mixin.
     *
     * @return A map of target-type-specific metadata, or an empty map.
     */
    @LuaFunction
    public Map<String, Object> getTargetMetadata() {
        final NavigationTarget nti = this.blockEntity.getNavTableItem();
        if (nti == null) {
            return Map.of();
        }
        return ((NavigationTargetExt) nti).getPeripheralMetadata(this.blockEntity, this.blockEntity.getHeldItem());
    }

    /**
     * Get the raw relative angle to the target, in degrees.
     *
     * @return The relative angle in degrees.
     */
    @LuaFunction
    public double getRelativeAngle() {
        return this.blockEntity.getRelativeAngle();
    }

    /**
     * Get the raw relative angle to the target, in radians.
     *
     * @return The relative angle in radians.
     */
    @LuaFunction
    public double getRelativeAngleRad() {
        return Math.toRadians(this.blockEntity.getRelativeAngle());
    }

    /**
     * Get the forward-error bearing to the target.
     * Forward-error bearing: 0 = target straight ahead of the block's arrow,
     * +90 = target to the right, -90 = to the left, ±180 = behind. The block's
     * arrow points along local +Z, which the raw relativeAngle (atan2(z, x))
     * encodes as 90°; this method subtracts that offset and wraps to ±180.
     *
     * @return The bearing in degrees, in [-180, 180].
     */
    @LuaFunction
    public double getBearing() {
        final double raw = this.blockEntity.getRelativeAngle() - 90.0;
        return ((raw % 360.0) + 540.0) % 360.0 - 180.0;
    }

    /**
     * Get the forward-error bearing to the target, in radians.
     *
     * @return The bearing in radians.
     */
    @LuaFunction
    public double getBearingRad() {
        return Math.toRadians(this.getBearing());
    }

    /**
     * Get the distance to the resolved target.
     * Computed in world frame between the table's projected world position
     * and the target's world position.
     *
     * @return The distance in blocks (= meters in Minecraft units).
     */
    @LuaFunction
    public double getDistanceToTarget() {
        return this.blockEntity.distanceToTarget();
    }

    /**
     * Get the rate at which the table is closing on the target.
     * Nav table samples distance every 11 ticks (0.55s); closure rate reflects
     * that cadence — short-window jitter is dominated by the sampling
     * granularity.
     *
     * @return The closure rate in blocks/sec (positive = approaching).
     */
    @LuaFunction
    public double getClosureRate() {
        return (this.blockEntity.lastDistanceToTarget() - this.blockEntity.distanceToTarget()) / (11.0 / 20.0);
    }

    /**
     * Get the vertical offset between target and the table's projected position.
     * Both positions are in world frame (after projecting the table out of any
     * sub-level), so this gives the signed altitude difference regardless of
     * contraption orientation.
     *
     * @return The signed vertical offset {@code target.y - self.y} in blocks.
     */
    @LuaFunction
    public double getVerticalOffsetToTarget() {
        final Vec3 target = this.blockEntity.getTargetPosition(true);
        if (target == null) {
            return 0.0;
        }
        return target.y - this.blockEntity.getProjectedSelfPos().y;
    }

    /**
     * Get the host sub-level's orientation as a quaternion.
     * Quaternion components in {x, y, z, w} order, matching JOML's constructor and
     * the convention used by CC quaternion libraries (e.g. TechTastic/Advanced-Math).
     *
     * @return A four-element list {x, y, z, w}.
     */
    @LuaFunction
    public List<Double> getOrientation() {
        final Quaterniondc q = this.blockEntity.getSublevelRot();
        return List.of(q.x(), q.y(), q.z(), q.w());
    }

    /**
     * Get the host sub-level's heading in degrees.
     * Heading: ship's +Z axis rotated into world frame, yaw measured as atan2(x, z).
     * 0° = facing world +Z (Minecraft south), matches player-yaw convention.
     *
     * @return The heading in degrees.
     */
    @LuaFunction
    public double getHeading() {
        return Math.toDegrees(this.computeHeadingRad());
    }

    /**
     * Get the host sub-level's heading in radians.
     *
     * @return The heading in radians.
     */
    @LuaFunction
    public double getHeadingRad() {
        return this.computeHeadingRad();
    }

    private double computeHeadingRad() {
        final Quaterniondc q = this.blockEntity.getSublevelRot();
        final Vector3d v = new Vector3d(0, 0, 1);
        q.transform(v);
        return Math.atan2(v.x, v.z);
    }
}
