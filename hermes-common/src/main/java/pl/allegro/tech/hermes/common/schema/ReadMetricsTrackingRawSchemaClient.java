package pl.allegro.tech.hermes.common.schema;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ReadMetricsTrackingRawSchemaClient implements RawSchemaClient {
    private final RawSchemaClient rawSchemaClient;
    private final HermesMetrics hermesMetrics;

    public ReadMetricsTrackingRawSchemaClient(
            RawSchemaClient rawSchemaClient,
            HermesMetrics hermesMetrics) {
        this.rawSchemaClient = rawSchemaClient;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(TopicName topic, SchemaVersion version) {
        return timedSchema(() -> rawSchemaClient.getRawSchemaWithMetadata(topic, version));
    }

    @Override
    public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(TopicName topic, SchemaId schemaId) {
        return timedSchema(() -> rawSchemaClient.getRawSchemaWithMetadata(topic, schemaId));
    }

    @Override
    public Optional<RawSchemaWithMetadata> getLatestRawSchemaWithMetadata(TopicName topic) {
        return timedSchema(() -> rawSchemaClient.getLatestRawSchemaWithMetadata(topic));
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        return timedVersions(() -> rawSchemaClient.getVersions(topic));
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {
        rawSchemaClient.registerSchema(topic, rawSchema);
    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {
        rawSchemaClient.deleteAllSchemaVersions(topic);
    }

    @Override
    public void validateSchema(TopicName topic, RawSchema rawSchema) {
        rawSchemaClient.validateSchema(topic, rawSchema);
    }

    private <T> T timedSchema(Supplier<? extends T> callable) {
        return timed(callable, Timers.GET_SCHEMA_LATENCY);
    }

    private <T> T timedVersions(Supplier<? extends T> callable) {
        return timed(callable, Timers.GET_SCHEMA_VERSIONS_LATENCY);
    }

    private <T> T timed(Supplier<? extends T> callable, String schemaTimer) {
        try (Timer.Context time = startLatencyTimer(schemaTimer)) {
            return callable.get();
        }
    }

    private Timer.Context startLatencyTimer(String schemaReadLatency) {
        return hermesMetrics.schemaTimer(schemaReadLatency).time();
    }

}
