package ink.astrius.create_avionics.content.instrument;

import ink.astrius.create_avionics.content.instrument.peripheral.InstrumentPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Samples its instrument's quantity once a second on the server and syncs it to clients, where the
 * dial eases towards the new reading instead of jumping. Peripherals read the cached sample, so a
 * script polling every tick never triggers a lookup.
 */
public class InstrumentBlockEntity extends BlockEntity {

    public static final int SAMPLE_INTERVAL = 20;
    /** Fraction of the remaining distance the dial covers each client tick. */
    private static final float DIAL_EASING = 0.08f;
    /** Game ticks per in-game hour. */
    private static final double TICKS_PER_HOUR = 1000;

    private final Instrument instrument;
    private @Nullable InstrumentPeripheral peripheral;

    private double value;
    private String source = "";
    private long sampledAt;
    // Rain gauge
    private double totalMm;
    private Atmosphere.Precipitation precipitation = Atmosphere.Precipitation.NONE;
    // Almanac
    private Calendar.Date date = new Calendar.Date(0, Optional.empty(), Optional.empty(), Optional.empty(), 0, "");

    // Client only: where the dial is drawn, and where it was last tick (for partial-tick smoothing)
    private float dial = -1;
    private float previousDial = -1;

    public InstrumentBlockEntity(final BlockPos pos, final BlockState state) {
        super(Instruments.BLOCK_ENTITY.get(), pos, state);
        this.instrument = ((InstrumentBlock) state.getBlock()).instrument();
    }

    public Instrument instrument() {
        return this.instrument;
    }

    /** The peripheral for this block, created once so CC sees the same object on every query. */
    public InstrumentPeripheral peripheral() {
        if (this.peripheral == null) {
            this.peripheral = InstrumentPeripheral.of(this);
        }
        return this.peripheral;
    }

    public double value() {
        return this.value;
    }

    public String source() {
        return this.source;
    }

    public long sampledAt() {
        return this.sampledAt;
    }

    public double totalMm() {
        return this.totalMm;
    }

    public Atmosphere.Precipitation precipitation() {
        return this.precipitation;
    }

    public Calendar.Date date() {
        return this.date;
    }

    public void resetTotal() {
        this.totalMm = 0;
        this.setChanged();
    }

    /** The unit of {@link #value()}: the almanac counts lunar cycles where there is no season calendar. */
    public String unit() {
        if (this.instrument == Instrument.ALMANAC && this.date.season().isEmpty()) {
            return "moon";
        }
        return this.instrument.unit();
    }

    /** Dial position 0..1 of the latest sample. */
    public double normalized() {
        return this.instrument.normalize(this.value);
    }

    public float dial(final float partialTick) {
        return this.previousDial + (this.dial - this.previousDial) * partialTick;
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final InstrumentBlockEntity be) {
        if ((level.getGameTime() + pos.hashCode()) % SAMPLE_INTERVAL != 0) {
            return;
        }
        final double before = be.normalized();
        final String sourceBefore = be.source;
        be.sample(level);
        // Only resync when the dial would visibly move, or something a client displays changed
        if (Math.abs(be.normalized() - before) > 0.002 || !be.source.equals(sourceBefore)
                || be.instrument == Instrument.RAIN_GAUGE) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state,
                                  final InstrumentBlockEntity be) {
        final float target = (float) be.normalized();
        if (be.dial < 0) {
            be.dial = be.previousDial = target;
            return;
        }
        be.previousDial = be.dial;
        be.dial += (target - be.dial) * DIAL_EASING;
    }

    private void sample(final Level level) {
        final BlockPos world = Atmosphere.worldPos(level, this.worldPosition);
        this.sampledAt = level.getGameTime();
        switch (this.instrument) {
            case THERMOMETER -> this.apply(Atmosphere.temperature(level, world));
            case HYGROMETER -> this.apply(Atmosphere.humidity(level, world));
            case BAROMETER -> this.apply(Atmosphere.pressure(level, world));
            case RAIN_GAUGE -> {
                this.precipitation = Atmosphere.precipitation(level, world);
                this.apply(Atmosphere.precipitationRate(level, world));
                this.totalMm += this.value * SAMPLE_INTERVAL / TICKS_PER_HOUR;
            }
            case ALMANAC -> {
                this.date = Calendar.at(level, world);
                this.value = this.date.progress();
                this.source = this.date.source();
                // Swap the face when the calendar appears or goes away (biome, dimension, mod)
                final AlmanacBlock.Dial dial = this.date.season().isPresent()
                        ? AlmanacBlock.Dial.SEASONS : AlmanacBlock.Dial.MOON;
                final BlockState state = this.getBlockState();
                if (state.hasProperty(AlmanacBlock.DIAL) && state.getValue(AlmanacBlock.DIAL) != dial) {
                    level.setBlock(this.worldPosition, state.setValue(AlmanacBlock.DIAL, dial), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private void apply(final Atmosphere.Reading reading) {
        this.value = reading.value();
        this.source = reading.source();
    }

    /** Comparator strength 0..15 for the current reading. */
    public int comparatorSignal() {
        return (int) Math.round(this.normalized() * 15);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("Value", this.value);
        tag.putString("Source", this.source);
        tag.putLong("SampledAt", this.sampledAt);
        if (this.instrument == Instrument.RAIN_GAUGE) {
            tag.putDouble("TotalMm", this.totalMm);
            tag.putString("Precipitation", this.precipitation.name());
        }
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.value = tag.getDouble("Value");
        this.source = tag.getString("Source");
        this.sampledAt = tag.getLong("SampledAt");
        this.totalMm = tag.getDouble("TotalMm");
        try {
            this.precipitation = Atmosphere.Precipitation.valueOf(tag.getString("Precipitation"));
        } catch (final IllegalArgumentException e) {
            this.precipitation = Atmosphere.Precipitation.NONE;
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
