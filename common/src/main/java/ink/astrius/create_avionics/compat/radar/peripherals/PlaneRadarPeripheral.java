package ink.astrius.create_avionics.compat.radar.peripherals;

import com.happysg.radar.block.radar.plane.StationaryRadarBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import dan200.computercraft.api.lua.LuaFunction;
import ink.astrius.create_avionics.compat.simulated.peripherals.SimPeripheral;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlaneRadarPeripheral extends SimPeripheral<StationaryRadarBlockEntity> {

    public PlaneRadarPeripheral(final StationaryRadarBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "plane_radar";
    }

    @LuaFunction
    public final List<Map<String, Object>> getTracks() {
        final List<Map<String, Object>> out = new ArrayList<>();
        for (final RadarTrack track : this.blockEntity.getTracks()) {
            if (track == null) continue;
            out.add(RadarBearingPeripheral.trackMap(track));
        }
        return out;
    }

    @LuaFunction
    public final List<Float> getPosition() {
        return SimPeripheral.vecList(this.blockEntity.getWorldPos().getCenter());
    }

    @LuaFunction
    public final double getRange() {
        return this.blockEntity.getRange();
    }
}
