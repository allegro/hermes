package pl.allegro.tech.hermes.schema;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

public class DirectSchemaVersionsRepository implements SchemaVersionsRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(DirectSchemaVersionsRepository.class);

  private final RawSchemaClient rawSchemaClient;

  public DirectSchemaVersionsRepository(RawSchemaClient rawSchemaClient) {
    this.rawSchemaClient = rawSchemaClient;
  }

  @Override
  public SchemaVersionsResult versions(Topic topic, boolean online) {
    try {
      List<SchemaVersion> versions = rawSchemaClient.getVersions(topic.getName());
      return SchemaVersionsResult.succeeded(versions);
    } catch (Exception e) {
      logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
      return SchemaVersionsResult.failed();
    }
  }

  @Override
  public void close() {
    // nothing to close
  }
}
