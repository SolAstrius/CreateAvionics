package ink.astrius.create_avionics.content.instrument;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * The instrument blocks. Each one measures a single quantity; the model faces north, and the
 * dial range here is what the moving partial sweeps.
 */
public enum Instrument implements StringRepresentable {
    /** Air temperature in degrees Celsius. The partial is a liquid column scaled along Y. */
    THERMOMETER("thermometer", "C", -30, 50, box(4, 0, 5, 12, 15, 11)),
    /** Relative humidity, 0..1. The needle sweeps -90 (dry) to +90 (wet). */
    HYGROMETER("hygrometer", "rh", 0, 1, box(2, 0, 6, 14, 14, 13)),
    /** Static air pressure in hectopascals. The needle sweeps -135 (stormy) to +135 (fair). */
    BAROMETER("barometer", "hPa", 950, 1050, box(3, 0, 8, 13, 13, 13)),
    /** Precipitation rate in millimetres per hour. No partial: the water is painted. */
    RAIN_GAUGE("rain_gauge", "mm/h", 0, 10, box(3, 0, 3, 13, 13, 13)),
    /** Progress through the year, 0..1 (the lunar cycle without seasons). The hand turns clockwise from the start of spring, or from the full moon on the moon dial. */
    ALMANAC("almanac", "year", 0, 1, box(2, 0, 7, 14, 16, 15));

    public static final Codec<Instrument> CODEC = StringRepresentable.fromEnum(Instrument::values);

    private final String id;
    private final String unit;
    private final double min;
    private final double max;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    Instrument(final String id, final String unit, final double min, final double max, final double[] northBox) {
        this.id = id;
        this.unit = unit;
        this.min = min;
        this.max = max;
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            this.shapes.put(facing, rotate(northBox, facing));
        }
    }

    public String id() {
        return this.id;
    }

    public String unit() {
        return this.unit;
    }

    /** Position of {@code value} on the dial, clamped to 0..1. */
    public double normalize(final double value) {
        return Math.clamp((value - this.min) / (this.max - this.min), 0, 1);
    }

    public VoxelShape shape(final Direction facing) {
        return this.shapes.get(facing);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    private static double[] box(final double x1, final double y1, final double z1,
                                final double x2, final double y2, final double z2) {
        return new double[]{x1, y1, z1, x2, y2, z2};
    }

    /** Rotate a north-facing box about the block centre, matching the blockstate's Y rotation. */
    private static VoxelShape rotate(final double[] b, final Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]);
            case SOUTH -> Block.box(16 - b[3], b[1], 16 - b[5], 16 - b[0], b[4], 16 - b[2]);
            case WEST -> Block.box(b[2], b[1], 16 - b[3], b[5], b[4], 16 - b[0]);
            default -> Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
        };
    }
}
