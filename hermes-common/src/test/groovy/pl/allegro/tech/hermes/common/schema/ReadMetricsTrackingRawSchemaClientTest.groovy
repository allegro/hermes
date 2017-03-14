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

import static pl.allegro.tech.hermes.common.schema.SchemaRepositoryType.SCHEMA_REGISTRY

class ReadMetricsTrackingRawSchemaClientTest extends Specification {
    @Shared
    TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared
    SchemaVersion schemaVersion = SchemaVersion.valueOf(1)

    @Shared
    RawSchema schema = RawSchema.valueOf("some_schema")

    @Shared
    String REPO_TYPE = SCHEMA_REGISTRY.toString()

    HermesMetrics hermesMetrics = Mock()
    Timer schemaLatencyTimer = Mock()
    Timer.Context schemaLatencyTime = Mock()
    Timer schemaVersionsLatencyTimer = Mock()
    Timer.Context schemaVersionsLatencyTime = Mock()

    RawSchemaClient rawSchemaClient = Mock()

    @Subject
    RawSchemaClient readMetricsTrackingClient = new ReadMetricsTrackingRawSchemaClient(rawSchemaClient, hermesMetrics, SCHEMA_REGISTRY)


    def "should track latency metrics for schema retrieval"(){
        when:
        readMetricsTrackingClient.getSchema(topicName, schemaVersion)

        then:
        1 * hermesMetrics.schemaTimer(Timers.SCHEMA_READ_LATENCY, REPO_TYPE) >> schemaLatencyTimer
        1 * schemaLatencyTimer.time() >> schemaLatencyTime

        then:
        1 * rawSchemaClient.getSchema(topicName, schemaVersion)

        then:
        1 * schemaLatencyTime.stop()
    }

    def "should track latency metrics for latest schema retrieval"(){
        when:
        readMetricsTrackingClient.getLatestSchema(topicName)

        then:
        1 * hermesMetrics.schemaTimer(Timers.SCHEMA_READ_LATENCY, REPO_TYPE) >> schemaLatencyTimer
        1 * schemaLatencyTimer.time() >> schemaLatencyTime

        then:
        1 * rawSchemaClient.getLatestSchema(topicName)

        then:
        1 * schemaLatencyTime.stop()
    }

    def "should track latency metrics for versions retrieval"(){
        when:
        readMetricsTrackingClient.getVersions(topicName)

        then:
        1 * hermesMetrics.schemaTimer(Timers.SCHEMA_VERSIONS_READ_LATENCY, REPO_TYPE) >> schemaVersionsLatencyTimer
        1 * schemaVersionsLatencyTimer.time() >> schemaVersionsLatencyTime

        then:
        1 * rawSchemaClient.getVersions(topicName)

        then:
        1 * schemaVersionsLatencyTime.stop()
    }

    def "should call inner client for non-read operations"() {
        when:
        readMetricsTrackingClient.deleteAllSchemaVersions(topicName)

        then:
        1 * rawSchemaClient.deleteAllSchemaVersions(topicName)

        when:
        readMetricsTrackingClient.registerSchema(topicName, schema)

        then:
        1 * rawSchemaClient.registerSchema(topicName, schema)
    }
}
