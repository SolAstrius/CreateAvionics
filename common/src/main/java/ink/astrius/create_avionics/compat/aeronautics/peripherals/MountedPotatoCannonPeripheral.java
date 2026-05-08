package ink.astrius.create_avionics.compat.aeronautics.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonInventory;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MountedPotatoCannonPeripheral extends SimPeripheral<MountedPotatoCannonBlockEntity> {

    public MountedPotatoCannonPeripheral(final MountedPotatoCannonBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "mounted_potato_cannon";
    }

    // --- Aim & geometry ---

    // Unit vector along the barrel in world frame.
    @LuaFunction
    public final List<Double> getAimingVector() {
        return SimPeripheral.vecList(this.blockEntity.getAimingVector());
    }

    // World-frame position of the muzzle.
    @LuaFunction
    public final List<Double> getBarrelPos() {
        return SimPeripheral.vecList(this.blockEntity.getBarrelPos());
    }

    // --- Drive ---

    @LuaFunction
    public final double getCogwheelSpeed() {
        return this.blockEntity.getCogwheelSpeed();
    }

    @LuaFunction
    public final double getKineticSpeed() {
        return this.blockEntity.getSpeed();
    }

    // --- Obstruction ---

    @LuaFunction
    public final boolean isBlocked() {
        return this.blockEntity.isBlocked();
    }

    // Distance along the barrel to the obstruction. Returns nil if clear.
    @LuaFunction
    public final Double getBlockedLength() {
        return this.blockEntity.isBlocked() ? this.blockEntity.getBlockedLength() : null;
    }

    // --- Ammo ---

    @LuaFunction
    public final boolean hasAmmo() {
        return !this.ammoStack().isEmpty();
    }

    @LuaFunction
    public final int getAmmoCount() {
        return this.ammoStack().getCount();
    }

    // Registry id of the loaded ammo item (e.g. "minecraft:potato"), or nil
    // if the cannon is empty.
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
