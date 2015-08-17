package pl.allegro.tech.hermes.management.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepoClientFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.infrastructure.schemarepo.SchemaRepoClient;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;
import pl.allegro.tech.hermes.management.domain.topic.schema.TopicFieldSchemaSourceRepository;
import pl.allegro.tech.hermes.management.infrastructure.schema.schemarepo.SchemaRepoSchemaSourceRepository;

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

    @Bean
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "zookeeper", matchIfMissing = true)
    public SchemaSourceRepository zookeeperSchemaSourceRepository() {
        return new TopicFieldSchemaSourceRepository(topicService);
    }

    @Bean
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "schemaRepo")
    public SchemaSourceRepository schemaRepoSchemaSourceRepository() {
        SchemaRepoClient client = new SchemaRepoClientFactory(new ConfigFactory()).provide();
        return new SchemaRepoSchemaSourceRepository(client);
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
