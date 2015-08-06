package pl.allegro.tech.hermes.management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepoClientFactory;
import pl.allegro.tech.hermes.infrastructure.schemarepo.SchemaRepoClient;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;
import pl.allegro.tech.hermes.management.domain.topic.schema.TopicFieldSchemaSourceRepository;
import pl.allegro.tech.hermes.management.infrastructure.schema.schemarepo.SchemaRepoSchemaSourceRepository;

@Configuration
@EnableConfigurationProperties(SchemaRepositoryProperties.class)
public class SchemaRepositoryConfiguration {

    @Autowired
    @Lazy
    private TopicService topicService;

    @Bean
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "zookeeper", matchIfMissing = true)
    public SchemaSourceRepository zookeeperSchemaSourceRepository() {
        return new TopicFieldSchemaSourceRepository(topicService);
    }

    @Bean
    @ConditionalOnProperty(value = "schemaRepository.repositoryType", havingValue = "schemaRepo")
    public SchemaSourceRepository schemaRepoSchemaSourceRepository() {
        SchemaRepoClient client = new SchemaRepoClientFactory(new ConfigFactory()).provide();
        return new SchemaRepoSchemaSourceRepository(client);
    }
}
