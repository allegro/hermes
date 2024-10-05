package pl.allegro.tech.hermes.common.schema;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

public class ReadMetricsTrackingRawSchemaClient implements RawSchemaClient {
  private final RawSchemaClient rawSchemaClient;
  private final MetricsFacade metricsFacade;

  public ReadMetricsTrackingRawSchemaClient(
      RawSchemaClient rawSchemaClient, MetricsFacade metricsFacade) {
    this.rawSchemaClient = rawSchemaClient;
    this.metricsFacade = metricsFacade;
  }

  @Override
  public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(
      TopicName topic, SchemaVersion version) {
    return timedSchema(() -> rawSchemaClient.getRawSchemaWithMetadata(topic, version));
  }

  @Override
  public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(
      TopicName topic, SchemaId schemaId) {
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
    return timed(callable, metricsFacade.schemaClient().schemaTimer());
  }

  private <T> T timedVersions(Supplier<? extends T> callable) {
    return timed(callable, metricsFacade.schemaClient().versionsTimer());
  }

  private <T> T timed(Supplier<? extends T> callable, HermesTimer timer) {
    try (HermesTimerContext time = timer.time()) {
      return callable.get();
    }
  }
}
