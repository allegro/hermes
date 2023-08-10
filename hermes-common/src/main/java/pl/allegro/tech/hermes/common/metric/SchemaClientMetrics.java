package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import static pl.allegro.tech.hermes.common.metric.Timers.GET_SCHEMA_LATENCY;
import static pl.allegro.tech.hermes.common.metric.Timers.GET_SCHEMA_VERSIONS_LATENCY;

public class SchemaClientMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public SchemaClientMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesTimer schemaTimer() {
        return HermesTimer.from(
                timer("schema.get-schema"),
                hermesMetrics.schemaTimer(GET_SCHEMA_LATENCY)
        );
    }

    public HermesTimer versionsTimer() {
        return HermesTimer.from(
                timer("schema.get-versions"),
                hermesMetrics.schemaTimer(GET_SCHEMA_VERSIONS_LATENCY)
        );
    }

    private Timer timer(String name) {
        return meterRegistry.timer(name, Tags.of("schema_repo_type", "schema-registry"));
    }

}
