package pl.allegro.tech.hermes.management.config;

import org.apache.curator.framework.CuratorFramework;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties({SchemaRepositoryProperties.class, SchemaCacheProperties.class})
public class SchemaRepositoryConfiguration {

    @Autowired
    SchemaCacheProperties schemaCacheProperties;
    
    @Autowired
    @Lazy
    TopicService topicService;

    @Autowired
    @Lazy
    private CuratorFramework storageZookeeper;

    @Autowired
    @Lazy
    private ZookeeperPaths zookeeperPaths;

    @Autowired
    private SchemaRepositoryProperties schemaRepositoryProperties;

    @Bean
    @ConditionalOnMissingBean(SchemaSourceRepository.class)
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "zookeeper", matchIfMissing = true)
    public SchemaSourceRepository zookeeperSchemaSourceRepository() {
        return new ZookeeperSchemaSourceRepository(storageZookeeper, zookeeperPaths);
    }

    @Bean
    @ConditionalOnMissingBean(SchemaSourceRepository.class)
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "schema_repo")
    public SchemaSourceRepository schemaRepoSchemaSourceRepository() {
        SchemaRepoClient client = new JerseySchemaRepoClient(ClientBuilder.newClient(), URI.create(schemaRepositoryProperties.getServerUrl()));
        return new SchemaRepoSchemaSourceRepository(client);
    }

    @Bean
    @ConditionalOnMissingBean(SchemaSourceRepository.class)
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "topic_field")
    public SchemaSourceRepository topicFieldSchemaSourceRepository() {
        return new TopicFieldSchemaSourceRepository(topicService);
    }

    @Bean
    public SchemaRepository<Schema> avroSchemaRepository(SchemaSourceRepository schemaSourceRepository) {
        return new SchemaRepository<>(
                schemaSourceRepository,
                createSchemaReloadExecutorService(schemaCacheProperties.getPoolSize(), "avro"),
                schemaCacheProperties.getRefreshAfterWriteMinutes(),
                schemaCacheProperties.getExpireAfterWriteMinutes(),
                source -> new Schema.Parser().parse(source.value()));
    }

    private ExecutorService createSchemaReloadExecutorService(int poolSize, String format) {
        return Executors.newFixedThreadPool(poolSize, new ThreadFactoryBuilder().setNameFormat(format + "-schema-reloader-%d").build());
    }

}
