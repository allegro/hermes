package pl.allegro.tech.hermes.benchmark.environment;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

public class InMemorySchemaClient implements RawSchemaClient {

  private final TopicName schemaTopicName;
  private final RawSchemaWithMetadata rawSchemaWithMetadata;

  public InMemorySchemaClient(TopicName schemaTopicName, String schemaSource, int id, int version) {
    this.schemaTopicName = schemaTopicName;
    rawSchemaWithMetadata = RawSchemaWithMetadata.of(schemaSource, id, version);
  }

  @Override
  public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(
      TopicName topic, SchemaVersion version) {
    return schemaTopicName.equals(topic)
            && Objects.equals(rawSchemaWithMetadata.getVersion(), version.value())
        ? Optional.of(rawSchemaWithMetadata)
        : Optional.empty();
  }

  @Override
  public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(
      TopicName topic, SchemaId schemaId) {
    return schemaTopicName.equals(topic)
            && Objects.equals(rawSchemaWithMetadata.getId(), schemaId.value())
        ? Optional.of(rawSchemaWithMetadata)
        : Optional.empty();
  }

  @Override
  public Optional<RawSchemaWithMetadata> getLatestRawSchemaWithMetadata(TopicName topic) {
    return schemaTopicName.equals(topic) ? Optional.of(rawSchemaWithMetadata) : Optional.empty();
  }

  @Override
  public List<SchemaVersion> getVersions(TopicName topic) {
    return ImmutableList.of(SchemaVersion.valueOf(rawSchemaWithMetadata.getVersion()));
  }

  @Override
  public void registerSchema(TopicName topic, RawSchema rawSchema) {}

  @Override
  public void deleteAllSchemaVersions(TopicName topic) {}

  @Override
  public void validateSchema(TopicName topic, RawSchema rawSchema) {}
}
