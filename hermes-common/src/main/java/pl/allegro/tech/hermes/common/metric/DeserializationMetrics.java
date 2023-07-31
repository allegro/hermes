package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

import static com.codahale.metrics.MetricRegistry.name;

public class DeserializationMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    private static final String BASE_PATH = "content.avro.deserialization";
    private static final String ERRORS_PATH = BASE_PATH + ".errors";

    public DeserializationMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesCounter errorsForHeaderSchemaVersion() {
        return HermesCounters.from(
                deserializationErrorCounter("headerSchemaVersion"),
                hermesMetrics.counter(name(ERRORS_PATH, "headerSchemaVersion"))
        );

    }

    public HermesCounter errorsForHeaderSchemaId() {
        return HermesCounters.from(
                deserializationErrorCounter("headerSchemaId"),
                hermesMetrics.counter(name(ERRORS_PATH, "headerSchemaId"))
        );
    }

    public HermesCounter errorsForSchemaIdAwarePayload() {
        return HermesCounters.from(
                deserializationErrorCounter("payloadWithSchemaId"),
                hermesMetrics.counter(name(ERRORS_PATH, "payloadWithSchemaId"))
        );
    }

    public HermesCounter errorsForSchemaVersionTruncation() {
        return HermesCounters.from(
                deserializationErrorCounter("schemaVersionTruncation"),
                hermesMetrics.counter(name(ERRORS_PATH, "schemaVersionTruncation"))
        );
    }

    private io.micrometer.core.instrument.Counter deserializationErrorCounter(String schemaSource) {
        return meterRegistry.counter(ERRORS_PATH, Tags.of("deserialization_type", schemaSource));
    }

    public HermesCounter missingSchemaIdInPayload() {
        return HermesCounters.from(
                meterRegistry.counter(name(BASE_PATH, "missing_schemaIdInPayload")),
                hermesMetrics.counter(name(BASE_PATH, "missed", "schemaIdInPayload"))
        );
    }

    public HermesCounter usingHeaderSchemaVersion() {
        return HermesCounters.from(
                deserializationAttemptCounter("headerSchemaVersion"),
                hermesMetrics.counter(name(BASE_PATH, "using", "headerSchemaVersion"))
        );
    }

    public HermesCounter usingHeaderSchemaId() {
        return HermesCounters.from(
                deserializationAttemptCounter("headerSchemaId"),
                hermesMetrics.counter(name(BASE_PATH, "using", "headerSchemaId"))
        );
    }

    public HermesCounter usingSchemaIdAware() {
        return HermesCounters.from(
                deserializationAttemptCounter("payloadWithSchemaId"),
                hermesMetrics.counter(name(BASE_PATH, "using", "schemaIdAware"))
        );
    }

    public HermesCounter usingSchemaVersionTruncation() {
        return HermesCounters.from(
                deserializationAttemptCounter("schemaVersionTruncation"),
                hermesMetrics.counter(name(BASE_PATH, "using", "schemaVersionTruncation"))
        );
    }

    private io.micrometer.core.instrument.Counter deserializationAttemptCounter(String deserializationType) {
        return meterRegistry.counter(BASE_PATH, Tags.of("deserialization_type", deserializationType));
    }
}
