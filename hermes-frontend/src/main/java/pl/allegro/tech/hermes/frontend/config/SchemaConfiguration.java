package pl.allegro.tech.hermes.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.avro.Schema;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.schema.AvroCompiledSchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.RawSchemaClientFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryInstanceResolverFactory;
import pl.allegro.tech.hermes.common.schema.SchemaVersionsRepositoryFactory;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS;

@Configuration
public class SchemaConfiguration {

    @Bean
    public SchemaRepository schemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                                             CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
        return new SchemaRepositoryFactory(schemaVersionsRepository, compiledAvroSchemaRepository).provide();
    }

    @Bean
    public CompiledSchemaRepository<Schema> avroCompiledSchemaRepository(RawSchemaClient rawSchemaClient,
                                                                         ConfigFactory configFactory) {
        return new AvroCompiledSchemaRepositoryFactory(rawSchemaClient, configFactory).provide();
    }

    @Bean
    public RawSchemaClient rawSchemaClient(ConfigFactory configFactory,
                                           HermesMetrics hermesMetrics,
                                           ObjectMapper objectMapper,
                                           SchemaRepositoryInstanceResolver resolver) {
        return new RawSchemaClientFactory(configFactory, hermesMetrics, objectMapper, resolver).provide();
    }

    @Bean
    public SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver(ConfigFactory configFactory,
                                                                             Client client) {
        return new SchemaRepositoryInstanceResolverFactory(configFactory, client).provide();
    }

    @Bean
    public Client schemaRepositoryClient(ObjectMapper mapper, ConfigFactory configFactory) {
        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, configFactory.getIntProperty(SCHEMA_REPOSITORY_HTTP_READ_TIMEOUT_MS))
                .property(ClientProperties.CONNECT_TIMEOUT, configFactory.getIntProperty(SCHEMA_REPOSITORY_HTTP_CONNECT_TIMEOUT_MS))
                .register(new JacksonJsonProvider(mapper));

        return ClientBuilder.newClient(config);
    }

    @Bean
    public SchemaVersionsRepository schemaVersionsRepositoryFactory(RawSchemaClient rawSchemaClient,
                                                                    ConfigFactory configFactory,
                                                                    InternalNotificationsBus notificationsBus,
                                                                    CompiledSchemaRepository compiledSchemaRepository) {
        return new SchemaVersionsRepositoryFactory(rawSchemaClient, configFactory, notificationsBus, compiledSchemaRepository)
                .provide();
    }
}