package pl.allegro.tech.hermes.common.schema

import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.search.Search
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.RawSchema
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.common.metric.Timers
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.schema.RawSchemaAdminClient
import pl.allegro.tech.hermes.schema.SchemaVersion
import pl.allegro.tech.hermes.test.helper.metrics.MicrometerUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class ReadMetricsTrackingRawSchemaAdminClientTest extends Specification {
    @Shared
    TopicName topicName = TopicName.fromQualifiedName("someGroup.someTopic")

    @Shared
    SchemaVersion schemaVersion = SchemaVersion.valueOf(1)

    @Shared
    RawSchema schema = RawSchema.valueOf("some_schema")

    MeterRegistry meterRegistry = new SimpleMeterRegistry()
    HermesMetrics hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler(""))

    MetricsFacade metricsFacade = new MetricsFacade(meterRegistry, hermesMetrics)

    RawSchemaAdminClient rawSchemaClient = Mock()

    @Subject
    RawSchemaAdminClient readMetricsTrackingClient = new ReadMetricsTrackingRawSchemaAdminClient(rawSchemaClient, metricsFacade)

    def "should track latency metrics for schema retrieval"() {
        expect:
        getSchemaCounterValue() == 0

        when:
        readMetricsTrackingClient.getRawSchemaWithMetadata(topicName, schemaVersion)

        then:
        1 * rawSchemaClient.getRawSchemaWithMetadata(topicName, schemaVersion)
        getSchemaCounterValue() == 1
    }

    def "should track latency metrics for latest schema retrieval"() {
        expect:
        getSchemaCounterValue() == 0

        when:
        readMetricsTrackingClient.getLatestRawSchemaWithMetadata(topicName)

        then:
        1 * rawSchemaClient.getLatestRawSchemaWithMetadata(topicName)
        getSchemaCounterValue() == 1
    }

    def "should track latency metrics for versions retrieval"() {
        expect:
        getVersionsCounterValue() == 0

        when:
        readMetricsTrackingClient.getVersions(topicName)

        then:
        1 * rawSchemaClient.getVersions(topicName)
        getVersionsCounterValue() == 1
    }

    def "should call inner client for non-read operations"() {
        when:
        readMetricsTrackingClient.deleteAllSchemaVersions(topicName)

        then:
        1 * rawSchemaClient.deleteAllSchemaVersions(topicName)
        getVersionsCounterValue() == 0

        when:
        readMetricsTrackingClient.registerSchema(topicName, schema)

        then:
        1 * rawSchemaClient.registerSchema(topicName, schema)
        getSchemaCounterValue() == 0
    }

    private long getSchemaCounterValue() {
        return getCounterValue("schema.get-schema", Timers.GET_SCHEMA_LATENCY)
    }

    private long getVersionsCounterValue() {
        return getCounterValue("schema.get-versions", Timers.GET_SCHEMA_VERSIONS_LATENCY)
    }

    private long getCounterValue(String meterRegistryName, String hermesMetricsName) {
        def meterRegistryCount = MicrometerUtils.metricValue(meterRegistry, meterRegistryName, Search.&timer, Timer.&count).orElse(0L);
        def hermesMetricsCount = hermesMetrics.schemaTimer(hermesMetricsName).count
        assert meterRegistryCount == hermesMetricsCount
        return meterRegistryCount
    }
}
