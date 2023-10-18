package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.schema.RawSchemaAdminClient;

public class ReadMetricsTrackingRawSchemaAdminClient extends ReadMetricsTrackingRawSchemaClient implements RawSchemaAdminClient {
    private final RawSchemaAdminClient rawSchemaAdminClient;
    
    public ReadMetricsTrackingRawSchemaAdminClient(
            RawSchemaAdminClient rawSchemaAdminClient,
            MetricsFacade metricsFacade) {
        super(metricsFacade, rawSchemaAdminClient);
        this.rawSchemaAdminClient = rawSchemaAdminClient;
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {
        rawSchemaAdminClient.registerSchema(topic, rawSchema);
    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {
        rawSchemaAdminClient.deleteAllSchemaVersions(topic);
    }

    @Override
    public void validateSchema(TopicName topic, RawSchema rawSchema) {
        rawSchemaAdminClient.validateSchema(topic, rawSchema);
    }
}
