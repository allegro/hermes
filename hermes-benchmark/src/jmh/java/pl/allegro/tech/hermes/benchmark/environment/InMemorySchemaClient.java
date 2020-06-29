package pl.allegro.tech.hermes.benchmark.environment;

import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaWithId;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.List;
import java.util.Optional;

public class InMemorySchemaClient implements RawSchemaClient {

    private final TopicName schemaTopicName;
    private final SchemaWithId schemaWithId;

    public InMemorySchemaClient(TopicName schemaTopicName, String schemaSource, int id) {
        this.schemaTopicName = schemaTopicName;
        schemaWithId = SchemaWithId.valueOf(schemaSource, id);
    }

    @Override
    public Optional<SchemaWithId> getSchemaWithId(TopicName topic, SchemaVersion version) {
        return getLatestSchemaWithId(topic);
    }

    @Override
    public Optional<SchemaWithId> getLatestSchemaWithId(TopicName topic) {
        return  schemaTopicName.equals(topic) ? Optional.of(schemaWithId) : Optional.empty();
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

    @Override
    public void validateSchema(TopicName topic, RawSchema rawSchema) {

    }
}
