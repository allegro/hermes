package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaMetadata;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;
import java.util.Optional;

public interface RawSchemaClient {

    Optional<SchemaMetadata> getSchemaMetadata(TopicName topic, SchemaVersion version);

    Optional<SchemaMetadata> getLatestSchemaMetadata(TopicName topic);

    Optional<SchemaMetadata> getSchemaMetadata(TopicName topic, SchemaId schemaId);

    List<SchemaVersion> getVersions(TopicName topic);

    void registerSchema(TopicName topic, RawSchema rawSchema);

    void deleteAllSchemaVersions(TopicName topic);

    void validateSchema(TopicName topic, RawSchema rawSchema);
}
