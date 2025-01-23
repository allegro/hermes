package pl.allegro.tech.hermes.schema;

import static java.lang.String.format;

import pl.allegro.tech.hermes.api.Topic;

public class SchemaExistenceEnsurer {
  private final SchemaRepository schemaRepository;

  public SchemaExistenceEnsurer(SchemaRepository schemaRepository) {
    this.schemaRepository = schemaRepository;
  }

  public void ensureSchemaExists(Topic topic, SchemaVersion version) {
    pullSchemaIfNeeded(topic, version);
  }

  public void ensureSchemaExists(Topic topic, SchemaId id) {
    pullSchemaIfNeeded(topic, id);
  }

  private void pullSchemaIfNeeded(Topic topic, SchemaVersion version) {
    try {
      schemaRepository.getAvroSchema(topic, version);
    } catch (SchemaException ex) {
      pullVersionsOnline(topic);
      throw new SchemaNotLoaded(
          format(
              "Could not find schema version [%s] provided in header for topic [%s]."
                  + " Trying pulling online...",
              version, topic),
          ex);
    }
  }

  private void pullSchemaIfNeeded(Topic topic, SchemaId id) {
    try {
      schemaRepository.getAvroSchema(topic, id);
    } catch (SchemaException ex) {
      throw new SchemaNotLoaded(
          format(
              "Could not find schema id [%s] provided in header for topic [%s]."
                  + " Trying pulling online...",
              id, topic),
          ex);
    }
  }

  private void pullVersionsOnline(Topic topic) {
    schemaRepository.refreshVersions(topic);
  }

  public static class SchemaNotLoaded extends RuntimeException {
    SchemaNotLoaded(String msg, Throwable th) {
      super(msg, th);
    }

    SchemaNotLoaded(String msg) {
      super(msg);
    }
  }
}
