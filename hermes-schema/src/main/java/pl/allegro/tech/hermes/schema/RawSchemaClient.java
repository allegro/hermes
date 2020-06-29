package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaWithId;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;
import java.util.Optional;

public interface RawSchemaClient {

    Optional<SchemaWithId> getSchemaWithId(TopicName topic, SchemaVersion version);

    Optional<SchemaWithId> getLatestSchemaWithId(TopicName topic);

    List<SchemaVersion> getVersions(TopicName topic);

    void registerSchema(TopicName topic, RawSchema rawSchema);

    void deleteAllSchemaVersions(TopicName topic);

    void validateSchema(TopicName topic, RawSchema rawSchema);
}
