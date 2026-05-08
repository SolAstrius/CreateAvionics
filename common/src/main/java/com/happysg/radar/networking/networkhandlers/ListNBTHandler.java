package com.happysg.radar.networking.networkhandlers;

import com.happysg.radar.block.behavior.networks.config.IdentificationConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class ListNBTHandler {

    // Legacy keys (old format)
    private static final String ENTRIES_KEY = "EntriesList";
    private static final String SINGLE_KEY = "IDSTRING";

    // New format root
    private static final String FILTERS_ROOT = "Filters";
    private static final String IDENT_KEY = "identification";

    public static void saveToHeldItem(Player player, List<String> entries) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag root = getCustomTag(stack);
        CompoundTag filters = root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND)
                ? root.getCompound(FILTERS_ROOT)
                : new CompoundTag();

        IdentificationConfig existing = filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)
                ? IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY))
                : IdentificationConfig.DEFAULT;

        IdentificationConfig cfg = new IdentificationConfig(entries, existing.label());

        filters.put(IDENT_KEY, cfg.toTag());
        root.put(FILTERS_ROOT, filters);

        // i remove legacy keys so i dont accidentally load stale data from old fields
        root.remove(ENTRIES_KEY);
        root.remove(SINGLE_KEY);
        root.remove("FriendOrFoeList");

        setCustomTag(stack, root);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static void saveStringToHeldItem(Player player, String value) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag root = getCustomTag(stack);
        CompoundTag filters = root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND)
                ? root.getCompound(FILTERS_ROOT)
                : new CompoundTag();

        IdentificationConfig existing = filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)
                ? IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY))
                : IdentificationConfig.DEFAULT;

        IdentificationConfig cfg = new IdentificationConfig(existing.entries(), value);

        filters.put(IDENT_KEY, cfg.toTag());
        root.put(FILTERS_ROOT, filters);

        root.remove(ENTRIES_KEY);
        root.remove(SINGLE_KEY);
        root.remove("FriendOrFoeList");

        setCustomTag(stack, root);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static LoadedLists loadFromHeldItem(Player player) {
        ItemStack stack = player.getMainHandItem();
        LoadedLists loaded = new LoadedLists();

        if (stack.isEmpty()) {
            return loaded;
        }

        CompoundTag root = getCustomTagOrNull(stack);
        if (root == null) {
            return loaded;
        }

        // New format first
        if (root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND)) {
            CompoundTag filters = root.getCompound(FILTERS_ROOT);
            if (filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)) {
                IdentificationConfig cfg = IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY));
                loaded.entries.addAll(cfg.entries());
                return loaded;
            }
        }

        // Legacy fallback
        ListTag entriesTag = root.getList(ENTRIES_KEY, Tag.TAG_STRING);
        for (int i = 0; i < entriesTag.size(); i++) {
            loaded.entries.add(entriesTag.getString(i));
        }

        return loaded;
    }

    public static String loadStringFromHeldItem(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return "";
        }

        CompoundTag root = getCustomTagOrNull(stack);
        if (root == null) {
            return "";
        }

        // New format first
        if (root.contains(FILTERS_ROOT, Tag.TAG_COMPOUND)) {
            CompoundTag filters = root.getCompound(FILTERS_ROOT);
            if (filters.contains(IDENT_KEY, Tag.TAG_COMPOUND)) {
                String label = IdentificationConfig.fromTag(filters.getCompound(IDENT_KEY)).label();
                return label == null ? "" : label;
            }
        }

        return root.contains(SINGLE_KEY, Tag.TAG_STRING) ? root.getString(SINGLE_KEY) : "";
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

    public static class LoadedLists {
        public final List<String> entries = new ArrayList<>();
    }
}