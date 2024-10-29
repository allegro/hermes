package pl.allegro.tech.hermes.consumers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.net.URI;
import org.apache.avro.Schema;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
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

@Configuration
@EnableConfigurationProperties({SchemaProperties.class, KafkaClustersProperties.class})
public class SchemaConfiguration {

  @Bean
  public SchemaRepository schemaRepository(
      SchemaVersionsRepository schemaVersionsRepository,
      CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
    return new SchemaRepositoryFactory(schemaVersionsRepository, compiledAvroSchemaRepository)
        .provide();
  }

  @Bean
  public CompiledSchemaRepository<Schema> avroCompiledSchemaRepository(
      RawSchemaClient rawSchemaClient, SchemaProperties schemaProperties) {
    return new AvroCompiledSchemaRepositoryFactory(
            rawSchemaClient,
            schemaProperties.getCache().getCompiledMaximumSize(),
            schemaProperties.getCache().getCompiledExpireAfterAccess(),
            schemaProperties.getCache().isEnabled())
        .provide();
  }

  @Bean
  public RawSchemaClient rawSchemaClient(
      MetricsFacade metricsFacade,
      ObjectMapper objectMapper,
      SchemaRepositoryInstanceResolver resolver,
      SchemaProperties schemaProperties,
      KafkaClustersProperties kafkaProperties) {
    return new RawSchemaClientFactory(
            kafkaProperties.getNamespace(),
            kafkaProperties.getNamespaceSeparator(),
            metricsFacade,
            objectMapper,
            resolver,
            schemaProperties.getRepository().isSubjectSuffixEnabled(),
            schemaProperties.getRepository().isSubjectNamespaceEnabled())
        .provide();
  }

  @Bean
  public SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver(
      SchemaProperties schemaProperties, Client client) {
    URI schemaRepositoryServerUri = URI.create(schemaProperties.getRepository().getServerUrl());
    return new DefaultSchemaRepositoryInstanceResolver(client, schemaRepositoryServerUri);
  }

  @Bean
  public Client schemaRepositoryClient(ObjectMapper mapper, SchemaProperties schemaProperties) {
    ClientConfig config =
        new ClientConfig()
            .property(
                ClientProperties.READ_TIMEOUT,
                (int) schemaProperties.getRepository().getHttpReadTimeout().toMillis())
            .property(
                ClientProperties.CONNECT_TIMEOUT,
                (int) schemaProperties.getRepository().getHttpConnectTimeout().toMillis())
            .register(new JacksonJsonProvider(mapper));

    return ClientBuilder.newClient(config);
  }

  @Bean
  public SchemaVersionsRepository schemaVersionsRepositoryFactory(
      RawSchemaClient rawSchemaClient,
      SchemaProperties schemaProperties,
      InternalNotificationsBus notificationsBus,
      CompiledSchemaRepository<?> compiledSchemaRepository) {
    return new SchemaVersionsRepositoryFactory(
            rawSchemaClient,
            schemaProperties.getCache(),
            notificationsBus,
            compiledSchemaRepository)
        .provide();
  }
}
