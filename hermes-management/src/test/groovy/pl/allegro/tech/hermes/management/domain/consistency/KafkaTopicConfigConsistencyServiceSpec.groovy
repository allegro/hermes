package pl.allegro.tech.hermes.management.domain.consistency

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.kafka.common.config.TopicConfig
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.RetentionTime
import pl.allegro.tech.hermes.common.kafka.KafkaTopic
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopics
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.management.config.KafkaConsistencyProperties
import pl.allegro.tech.hermes.management.config.TopicProperties
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.BrokersClusterService
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.TimeUnit

import spock.util.concurrent.PollingConditions

class KafkaTopicConfigConsistencyServiceSpec extends Specification {

    TopicManagement topicManagement = Stub()
    MultiDCAwareService multiDCAwareService = Stub()
    BrokersClusterService cluster = Mock()
    TopicProperties topicProperties = new TopicProperties()
    KafkaConsistencyProperties properties = new KafkaConsistencyProperties()
    def meterRegistry = new SimpleMeterRegistry()
    def metricsFacade = new MetricsFacade(meterRegistry)
    KafkaTopicConfigConsistencyService service

    def setup() {
        topicProperties.setPartitionsPerDc([dc: 2])
        multiDCAwareService.getClusters() >> [cluster]
        multiDCAwareService.getCluster("cluster") >> cluster
        cluster.getClusterName() >> "cluster"
        cluster.getDatacenter() >> "dc"
        service = new KafkaTopicConfigConsistencyService(
                topicManagement, multiDCAwareService, topicProperties,
                new TopicConfigDiffer(), properties, metricsFacade)
    }

    def cleanup() {
        service.stop()
    }

    def "should report and dry-run drift without writing"() {
        given:
        def topic = topic("group.topic", 1)
        def kafkaTopic = kafkaTopic("group.topic")
        topicManagement.getAllTopics() >> [topic]
        cluster.toKafkaTopics(topic) >> new KafkaTopics(kafkaTopic)
        cluster.listTopicNames() >> (["group.topic"] as Set)
        cluster.readTopicConfigs([kafkaTopic]) >> [(kafkaTopic): desired(topic) + [
                (TopicConfig.RETENTION_MS_CONFIG): "2"
        ]]

        when:
        def result = service.syncConfigs(Optional.of("cluster"), true)

        then:
        result.size() == 1
        result[0].configDiffs()*.key() == [TopicConfig.RETENTION_MS_CONFIG]
        0 * cluster.updateTopicConfig(_, _)
    }

    def "should update only the mapped Kafka topic that drifted"() {
        given:
        def topic = topic("group.topic", 1)
        def primary = kafkaTopic("group.topic_avro")
        def secondary = kafkaTopic("group.topic")
        topicManagement.getAllTopics() >> [topic]
        cluster.toKafkaTopics(topic) >> new KafkaTopics(primary, secondary)
        cluster.listTopicNames() >> ([primary.name().asString(), secondary.name().asString()] as Set)
        cluster.readTopicConfigs(_ as Collection) >> [
                (primary): desired(topic) + [(TopicConfig.RETENTION_MS_CONFIG): "2"],
                (secondary): desired(topic)
        ]

        when:
        def result = service.syncConfigs(Optional.of("cluster"), false)

        then:
        result*.kafkaTopicName() == [primary.name().asString()]
        1 * cluster.updateTopicConfig(primary, desired(topic))
        0 * cluster.updateTopicConfig(secondary, _)
    }

    def "should not touch other clusters when syncing a single cluster"() {
        given:
        def otherCluster = Mock(BrokersClusterService)
        otherCluster.getClusterName() >> "otherCluster"
        otherCluster.getDatacenter() >> "dc"
        multiDCAwareService.getClusters() >> [cluster, otherCluster]
        multiDCAwareService.getCluster("otherCluster") >> otherCluster

        def topic = topic("group.topic", 1)
        def kafkaTopic = kafkaTopic("group.topic")
        topicManagement.getAllTopics() >> [topic]
        cluster.toKafkaTopics(topic) >> new KafkaTopics(kafkaTopic)
        cluster.listTopicNames() >> (["group.topic"] as Set)
        cluster.readTopicConfigs(_ as Collection) >> [
                (kafkaTopic): desired(topic) + [(TopicConfig.RETENTION_MS_CONFIG): "2"]
        ]

        when:
        service.syncConfigs(Optional.of("cluster"), false)

        then: "only the targeted cluster is inspected and written"
        1 * cluster.updateTopicConfig(kafkaTopic, desired(topic))

        and: "the other registered cluster is never inspected or modified"
        0 * otherCluster.listTopicNames()
        0 * otherCluster.readTopicConfigs(_)
        0 * otherCluster.updateTopicConfig(_, _)
        0 * otherCluster.createTopic(_, _)
    }

    def "should create only missing mapped topic during bootstrap"() {
        given:
        def topic = topic("group.topic", 1)
        def primary = kafkaTopic("group.topic_avro")
        def secondary = kafkaTopic("group.topic")
        topicManagement.getAllTopics() >> [topic]
        cluster.toKafkaTopics(topic) >> new KafkaTopics(primary, secondary)
        cluster.listTopicNames() >> ([primary.name().asString()] as Set)

        when:
        def result = service.bootstrapMissingTopics("cluster", false)

        then:
        result == [secondary.name().asString()]
        1 * cluster.createTopic(topic, secondary)
        0 * cluster.createTopic(topic, primary)
    }

    def "should require a partitions per datacenter mapping for bootstrap"() {
        given:
        topicProperties.setPartitionsPerDc([:])

        when:
        service.bootstrapMissingTopics("cluster", true)

        then:
        thrown(IllegalStateException)
        0 * cluster.listTopicNames()
    }

    def "should report Kafka topic config inconsistencies per cluster with periodic check"() {
        given:
        def topic = topic("group.topic", 1)
        def kafkaTopic = kafkaTopic("group.topic")
        topicManagement.getAllTopics() >> [topic]
        cluster.toKafkaTopics(topic) >> new KafkaTopics(kafkaTopic)
        cluster.listTopicNames() >> (["group.topic"] as Set)
        cluster.readTopicConfigs([kafkaTopic]) >> [(kafkaTopic): desired(topic) + [
                (TopicConfig.RETENTION_MS_CONFIG): "2"
        ]]
        service.stop()
        properties.setEnabled(true)
        properties.setPeriodicCheckEnabled(true)
        properties.setInitialRefreshDelay(Duration.ZERO)
        properties.setRefreshInterval(Duration.ofDays(1))
        service = new KafkaTopicConfigConsistencyService(
                topicManagement, multiDCAwareService, topicProperties,
                new TopicConfigDiffer(), properties, metricsFacade)

        expect:
        new PollingConditions(timeout: 10).eventually {
            meterRegistry.get("kafka-topic-config.inconsistencies")
                    .tag("cluster", "cluster")
                    .gauge()
                    .value() == 1.0d
        }
    }

    private def topic(String name, int retentionDays) {
        TopicBuilder.topic(name)
                .withRetentionTime(new RetentionTime(retentionDays, TimeUnit.DAYS))
                .build()
    }

    private static KafkaTopic kafkaTopic(String name) {
        new KafkaTopic(KafkaTopicName.valueOf(name), ContentType.AVRO)
    }

    private Map<String, String> desired(def topic) {
        Map<String, String> config = [:]
        config[TopicConfig.RETENTION_MS_CONFIG] = String.valueOf(topic.retentionTime.durationInMillis)
        config[TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG] = "false"
        config[TopicConfig.MAX_MESSAGE_BYTES_CONFIG] = "1048576"
        config
    }
}
