package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CachedSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.SchemaOnlineRefresher;


class SchemaCacheRefresherCallback<T> implements TopicCallback {

    private final CachedSchemaVersionsRepository schemaVersionsRepository;
    private final CachedCompiledSchemaRepository<T> compiledSchemaRepository;
    private final SchemaOnlineRefresher<T> schemaRefresher;

    public SchemaCacheRefresherCallback(CachedSchemaVersionsRepository schemaVersionsRepository, CachedCompiledSchemaRepository<T> compiledSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.compiledSchemaRepository = compiledSchemaRepository;
        this.schemaRefresher = new SchemaOnlineRefresher<>(schemaVersionsRepository, compiledSchemaRepository);
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
            schemaRefresher.refreshSchemas(topic);
        }
    }
}
