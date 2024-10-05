package pl.allegro.tech.hermes.common.schema;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CachedSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.schema.SchemaVersionsResult;

class SchemaCacheRefresherCallback<T> implements TopicCallback {

  private static final Logger logger =
      LoggerFactory.getLogger(SchemaVersionsRepositoryFactory.class);

  public static final boolean REFRESH_ONLINE = true;

  private final CachedSchemaVersionsRepository schemaVersionsRepository;
  private final CachedCompiledSchemaRepository<T> compiledSchemaRepository;

  public SchemaCacheRefresherCallback(
      CachedSchemaVersionsRepository schemaVersionsRepository,
      CachedCompiledSchemaRepository<T> compiledSchemaRepository) {
    this.schemaVersionsRepository = schemaVersionsRepository;
    this.compiledSchemaRepository = compiledSchemaRepository;
  }

  @Override
  public void onTopicRemoved(Topic topic) {
    schemaVersionsRepository.removeFromCache(topic);
    compiledSchemaRepository.removeFromCache(topic);
  }

  @Override
  public void onTopicCreated(Topic topic) {
    refreshSchemas(topic);
  }

  @Override
  public void onTopicChanged(Topic topic) {
    refreshSchemas(topic);
  }

  private void refreshSchemas(Topic topic) {
    if (topic.getContentType() == ContentType.AVRO) {
      logger.info("Refreshing all schemas for {} topic.", topic.getQualifiedName());
      SchemaVersionsResult versions = schemaVersionsRepository.versions(topic, REFRESH_ONLINE);
      if (versions.isSuccess()) {
        refreshCompiledSchemas(topic, versions.get());
      }
    }
  }

  private void refreshCompiledSchemas(Topic topic, List<SchemaVersion> schemaVersions) {
    schemaVersions.forEach(
        schemaVersion -> {
          try {
            compiledSchemaRepository.getSchema(topic, schemaVersion, REFRESH_ONLINE);
          } catch (CouldNotLoadSchemaException e) {
            logger.warn(
                "Schema for topic {} at version {} could not be loaded",
                topic.getQualifiedName(),
                schemaVersion.value(),
                e);
          }
        });
  }
}
