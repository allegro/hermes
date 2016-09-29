package pl.allegro.tech.hermes.common.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.schema.CachedSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.SchemaSourceClient;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SchemaVersionsRepositoryFactory implements Factory<SchemaVersionsRepository> {

    private final SchemaSourceClient schemaSourceClient;
    private final ConfigFactory configFactory;

    @Inject
    public SchemaVersionsRepositoryFactory(SchemaSourceClient schemaSourceClient, ConfigFactory configFactory) {
        this.schemaSourceClient = schemaSourceClient;
        this.configFactory = configFactory;
    }

    @Override
    public SchemaVersionsRepository provide() {
        if (configFactory.getBooleanProperty(Configs.SCHEMA_CACHE_ENABLED)) {
            return new CachedSchemaVersionsRepository(schemaSourceClient,
                    getVersionsReloader(),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES),
                    configFactory.getIntProperty(Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES));
        }
        return new DirectSchemaVersionsRepository(schemaSourceClient);
    }

    private ExecutorService getVersionsReloader() {
        return Executors.newFixedThreadPool(
                configFactory.getIntProperty(Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE),
                new ThreadFactoryBuilder().setNameFormat("schema-source-reloader-%d").build());
    }

    @Override
    public void dispose(SchemaVersionsRepository instance) {

    }
}
