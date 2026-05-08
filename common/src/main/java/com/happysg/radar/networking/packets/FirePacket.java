package com.happysg.radar.networking.packets;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.happysg.radar.item.binos.Binoculars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

public record FirePacket(boolean enable) implements CustomPacketPayload {

    private static final String TAG_FILTERER_POS = "filtererPos";

    public static final Type<FirePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateRadar.MODID, "fire")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FirePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            FirePacket::enable,
            FirePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FirePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            ItemStack binos = findBinosStack(player);
            if (binos.isEmpty()) {
                return;
            }

            BlockPos filtererPos = getFiltererPos(binos);
            if (filtererPos == null) {
                return;
            }

            if (!serverLevel.isLoaded(filtererPos)) {
                return;
            }

            if (!(serverLevel.getBlockEntity(filtererPos) instanceof NetworkFiltererBlockEntity filtererBe)) {
                return;
            }

            if (packet.enable()) {
                BlockPos hit = Binoculars.getLastHit(binos);
                if (hit == null) {
                    return;
                }

                filtererBe.onBinocularsTriggered(player, binos, false);
            } else {
                // i reset the binocular targeting when the player releases fire
                filtererBe.onBinocularsTriggered(player, binos, true);
            }

            filtererBe.setChanged();
        });
    }

    public static void send(boolean enable) {
        PacketDistributor.sendToServer(new FirePacket(enable));
    }

    private static ItemStack findBinosStack(ServerPlayer player) {
        // i prioritize the actively used item because that is the clearest scoped intent
        ItemStack using = player.getUseItem();
        if (!using.isEmpty() && using.getItem() instanceof Binoculars) {
            return using;
        }

        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof Binoculars) {
            return main;
        }

        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() instanceof Binoculars) {
            return off;
        }

        return ItemStack.EMPTY;
    }

    @Nullable
    private static BlockPos getFiltererPos(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }

        CompoundTag tag = data.copyTag();
        return NbtUtils.readBlockPos(tag, TAG_FILTERER_POS).orElse(null);
    }
}