package pl.allegro.tech.hermes.infrastructure.schema;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.schema.CachedSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.DefaultCachedSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.NoCachedSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepoSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepositoryType;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.TopicFieldSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.ZookeeperSchemaSourceProvider;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE;

public class CachedSchemaSourceProviderFactory implements Factory<CachedSchemaSourceProvider> {

    private final ConfigFactory configFactory;
    private final SchemaRepoClient schemaRepoClient;
    private final CuratorFramework curatorFramework;
    private final ZookeeperPaths zookeeperPaths;

    @Inject
    public CachedSchemaSourceProviderFactory(ConfigFactory configFactory, SchemaRepoClient schemaRepoClient,
                                             CuratorFramework curatorFramework, ZookeeperPaths zookeeperPaths) {
        this.configFactory = configFactory;
        this.schemaRepoClient = schemaRepoClient;
        this.curatorFramework = curatorFramework;
        this.zookeeperPaths = zookeeperPaths;
    }

    @Override
    public CachedSchemaSourceProvider provide() {
        SchemaSourceProvider schemaSourceProvider = null;

        switch (SchemaRepositoryType.valueOf(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_TYPE).toUpperCase())) {
            case SCHEMA_REPO:
                schemaSourceProvider = new SchemaRepoSchemaSourceProvider(schemaRepoClient);
                break;
            case ZOOKEEPER:
                schemaSourceProvider = new ZookeeperSchemaSourceProvider(curatorFramework, zookeeperPaths);
                break;
            case TOPIC_FIELD:
                return new NoCachedSchemaSourceProvider(new TopicFieldSchemaSourceProvider());
        }
        return configFactory.getBooleanProperty(Configs.SCHEMA_CACHE_ENABLED)?
                new DefaultCachedSchemaSourceProvider(
                        configFactory.getIntProperty(SCHEMA_CACHE_REFRESH_AFTER_WRITE_MINUTES),
                        configFactory.getIntProperty(SCHEMA_CACHE_EXPIRE_AFTER_WRITE_MINUTES),
                        createSchemaReloadExecutorService(configFactory.getIntProperty(SCHEMA_CACHE_RELOAD_THREAD_POOL_SIZE)),
                        schemaSourceProvider) : new NoCachedSchemaSourceProvider(schemaSourceProvider);


    }

    @Override
    public void dispose(CachedSchemaSourceProvider instance) {

    }

    protected ExecutorService createSchemaReloadExecutorService(int poolSize) {
        return Executors.newFixedThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat("schema-source-reloader-%d").build());
    }
}
