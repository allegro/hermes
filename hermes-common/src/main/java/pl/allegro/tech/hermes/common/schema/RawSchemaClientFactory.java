package pl.allegro.tech.hermes.common.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

public class RawSchemaClientFactory {

  private final String kafkaNamespace;
  private final String kafkaNamespaceSeparator;
  private final MetricsFacade metricsFacade;
  private final ObjectMapper objectMapper;
  private final SchemaRepositoryInstanceResolver resolver;
  private final boolean subjectSuffixEnabled;
  private final boolean subjectNamespaceEnabled;

  public RawSchemaClientFactory(
      String kafkaNamespace,
      String kafkaNamespaceSeparator,
      MetricsFacade metricsFacade,
      ObjectMapper objectMapper,
      SchemaRepositoryInstanceResolver resolver,
      boolean subjectSuffixEnabled,
      boolean subjectNamespaceEnabled) {
    this.kafkaNamespace = kafkaNamespace;
    this.kafkaNamespaceSeparator = kafkaNamespaceSeparator;
    this.metricsFacade = metricsFacade;
    this.objectMapper = objectMapper;
    this.resolver = resolver;
    this.subjectSuffixEnabled = subjectSuffixEnabled;
    this.subjectNamespaceEnabled = subjectNamespaceEnabled;
  }

  public RawSchemaClient provide() {
    SubjectNamingStrategy subjectNamingStrategy =
        SubjectNamingStrategy.qualifiedName
            .withValueSuffixIf(subjectSuffixEnabled)
            .withNamespacePrefixIf(
                subjectNamespaceEnabled,
                new SubjectNamingStrategy.Namespace(kafkaNamespace, kafkaNamespaceSeparator));
    return createMetricsTrackingClient(
        new SchemaRegistryRawSchemaClient(resolver, objectMapper, subjectNamingStrategy));
  }

  private RawSchemaClient createMetricsTrackingClient(RawSchemaClient rawSchemaClient) {
    return new ReadMetricsTrackingRawSchemaClient(rawSchemaClient, metricsFacade);
  }
}
