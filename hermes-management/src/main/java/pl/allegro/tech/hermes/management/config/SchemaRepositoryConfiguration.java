package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.avro.Schema;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient;
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRegistryInstanceResolver;
import pl.allegro.tech.hermes.schema.schemarepo.SchemaRepoRawSchemaClient;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

@Configuration
@EnableConfigurationProperties({SchemaRepositoryProperties.class})
public class SchemaRepositoryConfiguration {

    @Autowired
    @Lazy
    TopicService topicService;

    @Autowired
    private SchemaRepositoryProperties schemaRepositoryProperties;

    @Bean(name = "schemaRepositoryClient")
    public Client schemaRepositoryClient(ObjectMapper mapper) {
        ClientConfig config = new ClientConfig()
                .property(ClientProperties.CONNECT_TIMEOUT, schemaRepositoryProperties.getConnectionTimeoutMillis())
                .property(ClientProperties.READ_TIMEOUT, schemaRepositoryProperties.getSocketTimeoutMillis())
                .register(new JacksonJsonProvider(mapper));

        return ClientBuilder.newClient(config);
    }

    @Bean
    @ConditionalOnMissingBean(RawSchemaClient.class)
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "schema_repo")
    public RawSchemaClient schemaRepoRawSchemaClient(Client httpClient) {
        return new SchemaRepoRawSchemaClient(new DefaultSchemaRegistryInstanceResolver(httpClient, URI.create(schemaRepositoryProperties.getServerUrl())));
    }

    @Bean
    @ConditionalOnMissingBean(RawSchemaClient.class)
    @ConditionalOnProperty(value = "schema.repository.type", havingValue = "schema_registry")
    public RawSchemaClient schemaRegistryRawSchemaClient(
            @Qualifier("schemaRepositoryClient") Client httpClient,
            ObjectMapper objectMapper
    ) {
        return new SchemaRegistryRawSchemaClient(new DefaultSchemaRegistryInstanceResolver(httpClient, URI.create(schemaRepositoryProperties.getServerUrl())),
                objectMapper, schemaRepositoryProperties.isValidationEnabled());
    }

    @Bean
    public SchemaRepository aggregateSchemaRepository(RawSchemaClient rawSchemaClient) {
        SchemaVersionsRepository versionsRepository = new DirectSchemaVersionsRepository(rawSchemaClient);
        CompiledSchemaRepository<Schema> avroSchemaRepository = new DirectCompiledSchemaRepository<>(
                rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler());

        return new SchemaRepository(versionsRepository, avroSchemaRepository);
    }

}
