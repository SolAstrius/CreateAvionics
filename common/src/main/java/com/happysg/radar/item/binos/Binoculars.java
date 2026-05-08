package com.happysg.radar.item.binos;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class Binoculars extends SpyglassItem {
    private static final Logger LOGGER = LogUtils.getLogger();
    // how far the ray should go
    private static final double MAX_DISTANCE = 512.0;

    // step size for walking along the ray (smaller = more accurate, slightly more expensive)
    private static final double STEP = 0.15;
    private static final String TAG_LAST_HIT = "LastHitPos";
    private BlockPos targetBlock;

    public Binoculars(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable(CreateRadar.MODID + ".binoculars.base_text"));
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            tooltipComponents.add(Component.translatable(CreateRadar.MODID + ".binoculars.no_controller"));
            return;
        }

        CompoundTag tag = customData.copyTag();

        BlockPos filtererPos = NbtUtils.readBlockPos(tag, "filtererPos").orElse(null);
        if (filtererPos != null) {
            tooltipComponents.add(
                    Component.translatable(CreateRadar.MODID + ".binoculars.controller")
                            .append(": " + filtererPos.toShortString())
            );
        } else {
            tooltipComponents.add(Component.translatable(CreateRadar.MODID + ".binoculars.no_controller"));
        }
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) {
            return super.useOn(context);
        }

        if (context.getLevel().getBlockEntity(clickedPos) instanceof NetworkFiltererBlockEntity blockEntity) {
            player.displayClientMessage(
                    Component.translatable(CreateRadar.MODID + ".binoculars.paired").withStyle(ChatFormatting.BLUE),
                    true
            );

            ItemStack stack = context.getItemInHand();
            CompoundTag tag = getCustomTag(stack);

            tag.put("filterPos", NbtUtils.writeBlockPos(blockEntity.getBlockPos()));
            tag.put("filtererPos", NbtUtils.writeBlockPos(blockEntity.getBlockPos()));

            setCustomTag(stack, tag);

            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    public static void setLastHit(ItemStack stack, @Nullable BlockPos pos) {
        CompoundTag tag = getCustomTag(stack);

        if (pos == null) {
            tag.remove(TAG_LAST_HIT);
            removeCustomTagIfEmpty(stack, tag);
            return;
        }

        tag.put(TAG_LAST_HIT, NbtUtils.writeBlockPos(pos));
        setCustomTag(stack, tag);
    }

    @Nullable
    public static BlockPos getLastHit(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }

        CompoundTag tag = data.copyTag();
        return NbtUtils.readBlockPos(tag, TAG_LAST_HIT).orElse(null);
    }

    public static boolean hasLastHit(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return false;
        }

        CompoundTag tag = data.copyTag();
        return NbtUtils.readBlockPos(tag, TAG_LAST_HIT).isPresent();
    }

    public static void clearLastHit(ItemStack stack) {
        CompoundTag tag = getCustomTag(stack);
        tag.remove(TAG_LAST_HIT);
        removeCustomTagIfEmpty(stack, tag);
    }
    private static CompoundTag getCustomTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private static void setCustomTag(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return;
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
    }

    private static void removeCustomTagIfEmpty(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }
}

