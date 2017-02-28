package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.Topic;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface SchemaVersionsRepository {

    default boolean schemaVersionExists(Topic topic, SchemaVersion version) {
        return versions(topic).contains(version);
    }

    default Optional<SchemaVersion> latestSchemaVersion(Topic topic) {
        return versions(topic).stream().max(Comparator.comparingInt(SchemaVersion::value));
    }

    default List<SchemaVersion> versions(Topic topic) {
        return versions(topic, false);
    }

    List<SchemaVersion> versions(Topic topic, boolean online);

    void close();
}
