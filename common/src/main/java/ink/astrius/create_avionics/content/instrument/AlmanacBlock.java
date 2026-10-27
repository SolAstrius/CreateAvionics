package ink.astrius.create_avionics.content.instrument;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The almanac shows a season dial where there is a season calendar, and a moon dial (the eight
 * phases, bright at the top for the full moon) everywhere else, so the face never claims a season
 * that doesn't exist. The block entity switches the dial when the calendar source changes.
 */
public class AlmanacBlock extends InstrumentBlock {

    public enum Dial implements StringRepresentable {
        SEASONS, MOON;

        @Override
        public String getSerializedName() {
            return this == SEASONS ? "seasons" : "moon";
        }
    }

    public static final EnumProperty<Dial> DIAL = EnumProperty.create("dial", Dial.class);

    public AlmanacBlock(final Properties properties) {
        super(Instrument.ALMANAC, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(DIAL, Dial.MOON));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DIAL);
    }
}
