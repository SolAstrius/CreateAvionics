package ink.astrius.create_avionics.compat.radar.peripherals;

import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RadarBearingPeripheral extends SimPeripheral<RadarBearingBlockEntity> {

    public RadarBearingPeripheral(final RadarBearingBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "radar";
    }

    @LuaFunction
    public final List<Map<String, Object>> getTracks() {
        final List<Map<String, Object>> out = new ArrayList<>();
        for (final RadarTrack track : this.blockEntity.getTracks()) {
            if (track == null) continue;
            out.add(trackMap(track));
        }
        return out;
    }

    // World-frame center of the radar bearing.
    @LuaFunction
    public final List<Float> getPosition() {
        return SimPeripheral.vecList(this.blockEntity.getWorldPos().getCenter());
    }

    // Current sweep angle in degrees.
    @LuaFunction
    public final double getRotation() {
        return this.blockEntity.getAngle();
    }

    @LuaFunction
    public final double getRotationSpeed() {
        return this.blockEntity.getAngularSpeed();
    }

    @LuaFunction
    public final double getRange() {
        return this.blockEntity.getRange();
    }

    @LuaFunction
    public final int getDishCount() {
        return this.blockEntity.getDishCount();
    }

    static Map<String, Object> trackMap(final RadarTrack track) {
        return Map.of(
                "id", track.id() == null ? "" : track.id(),
                "position", SimPeripheral.vecList(track.position()),
                "velocity", SimPeripheral.vecList(track.velocity()),
                "category", track.trackCategory() == null ? "" : track.trackCategory().toString(),
                "scannedTime", track.scannedTime(),
                "entityType", track.entityType() == null ? "" : track.entityType()
        );
    }
}
