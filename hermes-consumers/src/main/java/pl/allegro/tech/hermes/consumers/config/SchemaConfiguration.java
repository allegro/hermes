package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.avro.Schema;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.schema.AvroCompiledSchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.RawSchemaClientFactory;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryFactory;
import pl.allegro.tech.hermes.common.schema.SchemaVersionsRepositoryFactory;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRepositoryInstanceResolver;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;

@Configuration
@EnableConfigurationProperties(SchemaProperties.class)
public class SchemaConfiguration {

    @Bean
    public SchemaRepository schemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                                             CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
        return new SchemaRepositoryFactory(schemaVersionsRepository, compiledAvroSchemaRepository).provide();
    }

    @Bean
    public CompiledSchemaRepository<Schema> avroCompiledSchemaRepository(RawSchemaClient rawSchemaClient,
                                                                         SchemaProperties schemaProperties) {
        return new AvroCompiledSchemaRepositoryFactory(
                rawSchemaClient, schemaProperties.getCache().getCompiledMaximumSize(),
                schemaProperties.getCache().getCompiledExpireAfterAccessMinutes(), schemaProperties.getCache().isEnabled()
        ).provide();
    }

    @Bean
    public RawSchemaClient rawSchemaClient(ConfigFactory configFactory,
                                           HermesMetrics hermesMetrics,
                                           ObjectMapper objectMapper,
                                           SchemaRepositoryInstanceResolver resolver,
                                           SchemaProperties schemaProperties) {
        return new RawSchemaClientFactory(configFactory, hermesMetrics, objectMapper, resolver,
                schemaProperties.getRepository().isSubjectSuffixEnabled(), schemaProperties.getRepository().isSubjectNamespaceEnabled()).provide();
    }

    @Bean
    public SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver(SchemaProperties schemaProperties, Client client) {
        URI schemaRepositoryServerUri = URI.create(schemaProperties.getRepository().getServerUrl());
        return new DefaultSchemaRepositoryInstanceResolver(client, schemaRepositoryServerUri);
    }

    @Bean
    public Client schemaRepositoryClient(ObjectMapper mapper, SchemaProperties schemaProperties) {
        ClientConfig config = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, schemaProperties.getRepository().getHttpReadTimeoutMs())
                .property(ClientProperties.CONNECT_TIMEOUT, schemaProperties.getRepository().getHttpConnectTimeoutMs())
                .register(new JacksonJsonProvider(mapper));

        return ClientBuilder.newClient(config);
    }

    @Bean
    public SchemaVersionsRepository schemaVersionsRepositoryFactory(RawSchemaClient rawSchemaClient,
                                                                    SchemaProperties schemaProperties,
                                                                    InternalNotificationsBus notificationsBus,
                                                                    CompiledSchemaRepository<?> compiledSchemaRepository) {
        return new SchemaVersionsRepositoryFactory(rawSchemaClient, schemaProperties.getCache().toSchemaVersionRepositoryParameters(), notificationsBus, compiledSchemaRepository)
                .provide();
    }
}