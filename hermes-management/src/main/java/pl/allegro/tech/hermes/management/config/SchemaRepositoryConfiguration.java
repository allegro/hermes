package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchema;
import org.apache.avro.Schema;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SimpleSchemaVersionsRepository;
import pl.allegro.tech.hermes.infrastructure.schema.repo.JerseySchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;
import pl.allegro.tech.hermes.management.infrastructure.schema.SchemaRepoSchemaSourceRepository;
import pl.allegro.tech.hermes.management.infrastructure.schema.ZookeeperSchemaSourceRepository;

import javax.ws.rs.client.Client;
import java.net.URI;

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
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "zookeeper", matchIfMissing = true)
    public SchemaSourceRepository zookeeperSchemaSourceRepository() {
        return new ZookeeperSchemaSourceRepository(storageZookeeper, zookeeperPaths);
    }

    @Bean
    @ConditionalOnMissingBean(SchemaSourceRepository.class)
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "schema_repo")
    public SchemaSourceRepository schemaRepoSchemaSourceRepository(Client jerseyClient) {
        SchemaRepoClient client = new JerseySchemaRepoClient(jerseyClient, URI.create(schemaRepositoryProperties.getServerUrl()));
        return new SchemaRepoSchemaSourceRepository(client);
    }

    @Bean
    public SchemaSourceProvider schemaSourceProvider(SchemaSourceRepository schemaSourceRepository) {
        return schemaSourceRepository;
    }

    @Bean
    public SchemaRepository aggregateSchemaRepository(SchemaSourceProvider schemaSourceProvider, ObjectMapper objectMapper) {
        SchemaVersionsRepository versionsRepository = new SimpleSchemaVersionsRepository(schemaSourceProvider);
        CompiledSchemaRepository<Schema> avroSchemaRepository = new DirectCompiledSchemaRepository<>(
                schemaSourceProvider, SchemaCompilersFactory.avroSchemaCompiler());
        CompiledSchemaRepository<JsonSchema> jsonSchemaRepository = new DirectCompiledSchemaRepository<>(
                schemaSourceProvider, SchemaCompilersFactory.jsonSchemaCompiler(objectMapper));

        return new SchemaRepository(versionsRepository, avroSchemaRepository, jsonSchemaRepository);
    }

}
