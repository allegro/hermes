package pl.allegro.tech.hermes.management.config;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.infrastructure.schema.repo.JerseySchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;
import pl.allegro.tech.hermes.management.domain.topic.schema.TopicFieldSchemaSourceRepository;
import pl.allegro.tech.hermes.management.infrastructure.schema.SchemaRepoSchemaSourceRepository;
import pl.allegro.tech.hermes.management.infrastructure.schema.ZookeeperSchemaSourceRepository;

import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

@Configuration
@EnableConfigurationProperties(SchemaRepositoryProperties.class)
public class SchemaRepositoryConfiguration {

    @Autowired
    @Lazy
    private TopicService topicService;

    @Autowired
    @Lazy
    private CuratorFramework storageZookeeper;

    @Autowired
    @Lazy
    private ZookeeperPaths zookeeperPaths;

    @Autowired
    private SchemaRepositoryProperties schemaRepositoryProperties;

    @Bean
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "zookeeper", matchIfMissing = true)
    public SchemaSourceRepository zookeeperSchemaSourceRepository() {
        return new ZookeeperSchemaSourceRepository(storageZookeeper, zookeeperPaths);
    }

    @Bean
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "schemaRepo")
    public SchemaSourceRepository schemaRepoSchemaSourceRepository() {
        SchemaRepoClient client = new JerseySchemaRepoClient(ClientBuilder.newClient(), URI.create(schemaRepositoryProperties.getSchemaRepoServerUrl()));
        return new SchemaRepoSchemaSourceRepository(client);
    }

    @Bean
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "topic_field")
    public SchemaSourceRepository topicFieldSchemaSourceRepository() {
        return new TopicFieldSchemaSourceRepository(topicService);
    }

}
