package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonInventory;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimKineticPeripheral;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A mounted potato cannon. Reports aim, drive speed, obstruction state, and
 * loaded ammo.
 *
 * @cc.module mounted_potato_cannon
 */
public class MountedPotatoCannonPeripheral extends SimKineticPeripheral<MountedPotatoCannonBlockEntity> {

    public MountedPotatoCannonPeripheral(final MountedPotatoCannonBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "mounted_potato_cannon";
    }

    // --- Aim & geometry ---

    /**
     * Get the cannon's aim direction.
     * The block entity reports the aim in the host sub-level's body frame; this
     * rotates it into world frame so it stays usable as the contraption turns.
     * When the cannon isn't on a sub-level (placed on stationary ground), body
     * frame already equals world frame and the value is returned unchanged.
     *
     * @return Unit vector along the barrel in world frame, as a 3-element list.
     */
    @LuaFunction(mainThread = true)
    public final List<Double> getAimingVector() {
        final Vec3 local = this.blockEntity.getAimingVector();
        final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity);
        // transformNormal: rotation only (no translation), so a unit aim stays a unit.
        return SimPeripheral.vecList(subLevel == null ? local : subLevel.logicalPose().transformNormal(local));
    }

    /**
     * Get the cannon's muzzle position.
     * The block entity reports the muzzle in the host sub-level's local frame
     * (large sub-level coordinates); this projects it out into world frame.
     * When the cannon isn't on a sub-level (placed on stationary ground), local
     * frame already equals world frame and the value is returned unchanged.
     *
     * @return World-frame position of the muzzle, as a 3-element list.
     */
    @LuaFunction(mainThread = true)
    public final List<Double> getBarrelPos() {
        final Vec3 local = this.blockEntity.getBarrelPos();
        final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity);
        // transformPosition: full pose (rotation + translation) for a point.
        return SimPeripheral.vecList(subLevel == null ? local : subLevel.logicalPose().transformPosition(local));
    }

    // --- Drive ---

    /**
     * Get the speed of the driving cogwheel.
     *
     * @return The cogwheel speed.
     */
    @LuaFunction
    public final double getCogwheelSpeed() {
        return this.blockEntity.getCogwheelSpeed();
    }

    // --- Obstruction ---

    /**
     * Check whether the barrel is currently obstructed.
     *
     * @return True if blocked.
     */
    @LuaFunction
    public final boolean isBlocked() {
        return this.blockEntity.isBlocked();
    }

    /**
     * Get the distance to a barrel obstruction, if any.
     *
     * @return Distance along the barrel to the obstruction. Returns nil if clear.
     */
    @LuaFunction
    public final Double getBlockedLength() {
        return this.blockEntity.isBlocked() ? this.blockEntity.getBlockedLength() : null;
    }

    // --- Ammo ---

    /**
     * Check whether the cannon has ammo loaded.
     *
     * @return True if ammo is loaded.
     */
    @LuaFunction
    public final boolean hasAmmo() {
        return !this.ammoStack().isEmpty();
    }

    /**
     * Get the count of loaded ammo.
     *
     * @return The ammo stack size.
     */
    @LuaFunction
    public final int getAmmoCount() {
        return this.ammoStack().getCount();
    }

    /**
     * Get the registry id of the loaded ammo item.
     *
     * @return Registry id of the loaded ammo item (e.g. "minecraft:potato"), or nil
     *         if the cannon is empty.
     */
    @LuaFunction
    public final String getAmmoType() {
        final ItemStack stack = this.ammoStack();
        if (stack.isEmpty()) return null;
        final ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? null : key.toString();
    }

    private ItemStack ammoStack() {
        final MountedPotatoCannonInventory inv = this.blockEntity.getInventory();
        return inv == null ? ItemStack.EMPTY : inv.slot.getStack();
    }
}
