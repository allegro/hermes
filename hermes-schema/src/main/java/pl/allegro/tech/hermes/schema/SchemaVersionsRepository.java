package pl.allegro.tech.hermes.schema;

import java.util.Comparator;
import java.util.Optional;
import pl.allegro.tech.hermes.api.Topic;

public interface SchemaVersionsRepository {

  default boolean schemaVersionExists(Topic topic, SchemaVersion version) {
    return versions(topic).get().contains(version);
  }

  default Optional<SchemaVersion> latestSchemaVersion(Topic topic) {
    return versions(topic).get().stream().max(Comparator.comparingInt(SchemaVersion::value));
  }

  default Optional<SchemaVersion> onlineLatestSchemaVersion(Topic topic) {
    return versions(topic, true).get().stream().max(Comparator.comparingInt(SchemaVersion::value));
  }

  default SchemaVersionsResult versions(Topic topic) {
    return versions(topic, false);
  }

  SchemaVersionsResult versions(Topic topic, boolean online);

  void close();
}
