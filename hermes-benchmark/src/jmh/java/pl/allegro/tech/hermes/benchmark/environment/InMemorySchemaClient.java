package pl.allegro.tech.hermes.benchmark.environment;

import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.List;
import java.util.Optional;

public class InMemorySchemaClient implements RawSchemaClient {

    private final TopicName schemaTopicName;
    private final RawSchema rawSchema;

    public InMemorySchemaClient(TopicName schemaTopicName, String schemaSource) {
        this.schemaTopicName = schemaTopicName;
        rawSchema = RawSchema.valueOf(schemaSource);
    }

    @Override
    public Optional<RawSchema> getSchema(TopicName topic, SchemaVersion version) {
        return getLatestSchema(topic);
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        return schemaTopicName.equals(topic) ? Optional.of(rawSchema) : Optional.empty();
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        return ImmutableList.of(SchemaVersion.valueOf(0));
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {

    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {

    }
}
