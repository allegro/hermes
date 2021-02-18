package pl.allegro.tech.hermes.common.schema

import com.codahale.metrics.Timer
import pl.allegro.tech.hermes.api.RawSchema
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.common.metric.Timers
import pl.allegro.tech.hermes.schema.RawSchemaClient
import pl.allegro.tech.hermes.schema.SchemaVersion
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class ReadMetricsTrackingRawSchemaClientTest extends Specification {
    @Shared
    TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared
    SchemaVersion schemaVersion = SchemaVersion.valueOf(1)

    @Shared
    RawSchema schema = RawSchema.valueOf("some_schema")

    HermesMetrics hermesMetrics = Mock()
    
    Timer schemaLatencyTimer = new Timer()

    Timer schemaVersionsLatencyTimer = new Timer()

    RawSchemaClient rawSchemaClient = Mock()

    @Subject
    RawSchemaClient readMetricsTrackingClient = new ReadMetricsTrackingRawSchemaClient(rawSchemaClient, hermesMetrics)

    def "should track latency metrics for schema retrieval"(){
        expect:
        schemaLatencyTimer.count == 0

        when:
        readMetricsTrackingClient.getRawSchemaWithMetadata(topicName, schemaVersion)

        then:
        1 * hermesMetrics.schemaTimer(Timers.GET_SCHEMA_LATENCY) >> schemaLatencyTimer
        1 * rawSchemaClient.getRawSchemaWithMetadata(topicName, schemaVersion)
        schemaLatencyTimer.count == 1
    }

    def "should track latency metrics for latest schema retrieval"(){
        expect:
        schemaLatencyTimer.count == 0

        when:
        readMetricsTrackingClient.getLatestRawSchemaWithMetadata(topicName)

        then:
        1 * hermesMetrics.schemaTimer(Timers.GET_SCHEMA_LATENCY) >> schemaLatencyTimer
        1 * rawSchemaClient.getLatestRawSchemaWithMetadata(topicName)
        schemaLatencyTimer.count == 1
    }

    def "should track latency metrics for versions retrieval"(){
        expect:
        schemaVersionsLatencyTimer.count == 0

        when:
        readMetricsTrackingClient.getVersions(topicName)

        then:
        1 * hermesMetrics.schemaTimer(Timers.GET_SCHEMA_VERSIONS_LATENCY) >> schemaVersionsLatencyTimer
        1 * rawSchemaClient.getVersions(topicName)
        schemaVersionsLatencyTimer.count == 1
    }

    def "should call inner client for non-read operations"() {
        when:
        readMetricsTrackingClient.deleteAllSchemaVersions(topicName)

        then:
        1 * rawSchemaClient.deleteAllSchemaVersions(topicName)
        schemaVersionsLatencyTimer.count == 0

        when:
        readMetricsTrackingClient.registerSchema(topicName, schema)

        then:
        1 * rawSchemaClient.registerSchema(topicName, schema)
        schemaLatencyTimer.count == 0
    }
}
