package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;
import java.util.Optional;

public interface RawSchemaClient {

    Optional<RawSchema> getSchema(TopicName topic, SchemaVersion version);

    Optional<RawSchema> getLatestSchema(TopicName topic);

    List<SchemaVersion> getVersions(TopicName topic);

    void registerSchema(TopicName topic, RawSchema rawSchema);

    void deleteAllSchemaVersions(TopicName topic);
}
