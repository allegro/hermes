package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;

import java.util.Optional;

public interface SchemaVersionsRepository {

    boolean schemaVersionExists(Topic topic, int version);

    Optional<Integer> latestSchemaVersion(Topic topic);

}
