package ink.astrius.create_avionics.compat.radar.peripherals;

import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MonitorPeripheral extends SimPeripheral<MonitorBlockEntity> {

    public MonitorPeripheral(final MonitorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "monitor";
    }

    // Monitor blocks are part of a multiblock; track data is held by the controller.
    // All accessors first resolve to the controller (which may be `this` itself, or null
    // if the multiblock isn't assembled).

    @LuaFunction
    public final List<Map<String, Object>> getTracks() {
        final MonitorBlockEntity controller = this.blockEntity.getController();
        if (controller == null) return List.of();
        final var tracks = controller.getTracks();
        if (tracks == null) return List.of();

        final List<Map<String, Object>> out = new ArrayList<>();
        for (final RadarTrack track : tracks) {
            if (track == null) continue;
            out.add(RadarBearingPeripheral.trackMap(track));
        }
        return out;
    }

    // The track id the player has clicked on the monitor, or empty string if none.
    @LuaFunction
    public final String getSelectedTrackId() {
        final MonitorBlockEntity controller = this.blockEntity.getController();
        if (controller == null) return "";
        final String id = controller.getSelectedEntity();
        return id == null ? "" : id;
    }

    // The full track payload for the player-selected contact, or empty map if none.
    @LuaFunction
    public final Map<String, Object> getSelectedTrack() {
        final MonitorBlockEntity controller = this.blockEntity.getController();
        if (controller == null) return Map.of();
        final var tracks = controller.getTracks();
        if (tracks == null) return Map.of();
        final String selectedId = controller.getSelectedEntity();
        if (selectedId == null || selectedId.isEmpty()) return Map.of();

        for (final RadarTrack track : tracks) {
            if (track == null) continue;
            if (Objects.equals(track.id(), selectedId)) {
                return RadarBearingPeripheral.trackMap(track);
            }
        }
        return Map.of();
    }
}
