package ink.astrius.create_avionics.content.radar;

import ink.astrius.create_avionics.content.radio.Scatterer;
import ink.astrius.create_avionics.content.radio.SignalModel;
import ink.astrius.create_avionics.content.radio.SimpleSignalModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The weather radar's physics and coding, without a world. */
class WeatherRadarTest {

    private final SignalModel model = new SimpleSignalModel();

    @Test
    void zrRelationsGiveTheDocumentedReflectivities() {
        assertEquals(23.0, this.model.reflectivity(Scatterer.RAIN, 1), 0.05);
        assertEquals(29.4, this.model.reflectivity(Scatterer.RAIN, 2.5), 0.05);
        assertEquals(37.5, this.model.reflectivity(Scatterer.RAIN, 8), 0.05);
        assertEquals(45.4, this.model.reflectivity(Scatterer.RAIN, 25), 0.05);
        assertEquals(55.0, this.model.reflectivity(Scatterer.RAIN, 100), 0.05);
        assertEquals(28.0, this.model.reflectivity(Scatterer.SNOW, 1.25), 0.05);
        assertEquals(16.0, this.model.reflectivity(Scatterer.ICE, 1), 0.05);
        assertEquals(Double.NEGATIVE_INFINITY, this.model.reflectivity(Scatterer.RAIN, 0));
        assertEquals(Double.NEGATIVE_INFINITY, this.model.reflectivity(Scatterer.NONE, 5));
    }

    @Test
    void weakestDetectableEchoGrowsWithTheSquareOfDistance() {
        final double floor = this.model.noiseFloor();
        assertEquals(-20, floor - this.model.volumeEcho(100, 0), 1e-9);
        assertEquals(6.2, floor - this.model.volumeEcho(2048, 0), 0.05);
    }

    @Test
    void attenuationFollowsTheRate() {
        assertEquals(0.0045, this.model.specificAttenuation(Scatterer.RAIN, 2.5), 0.0001);
        assertEquals(0.0182, this.model.specificAttenuation(Scatterer.RAIN, 8), 0.0002);
        assertEquals(0.0018, this.model.specificAttenuation(Scatterer.SNOW, 8), 0.0001);
        assertEquals(0, this.model.specificAttenuation(Scatterer.GROUND, 8));
    }

    @Test
    void aHeavyCoreShadowsWhatIsBehindIt() {
        final int n = 20;
        final Scatterer[] kind = new Scatterer[n];
        final double[] rate = new double[n];
        Arrays.fill(kind, Scatterer.RAIN);
        Arrays.fill(rate, 2.5);
        final double[] clear = new double[n];
        this.model.weatherRadial(32, kind, rate, clear);
        // Seven bins (224 blocks) of 25 mm/h before the last bins
        for (int i = 5; i < 12; i++) {
            rate[i] = 25;
        }
        final double[] shadowed = new double[n];
        this.model.weatherRadial(32, kind, rate, shadowed);
        final double r = 19.5 * 32;
        final double apparentClear = clear[19] - this.model.volumeEcho(r, 0);
        final double apparentShadow = shadowed[19] - this.model.volumeEcho(r, 0);
        // Two-way loss through the core, less what the light rain there would have cost anyway
        final double expected = 2 * 7 * 32 * (this.model.specificAttenuation(Scatterer.RAIN, 25)
                - this.model.specificAttenuation(Scatterer.RAIN, 2.5));
        assertEquals(expected, apparentClear - apparentShadow, 1e-6);
        assertTrue(expected > 25, "a 224-block core of 25 mm/h costs more than 25 dB: " + expected);
    }

    @Test
    void sweepCodesRoundTrip() {
        assertEquals(3, WeatherSweep.encodeDbz(-40));
        assertEquals(255, WeatherSweep.encodeDbz(120));
        for (final double dbz : new double[]{-32, -6.5, 0, 23, 45.5, 94}) {
            assertEquals(dbz, WeatherSweep.decodeDbz(WeatherSweep.encodeDbz(dbz)), 1e-9);
        }
        for (final double v : new double[]{-31.5, -2.25, 0, 7.75, 31.5}) {
            assertEquals(v, WeatherSweep.decodeVelocity(WeatherSweep.encodeVelocity(v)), 1e-9);
        }
        assertEquals(129, WeatherSweep.encodeVelocity(0));
    }

    @Test
    void columnProfile() {
        final WeatherField.Column rain = new WeatherField.Column(1, 10, 120, 220, 3, 0, 160, 0.8);
        final Scatterer[] kind = new Scatterer[1];
        final double[] rate = new double[1];
        kind[0] = Scatterer.NONE;
        WeatherRadarBlockEntity.fill(rain, 100, kind, rate, 0);
        assertEquals(Scatterer.RAIN, kind[0]);
        assertEquals(10, rate[0], 1e-9);
        WeatherRadarBlockEntity.fill(rain, 180, kind, rate, 0);
        assertEquals(Scatterer.ICE, kind[0]);
        // 20 blocks above the freezing level: 2 dB thinner
        assertEquals(-2, this.model.reflectivity(Scatterer.RAIN, rate[0]) - this.model.reflectivity(Scatterer.RAIN, 10), 1e-9);
        kind[0] = Scatterer.NONE;
        WeatherRadarBlockEntity.fill(rain, 230, kind, rate, 0);
        assertEquals(Scatterer.NONE, kind[0]);

        final WeatherField.Column cloud = new WeatherField.Column(0, 0, 120, 140, 3, 0, 160, 0.5);
        WeatherRadarBlockEntity.fill(cloud, 130, kind, rate, 0);
        assertEquals(Scatterer.CLOUD, kind[0]);
        kind[0] = Scatterer.NONE;
        WeatherRadarBlockEntity.fill(cloud, 100, kind, rate, 0);
        assertEquals(Scatterer.NONE, kind[0]);

        // Vanilla: no heights, precipitation up to the cloud layer
        final WeatherField.Column vanilla = new WeatherField.Column(2, 1.25, Double.NaN, WeatherField.VANILLA_CLOUD_TOP,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        WeatherRadarBlockEntity.fill(vanilla, 150, kind, rate, 0);
        assertEquals(Scatterer.SNOW, kind[0]);
        assertEquals(1.25, rate[0], 1e-9);
    }

    @Test
    void fieldValuesReadWithAnyStride() {
        final WeatherField.Column shortRow = WeatherField.Column.read(new double[]{1, 4, 9, 9}, 0, 2);
        assertEquals(1, shortRow.kind());
        assertEquals(4, shortRow.rate());
        assertTrue(Double.isNaN(shortRow.cloudTop()));
        final WeatherField.Column longRow = WeatherField.Column.read(new double[]{2, 1, 100, 150, 1, 2, 90, 0.5, 42}, 0, 9);
        assertEquals(2, longRow.kind());
        assertEquals(0.5, longRow.cloud());
    }
}
