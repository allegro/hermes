package pl.allegro.tech.hermes.schema;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

public class SchemaCacheRefresher<T> {
    private static final Logger logger = LoggerFactory.getLogger(SchemaCacheRefresher.class);
    private static final boolean REFRESH_ONLINE = true;

    private final SchemaVersionsRepository schemaVersionsRepository;
    private final CompiledSchemaRepository<T> compiledSchemaRepository;

    public SchemaCacheRefresher(SchemaVersionsRepository schemaVersionsRepository,
                                CompiledSchemaRepository<T> compiledSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.compiledSchemaRepository = compiledSchemaRepository;
    }

    public void refreshSchemas(Topic topic) {
        logger.info("Refreshing all schemas for {} topic.", topic.getQualifiedName());
        SchemaVersionsResult versions = schemaVersionsRepository.versions(topic, REFRESH_ONLINE);
        if (versions.isSuccess()) {
            refreshCompiledSchemas(topic, versions.get());
        }
    }

    private void refreshCompiledSchemas(Topic topic, List<SchemaVersion> schemaVersions) {
        schemaVersions.forEach(schemaVersion -> {
            try {
                compiledSchemaRepository.getSchema(topic, schemaVersion, REFRESH_ONLINE);
            } catch (CouldNotLoadSchemaException e) {
                logger.warn("Schema for topic {} at version {} could not be loaded",
                        topic.getQualifiedName(), schemaVersion.value(), e);
            }
        });
    }
}
