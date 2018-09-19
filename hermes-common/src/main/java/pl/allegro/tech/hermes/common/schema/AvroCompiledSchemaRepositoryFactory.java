package pl.allegro.tech.hermes.common.schema;

import org.apache.avro.Schema;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;

import javax.inject.Inject;

public class AvroCompiledSchemaRepositoryFactory implements Factory<CompiledSchemaRepository<Schema>> {

    private static final Logger logger = LoggerFactory.getLogger(AvroCompiledSchemaRepositoryFactory.class);

    private final RawSchemaClient rawSchemaClient;
    private final ConfigFactory configFactory;
    private final InternalNotificationsBus notificationsBus;
    private final SchemaVersionsRepository schemaVersionsRepository;

    @Inject
    public AvroCompiledSchemaRepositoryFactory(RawSchemaClient rawSchemaClient,
                                               ConfigFactory configFactory,
                                               InternalNotificationsBus notificationsBus,
                                               SchemaVersionsRepository schemaVersionsRepository) {
        this.rawSchemaClient = rawSchemaClient;
        this.configFactory = configFactory;
        this.notificationsBus = notificationsBus;
        this.schemaVersionsRepository = schemaVersionsRepository;
    }

    @Override
    public CompiledSchemaRepository<Schema> provide() {
        CachedCompiledSchemaRepository<Schema> repository = new CachedCompiledSchemaRepository<>(
                new DirectCompiledSchemaRepository<>(rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler()),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_MAXIMUM_SIZE),
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_COMPILED_EXPIRE_AFTER_ACCESS_MINUTES));

        notificationsBus.registerTopicCallback(new TopicCallback() {
            @Override
            public void onTopicRemoved(Topic topic) {
                repository.removeFromCache(topic);
            }

            @Override
            public void onTopicCreated(Topic topic) {
                logger.info("Loading latest schema for created topic {}", topic.getQualifiedName());
                schemaVersionsRepository.onlineLatestSchemaVersion(topic).ifPresent(
                        schemaVersion -> repository.getSchema(topic, schemaVersion, true));
            }

            @Override
            public void onTopicChanged(Topic topic) {
                logger.info("Loading latest schema for updated topic {}", topic.getQualifiedName());
                schemaVersionsRepository.onlineLatestSchemaVersion(topic).ifPresent(
                        schemaVersion -> repository.getSchema(topic, schemaVersion, true));
            }
        });

        return repository;
    }

    @Override
    public void dispose(CompiledSchemaRepository<Schema> instance) {

    }
}
