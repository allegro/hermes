package pl.allegro.tech.hermes.common.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.schema.CachedCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.CachedSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SchemaVersionsRepositoryFactory {

    private final RawSchemaClient rawSchemaClient;
    private final ConfigFactory configFactory;
    private final InternalNotificationsBus notificationsBus;
    private final CompiledSchemaRepository<?> compiledSchemaRepository;

    public SchemaVersionsRepositoryFactory(RawSchemaClient rawSchemaClient,
                                           ConfigFactory configFactory,
                                           InternalNotificationsBus notificationsBus,
                                           CompiledSchemaRepository<?> compiledSchemaRepository) {
        this.rawSchemaClient = rawSchemaClient;
        this.configFactory = configFactory;
        this.notificationsBus = notificationsBus;
        this.compiledSchemaRepository = compiledSchemaRepository;
    }

    public SchemaVersionsRepository provide() {
        if (configFactory.getBooleanProperty(Configs.SCHEMA_CACHE_ENABLED)) {
            CachedSchemaVersionsRepository cachedSchemaVersionsRepository = new CachedSchemaVersionsRepository(
                    rawSchemaClient,
                    getVersionsReloader(),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES));

            notificationsBus.registerTopicCallback(
                    new SchemaCacheRefresherCallback<>(
                            cachedSchemaVersionsRepository,
                            (CachedCompiledSchemaRepository<?>) compiledSchemaRepository));

            return cachedSchemaVersionsRepository;
        }
        return new DirectSchemaVersionsRepository(rawSchemaClient);
    }

    private ExecutorService getVersionsReloader() {
        return Executors.newFixedThreadPool(
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE),
                new ThreadFactoryBuilder().setNameFormat("schema-source-reloader-%d").build());
    }
}
