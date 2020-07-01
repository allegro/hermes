package pl.allegro.tech.hermes.benchmark.environment;

import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InMemorySchemaClient implements RawSchemaClient {

    private final TopicName schemaTopicName;
    private final SchemaMetadata schemaMetadata;

    public InMemorySchemaClient(TopicName schemaTopicName, String schemaSource, int id, int version) {
        this.schemaTopicName = schemaTopicName;
        schemaMetadata = SchemaMetadata.of(schemaSource, id, version);
    }

    @Override
    public Optional<SchemaMetadata> getSchemaMetadata(TopicName topic, SchemaVersion version) {
        return schemaTopicName.equals(topic) && Objects.equals(schemaMetadata.getVersion(), version) ?
            Optional.of(schemaMetadata) : Optional.empty();
    }

    @Override
    public Optional<SchemaMetadata> getLatestSchemaMetadata(TopicName topic) {
        return schemaTopicName.equals(topic) ? Optional.of(schemaMetadata) : Optional.empty();
    }

    @Override
    public Optional<SchemaMetadata> getSchemaMetadata(TopicName topic, SchemaId schemaId) {
        return schemaTopicName.equals(topic) && Objects.equals(schemaMetadata.getId(), schemaId) ?
            Optional.of(schemaMetadata) : Optional.empty();
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        return ImmutableList.of(SchemaVersion.valueOf(schemaMetadata.getVersion()));
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {

    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {

    }

    @Override
    public void validateSchema(TopicName topic, RawSchema rawSchema) {

    }
}
