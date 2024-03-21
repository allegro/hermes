package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ReadMetricsTrackingRawSchemaClient implements RawSchemaClient {
    private final MetricsFacade metricsFacade;
    protected final RawSchemaClient rawSchemaClient;

    public ReadMetricsTrackingRawSchemaClient(
            MetricsFacade metricsFacade,
            RawSchemaClient rawSchemaClient) {
        this.metricsFacade = metricsFacade;
        this.rawSchemaClient = rawSchemaClient;
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

    protected <T> T timedSchema(Supplier<? extends T> callable) {
        return timed(callable, metricsFacade.schemaClient().schemaTimer());
    }

    protected <T> T timedVersions(Supplier<? extends T> callable) {
        return timed(callable, metricsFacade.schemaClient().versionsTimer());
    }

    protected <T> T timed(Supplier<? extends T> callable, HermesTimer timer) {
        try (HermesTimerContext time = timer.time()) {
            return callable.get();
        }
    }
}
