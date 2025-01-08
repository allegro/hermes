package pl.allegro.tech.hermes.management.config;

import static pl.allegro.tech.hermes.schema.SubjectNamingStrategy.qualifiedName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.net.URI;
import org.apache.avro.Schema;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.schema.CompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectCompiledSchemaRepository;
import pl.allegro.tech.hermes.schema.DirectSchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaCompilersFactory;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersionsRepository;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient;
import pl.allegro.tech.hermes.schema.resolver.DefaultSchemaRepositoryInstanceResolver;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

@Configuration
@EnableConfigurationProperties({SchemaRepositoryProperties.class})
public class SchemaRepositoryConfiguration {

  @Autowired @Lazy TopicService topicService;

  @Autowired private SchemaRepositoryProperties schemaRepositoryProperties;

  @Bean(name = "schemaRepositoryClient")
  public Client schemaRepositoryClient(ObjectMapper mapper) {
    ClientConfig config =
        new ClientConfig()
            .property(
                ClientProperties.CONNECT_TIMEOUT,
                schemaRepositoryProperties.getConnectionTimeoutMillis())
            .property(
                ClientProperties.READ_TIMEOUT, schemaRepositoryProperties.getSocketTimeoutMillis())
            .register(new JacksonJsonProvider(mapper));

    return ClientBuilder.newClient(config);
  }

  @Bean
  public SubjectNamingStrategy subjectNamingStrategy(
      KafkaClustersProperties kafkaClustersProperties) {
    return qualifiedName
        .withNamespacePrefixIf(
            schemaRepositoryProperties.isSubjectNamespaceEnabled(),
            new SubjectNamingStrategy.Namespace(
                kafkaClustersProperties.getDefaultNamespace(),
                kafkaClustersProperties.getNamespaceSeparator()))
        .withValueSuffixIf(schemaRepositoryProperties.isSubjectSuffixEnabled());
  }

  @Bean
  @ConditionalOnMissingBean(RawSchemaClient.class)
  public RawSchemaClient schemaRegistryRawSchemaClient(
      SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
      ObjectMapper objectMapper,
      SubjectNamingStrategy subjectNamingStrategy) {
    return new SchemaRegistryRawSchemaClient(
        schemaRepositoryInstanceResolver,
        objectMapper,
        schemaRepositoryProperties.isValidationEnabled(),
        schemaRepositoryProperties.getDeleteSchemaPathSuffix(),
        subjectNamingStrategy);
  }

  @Bean
  @ConditionalOnMissingBean(SchemaRepositoryInstanceResolver.class)
  public SchemaRepositoryInstanceResolver defaultSchemaRepositoryInstanceResolver(
      @Qualifier("schemaRepositoryClient") Client client) {
    return new DefaultSchemaRepositoryInstanceResolver(
        client, URI.create(schemaRepositoryProperties.getServerUrl()));
  }

  @Bean
  public SchemaRepository aggregateSchemaRepository(RawSchemaClient rawSchemaClient) {
    SchemaVersionsRepository versionsRepository =
        new DirectSchemaVersionsRepository(rawSchemaClient);
    CompiledSchemaRepository<Schema> avroSchemaRepository =
        new DirectCompiledSchemaRepository<>(
            rawSchemaClient, SchemaCompilersFactory.avroSchemaCompiler());

    return new SchemaRepository(versionsRepository, avroSchemaRepository);
  }
}
