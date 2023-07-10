package pl.allegro.tech.hermes.metrics;

public class HermesGauge {
    protected final io.micrometer.core.instrument.Gauge micrometerGauge;
    protected final com.codahale.metrics.Gauge graphiteGauge;

    public HermesGauge(io.micrometer.core.instrument.Gauge micrometerGauge, com.codahale.metrics.Gauge graphiteGauge) {
        this.micrometerGauge = micrometerGauge;
        this.graphiteGauge = graphiteGauge;
    }
}
