package com.happysg.radar.networking.networkhandlers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class BoolNBThelper {
    private BoolNBThelper() {
    }

    public static void saveBooleansAsBytes(ItemStack stack, boolean[] flags, String key) {
        if (stack == null || flags == null || key == null) {
            return;
        }

        CompoundTag tag = getCustomTag(stack);

        byte[] arr = new byte[flags.length];
        for (int i = 0; i < flags.length; i++) {
            arr[i] = (byte) (flags[i] ? 1 : 0);
        }

        tag.putByteArray(key, arr);
        setCustomTag(stack, tag);
    }

    public static boolean[] loadBooleansFromBytes(ItemStack stack, String key, int expectedLength) {
        boolean[] res = new boolean[Math.max(0, expectedLength)];

        if (stack == null || key == null || expectedLength <= 0) {
            return res;
        }

        CompoundTag tag = getCustomTagOrNull(stack);
        if (tag == null || !tag.contains(key)) {
            return res;
        }

        try {
            byte[] arr = tag.getByteArray(key);
            int len = Math.min(arr.length, res.length);

            for (int i = 0; i < len; i++) {
                res[i] = arr[i] != 0;
            }
        } catch (Throwable ignored) {
        }

        return res;
    }

    public static void loadBooleansInto(ItemStack stack, String key, boolean[] dest) {
        if (dest == null) {
            return;
        }

        boolean[] tmp = loadBooleansFromBytes(stack, key, dest.length);
        System.arraycopy(tmp, 0, dest, 0, Math.min(tmp.length, dest.length));
    }

    private static CompoundTag getCustomTag(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

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