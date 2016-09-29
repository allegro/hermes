package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.TopicName;

import java.util.List;
import java.util.Optional;

public interface SchemaSourceClient {

    Optional<SchemaSource> getSchemaSource(TopicName topic, SchemaVersion version);

    Optional<SchemaSource> getLatestSchemaSource(TopicName topic);

    List<SchemaVersion> getVersions(TopicName topic);

    void registerSchemaSource(TopicName topic, SchemaSource schemaSource);

    void deleteAllSchemaSources(TopicName topic);
}
