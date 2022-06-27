package pl.allegro.tech.hermes.common.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.confluent.SchemaRegistryRawSchemaClient;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

public class RawSchemaClientFactory {

    private final ConfigFactory configFactory;
    private final HermesMetrics hermesMetrics;
    private final ObjectMapper objectMapper;
    private final SchemaRepositoryInstanceResolver resolver;
    private final boolean subjectSuffixEnabled;
    private final boolean subjectNamespaceEnabled;

    public RawSchemaClientFactory(ConfigFactory configFactory, HermesMetrics hermesMetrics, ObjectMapper objectMapper,
                                  SchemaRepositoryInstanceResolver resolver, boolean subjectSuffixEnabled, boolean subjectNamespaceEnabled) {
        this.configFactory = configFactory;
        this.hermesMetrics = hermesMetrics;
        this.objectMapper = objectMapper;
        this.resolver = resolver;
        this.subjectSuffixEnabled = subjectSuffixEnabled;
        this.subjectNamespaceEnabled = subjectNamespaceEnabled;
    }

    public RawSchemaClient provide() {
        SubjectNamingStrategy subjectNamingStrategy = SubjectNamingStrategy.qualifiedName
                .withValueSuffixIf(subjectSuffixEnabled)
                .withNamespacePrefixIf(
                        subjectNamespaceEnabled,
                        new SubjectNamingStrategy.Namespace(
                                configFactory.getStringProperty(Configs.KAFKA_NAMESPACE),
                                configFactory.getStringProperty(Configs.KAFKA_NAMESPACE_SEPARATOR)
                        )
                );
        return createMetricsTrackingClient(
                new SchemaRegistryRawSchemaClient(resolver, objectMapper, subjectNamingStrategy)
        );
    }

    private RawSchemaClient createMetricsTrackingClient(RawSchemaClient rawSchemaClient) {
        return new ReadMetricsTrackingRawSchemaClient(rawSchemaClient, hermesMetrics);
    }
}
