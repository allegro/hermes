package pl.allegro.tech.hermes.management.config;

import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.schemarepo.SchemaRepoRawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;

import javax.ws.rs.client.Client;
import java.net.URI;

@Configuration
@EnableConfigurationProperties({SchemaRepositoryProperties.class})
public class SchemaRepositoryConfiguration {

    @Autowired
    @Lazy
    TopicService topicService;

    @Autowired
    private SchemaRepositoryProperties schemaRepositoryProperties;

    @Bean
    @ConditionalOnMissingBean(RawSchemaClient.class)
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "schema_repo")
    public RawSchemaClient schemaRepoRawSchemaClient(Client httpClient) {
        return new SchemaRepoRawSchemaClient(httpClient, URI.create(schemaRepositoryProperties.getServerUrl()));
    }

    @Bean
    @ConditionalOnMissingBean(RawSchemaClient.class)
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "schema_registry")
    public RawSchemaClient schemaRegistryRawSchemaClient(Client httpClient) {
        return new SchemaRegistryRawSchemaClient(httpClient, URI.create(schemaRepositoryProperties.getServerUrl()));
    }

    @Bean
    public SchemaRepository aggregateSchemaRepository(RawSchemaClient rawSchemaClient) {
        SchemaVersionsRepository versionsRepository = new DirectSchemaVersionsRepository(rawSchemaClient);
        CompiledSchemaRepository<Schema> avroSchemaRepository = new DirectCompiledSchemaRepository<>(
                rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler());

        return new SchemaRepository(versionsRepository, avroSchemaRepository);
    }

}
