package pl.allegro.tech.hermes.management.infrastructure.monitoring

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ConsumerGroupDescription
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult
import org.apache.kafka.clients.admin.DescribeTopicsResult
import org.apache.kafka.clients.admin.MemberAssignment
import org.apache.kafka.clients.admin.MemberDescription
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.ConsumerGroupState
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.TopicPartitionInfo
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.KafkaTopic
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopics
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification
import spock.lang.Unroll

class MonitoringServiceTest extends Specification {


    private KafkaNamesMapper kafkaNamesMapper = Mock()
    private AdminClient adminClient = Mock()
    private DescribeConsumerGroupsResult describeConsumerGroupsResult = Mock()
    private ConsumerGroupDescription consumerGroupDescription = Mock()
    private KafkaFuture kafkaFuture = Mock()

    private DescribeTopicsResult describeTopicsResult = Mock()
    private TopicDescription topicDescription = Mock()
    private TopicPartitionInfo topicPartitionInfo = Mock()

    private MemberDescription memberDescription = Mock()
    private MemberAssignment memberAssignment = Mock()

    TopicName topicName
    Topic topic
    String subscriptionName = "subscription"


    void setup() {
        topicName = new TopicName("group", "topic")
        topic = TopicBuilder.topic(topicName).build()

        kafkaNamesMapper.toKafkaTopics(topic) >> new KafkaTopics(new KafkaTopic(new KafkaTopicName(topicName.qualifiedName()), ContentType.AVRO))
        kafkaNamesMapper.toConsumerGroupId(new SubscriptionName(subscriptionName, topic.getName())) >> new ConsumerGroupId("consumerGroup")

        adminClient.describeConsumerGroups(_) >> describeConsumerGroupsResult
        describeConsumerGroupsResult.all() >> kafkaFuture

        adminClient.describeTopics(_) >> describeTopicsResult
        describeTopicsResult.all() >> kafkaFuture
        kafkaFuture.thenApply(_) >> kafkaFuture

        consumerGroupDescription.members() >> List.of(memberDescription)
        memberDescription.assignment() >> memberAssignment

        kafkaFuture.get() >>> [Map.of("", consumerGroupDescription), topicDescription]
    }

    def "Should return true as all partitions are assigned and consumer group is STABLE"() {
        given:
        consumerGroupDescription.state() >> ConsumerGroupState.STABLE
        topicDescription.partitions() >> List.of(topicPartitionInfo) //topic partitions - 1
        memberAssignment.topicPartitions() >> List.of(new TopicPartition("topic", 0)) //partitions in consumer groups - 1
        def monitoringService = new MonitoringService(kafkaNamesMapper, adminClient, "clusterName")

        when:
        def result = monitoringService.checkIfAllPartitionsAreAssigned(topic, subscriptionName)

        then:
        result
    }

    def "Should return false as not all partitions are assigned"() {
        given:
        consumerGroupDescription.state() >> ConsumerGroupState.STABLE
        topicDescription.partitions() >> List.of(topicPartitionInfo, topicPartitionInfo) //topic partitions - 2
        memberAssignment.topicPartitions() >> List.of(new TopicPartition("topic", 0)) //partitions in consumer groups - 1
        def monitoringService = new MonitoringService(kafkaNamesMapper, adminClient, "clusterName")

        when:
        def result = monitoringService.checkIfAllPartitionsAreAssigned(topic, subscriptionName)

        then:
        !result
    }

    @Unroll
    def "Should return true as consumer group is #state"() {
        given:
        consumerGroupDescription.state() >> state
        def monitoringService = new MonitoringService(kafkaNamesMapper, adminClient, "clusterName")

        when:
        def result = monitoringService.checkIfAllPartitionsAreAssigned(topic, subscriptionName)

        then:
        result

        where:
        state << [ConsumerGroupState.PREPARING_REBALANCE, ConsumerGroupState.COMPLETING_REBALANCE]
    }
}