package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.schema.*;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

import javax.inject.Inject;

public class SchemaSourceProviderFactory implements Factory<SchemaSourceProvider> {

    private final ConfigFactory configFactory;
    private final SchemaRepoClient schemaRepoClient;
    private final CuratorFramework curatorFramework;
    private final ZookeeperPaths zookeeperPaths;

    @Inject
    public SchemaSourceProviderFactory(ConfigFactory configFactory, SchemaRepoClient schemaRepoClient,
                                       CuratorFramework curatorFramework, ZookeeperPaths zookeeperPaths) {
        this.configFactory = configFactory;
        this.schemaRepoClient = schemaRepoClient;
        this.curatorFramework = curatorFramework;
        this.zookeeperPaths = zookeeperPaths;
    }

    @Override
    public SchemaSourceProvider provide() {
        String schemaRepositoryType = configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_TYPE).toUpperCase();
        switch (SchemaRepositoryType.valueOf(schemaRepositoryType)) {
            case SCHEMA_REPO:
                return new SchemaRepoSchemaSourceProvider(schemaRepoClient);
            case ZOOKEEPER:
                return new ZookeeperSchemaSourceProvider(curatorFramework, zookeeperPaths);
            case TOPIC_FIELD:
                return new TopicFieldSchemaSourceProvider();
            default:
                throw new IllegalStateException("Unknown schema repository type " + schemaRepositoryType);
        }
    }

    @Override
    public void dispose(SchemaSourceProvider instance) {

    }

}
