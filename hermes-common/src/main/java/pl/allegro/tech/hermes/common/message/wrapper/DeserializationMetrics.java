package pl.allegro.tech.hermes.common.message.wrapper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import javax.inject.Inject;

import static com.codahale.metrics.MetricRegistry.name;

public class DeserializationMetrics {
    private final MetricRegistry metricRegistry;

    @Inject
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

    public Counter errorsForAnySchemaVersion() {
        return metricRegistry.counter(name(deserializationErrorsPath(), "anySchemaVersion"));
    }

    public Counter errorsForAnyOnlineSchemaVersion() {
        return metricRegistry.counter(name(deserializationErrorsPath(), "anyOnlineSchemaVersion"));
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

    public Counter usingAnySchemaVersion() {
        return metricRegistry.counter(name(deserializationPath(), "using", "anySchemaVersion"));
    }

    private String deserializationErrorsPath() {
        return deserializationPath() + ".errors";
    }

    private String deserializationPath() {
        return "content.avro.deserialization";
    }
}
