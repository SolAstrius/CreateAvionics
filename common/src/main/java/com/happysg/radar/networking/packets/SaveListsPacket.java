package com.happysg.radar.networking.packets;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.networking.networkhandlers.ListNBTHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaveListsPacket implements CustomPacketPayload {
    public static final Type<SaveListsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateRadar.MODID, "save_lists")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveListsPacket> STREAM_CODEC =
            StreamCodec.ofMember(SaveListsPacket::encode, SaveListsPacket::decode);

    private final List<String> entries;
    private final String idString;
    private final boolean isIdString;

    /** Constructor for list mode **/
    public SaveListsPacket(List<String> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }

        this.entries = new ArrayList<>(entries);
        this.idString = null;
        this.isIdString = false;
    }

    /** Constructor for single-string mode **/
    public SaveListsPacket(String idString) {
        if (idString == null) {
            throw new IllegalArgumentException("idString cannot be null");
        }

        this.entries = Collections.emptyList();
        this.idString = idString;
        this.isIdString = true;
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeByte(2);
        buf.writeBoolean(isIdString);

        if (isIdString) {
            buf.writeUtf(idString, 32767);
            return;
        }

        buf.writeVarInt(entries.size());
        for (String entry : entries) {
            buf.writeUtf(entry == null ? "" : entry, 32767);
        }
    }

    private static SaveListsPacket decode(RegistryFriendlyByteBuf buf) {
        int version = buf.readByte();

        if (version == 1) {
            boolean isId = buf.readBoolean();

            if (isId) {
                return new SaveListsPacket(buf.readUtf(32767));
            }

            int size = buf.readVarInt();
            List<String> entries = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                entries.add(buf.readUtf(32767));
            }

            return new SaveListsPacket(entries);
        }

        boolean isId = buf.readBoolean();

        if (isId) {
            return new SaveListsPacket(buf.readUtf(32767));
        }

        int size = buf.readVarInt();
        List<String> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            entries.add(buf.readUtf(32767));
        }

        return new SaveListsPacket(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Handle on the server: call the appropriate ListNBTHandler method **/
    public static void handle(SaveListsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (packet.isIdString) {
                ListNBTHandler.saveStringToHeldItem(player, packet.idString);
            } else {
                ListNBTHandler.saveToHeldItem(player, packet.entries);
            }

            player.getInventory().setChanged();
        });
    }

    public static void send(List<String> entries) {
        PacketDistributor.sendToServer(new SaveListsPacket(entries));
    }

    public static void send(String idString) {
        PacketDistributor.sendToServer(new SaveListsPacket(idString));
    }
}