package pl.allegro.tech.hermes.domain.topic.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.hk2.api.Factory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractSchemaRepositoryFactory<T> implements Factory<SchemaRepository<T>> {

    protected ExecutorService createSchemaReloadExecutorService(int poolSize, String format) {
        return Executors.newFixedThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat(format + "-schema-reloader-%d").build());
    }

}
