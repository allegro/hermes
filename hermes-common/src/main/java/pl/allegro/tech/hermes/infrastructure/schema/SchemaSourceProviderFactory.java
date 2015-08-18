package pl.allegro.tech.hermes.infrastructure.schema;

import org.apache.curator.framework.CuratorFramework;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepoSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepositoryType;
import pl.allegro.tech.hermes.domain.topic.schema.TopicFieldSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.ZookeeperSchemaSourceProvider;
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
        switch (SchemaRepositoryType.valueOf(configFactory.getStringProperty(Configs.SCHEMA_REPOSITORY_TYPE).toUpperCase())) {
            case SCHEMA_REPO:
                return new SchemaRepoSchemaSourceProvider(schemaRepoClient);
            case TOPIC_FIELD:
                return new TopicFieldSchemaSourceProvider();
            case ZOOKEEPER:
                return new ZookeeperSchemaSourceProvider(curatorFramework, zookeeperPaths);
        }
        return null;
    }

    @Override
    public void dispose(SchemaSourceProvider instance) {

    }
}
