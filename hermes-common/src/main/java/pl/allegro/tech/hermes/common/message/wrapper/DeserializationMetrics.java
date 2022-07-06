package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import static com.codahale.metrics.MetricRegistry.name;

public class DeserializationMetrics {
    private final MetricRegistry metricRegistry;

    public DeserializationMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public Counter errorsForHeaderSchemaVersion() {
        return metricRegistry.counter(name(deserializationErrorsPath(), "headerSchemaVersion"));
    }

    public Counter errorsForHeaderSchemaId() {
        return metricRegistry.counter(name(deserializationErrorsPath(), "headerSchemaId"));
    }

    public Counter errorsForSchemaIdAwarePayload() {
        return metricRegistry.counter(name(deserializationErrorsPath(), "payloadWithSchemaId"));
    }

    public Counter errorsForSchemaVersionTruncation() {
        return metricRegistry.counter(name(deserializationErrorsPath(), "schemaVersionTruncation"));
    }

    public Counter missedSchemaIdInPayload() {
        return metricRegistry.counter(name(deserializationPath(), "missed", "schemaIdInPayload"));
    }

    public Counter usingHeaderSchemaVersion() {
        return metricRegistry.counter(name(deserializationPath(), "using", "headerSchemaVersion"));
    }

    public Counter usingHeaderSchemaId() {
        return metricRegistry.counter(name(deserializationPath(), "using", "headerSchemaId"));
    }

    public Counter usingSchemaIdAware() {
        return metricRegistry.counter(name(deserializationPath(), "using", "schemaIdAware"));
    }

    public Counter usingSchemaVersionTruncation() {
        return metricRegistry.counter(name(deserializationPath(), "using", "schemaVersionTruncation"));
    }

    private String deserializationErrorsPath() {
        return deserializationPath() + ".errors";
    }

    private String deserializationPath() {
        return "content.avro.deserialization";
    }
}
