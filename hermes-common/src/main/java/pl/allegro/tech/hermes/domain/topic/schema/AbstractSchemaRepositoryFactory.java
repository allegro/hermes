package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractSchemaRepositoryFactory<T> implements Factory<SchemaRepository<T>> {

    protected ExecutorService createSchemaReloadExecutorService(ConfigFactory configFactory, String format) {
        int poolSize = configFactory.getIntProperty(Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE);
        return Executors.newFixedThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat(format + "-schema-reloader-%d").build());
    }

}
