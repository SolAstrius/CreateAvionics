package com.happysg.radar.block.monitor;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record MonitorSelectionPacket(BlockPos controllerPos, @Nullable String selectedId) implements CustomPacketPayload {
    public static final Type<MonitorSelectionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateRadar.MODID, "monitor_selection")
    );

    public static final StreamCodec<FriendlyByteBuf, MonitorSelectionPacket> STREAM_CODEC =
            StreamCodec.ofMember(MonitorSelectionPacket::encode, MonitorSelectionPacket::decode);

    private void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(controllerPos);
        buf.writeBoolean(selectedId != null);

        if (selectedId != null) {
            buf.writeUtf(selectedId);
        }
    }

    private static MonitorSelectionPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String id = buf.readBoolean() ? buf.readUtf() : null;
        return new MonitorSelectionPacket(pos, id);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MonitorSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }

            if (!(sp.level().getBlockEntity(packet.controllerPos()) instanceof MonitorBlockEntity be)) {
                return;
            }

            MonitorBlockEntity controller = be.isController() ? be : be.getController();
            if (controller == null || !controller.isLinked()) {
                return;
            }

            if (packet.selectedId() == null) {
                controller.activetrack = null;
                controller.selectedEntity = null;
                controller.setSelectedTargetServer(null);
                controller.notifyUpdate();
                return;
            }

            RadarTrack found = null;
            for (RadarTrack track : controller.cachedTracks) {
                if (packet.selectedId().equals(track.id())) {
                    found = track;
                    break;
                }
            }

            if (found != null) {
                controller.selectedEntity = found.id();
                controller.setSelectedTargetServer(found);
                controller.notifyUpdate();
            }
        });
    }

    public static void send(BlockPos controllerPos, @Nullable String selectedId) {
        PacketDistributor.sendToServer(new MonitorSelectionPacket(controllerPos, selectedId));
    }
}