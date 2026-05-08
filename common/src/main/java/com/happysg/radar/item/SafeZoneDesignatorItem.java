package com.happysg.radar.item;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SafeZoneDesignatorItem extends Item {

    public SafeZoneDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!isSelected || !level.isClientSide) {
            return;
        }

        BlockPos monitorPos = getMonitorPos(stack);
        if (monitorPos == null) {
            return;
        }

        if (level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitorBlockEntity) {
            monitorBlockEntity.showSafeZone();
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        CompoundTag data = getCustomTag(stack);

        if (level.getBlockEntity(pos) instanceof MonitorBlockEntity monitorBlockEntity) {
            data.put("monitorPos", NbtUtils.writeBlockPos(monitorBlockEntity.getControllerPos()));
            setCustomTag(stack, data);

            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.set", ChatFormatting.GREEN);
            return InteractionResult.SUCCESS;
        }

        BlockPos monitorPos = NbtUtils.readBlockPos(data, "monitorPos").orElse(null);
        if (monitorPos == null) {
            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.no_monitor", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }

        BlockPos startPos = NbtUtils.readBlockPos(data, "startPos").orElse(null);
        if (startPos == null) {
            if (level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitorBlockEntity) {
                if (monitorBlockEntity.getController().tryRemoveAABB(pos)) {
                    displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.remove", ChatFormatting.RED);
                    return InteractionResult.SUCCESS;
                }
            }

            data.put("startPos", NbtUtils.writeBlockPos(pos));
            setCustomTag(stack, data);

            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.start", ChatFormatting.GREEN);
            return InteractionResult.SUCCESS;
        }

        if (player.isCrouching()) {
            data.remove("startPos");
            setCustomTag(stack, data);

            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.reset", ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitorBlockEntity) {
            monitorBlockEntity.addSafeZone(startPos, pos);

            data.remove("startPos");
            setCustomTag(stack, data);

            displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.end", ChatFormatting.GREEN);
            return InteractionResult.SUCCESS;
        }

        displayMessage(player, CreateRadar.MODID + ".item.safe_zone_designator.no_monitor", ChatFormatting.RED);
        return InteractionResult.FAIL;
    }

    private void displayMessage(Player player, String messageKey, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(messageKey).withStyle(color), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        BlockPos monitorPos = getMonitorPos(stack);
        if (monitorPos != null) {
            tooltipComponents.add(Component.translatable(CreateRadar.MODID + ".guided_fuze.linked_monitor", monitorPos));
        } else {
            tooltipComponents.add(Component.translatable(CreateRadar.MODID + ".guided_fuze.no_monitor"));
        }
    }

    @Nullable
    public BlockPos getMonitorPos(ItemStack stack) {
        CompoundTag data = getCustomTagOrNull(stack);
        if (data == null) {
            return null;
        }

        return NbtUtils.readBlockPos(data, "monitorPos").orElse(null);
    }

    private static CompoundTag getCustomTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    @Nullable
    private static CompoundTag getCustomTagOrNull(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }

        return data.copyTag();
    }

    private static void setCustomTag(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return;
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
    }
}