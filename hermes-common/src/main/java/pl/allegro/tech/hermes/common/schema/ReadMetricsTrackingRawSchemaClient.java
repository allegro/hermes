package pl.allegro.tech.hermes.common.schema;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaData;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ReadMetricsTrackingRawSchemaClient implements RawSchemaClient {
    private final RawSchemaClient rawSchemaClient;
    private final HermesMetrics hermesMetrics;
    private final SchemaRepositoryType schemaRepoType;

    public ReadMetricsTrackingRawSchemaClient(
            RawSchemaClient rawSchemaClient,
            HermesMetrics hermesMetrics,
            SchemaRepositoryType schemaRepoType) {
        this.rawSchemaClient = rawSchemaClient;
        this.hermesMetrics = hermesMetrics;
        this.schemaRepoType = schemaRepoType;
    }

    @Override
    public Optional<RawSchema> getSchema(TopicName topic, SchemaVersion version) {
        return timedSchema(() -> rawSchemaClient.getSchema(topic, version));
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        return timedSchema(() -> rawSchemaClient.getLatestSchema(topic));
    }

    @Override
    public Optional<SchemaData> getSchemaData(TopicName topic, SchemaVersion version) {
        return timedSchema(() -> rawSchemaClient.getSchemaData(topic, version));
    }

    @Override
    public Optional<SchemaData> getLatestSchemaData(TopicName topic) {
        return timedSchema(() -> rawSchemaClient.getLatestSchemaData(topic));
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
        return hermesMetrics.schemaTimer(schemaReadLatency, schemaRepoType).time();
    }

}
