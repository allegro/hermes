package pl.allegro.tech.hermes.management.infrastructure.kafka.service

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ConsumerGroupDescription
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult
import org.apache.kafka.clients.admin.DescribeTopicsResult
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult
import org.apache.kafka.clients.admin.MemberAssignment
import org.apache.kafka.clients.admin.MemberDescription
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupIdNotFoundException
import org.apache.kafka.common.errors.TimeoutException
import org.apache.kafka.common.internals.KafkaFutureImpl
import org.junit.Rule
import org.springframework.boot.test.system.OutputCaptureRule
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper
import pl.allegro.tech.hermes.management.infrastructure.kafka.MovingSubscriptionOffsetsValidationException
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

class BrokersClusterConsumerGroupsSpec extends Specification {
    @Rule
    OutputCaptureRule output = new OutputCaptureRule()

    def admin = Mock(AdminClient)
    def mapper = new JsonToAvroMigrationKafkaNamesMapper("", "_")
    def topic = TopicBuilder.topic("pl.allegro.test.Foo").build()
    def service = new BrokersClusterService("dc", "primary", null, null, null, mapper, null, null, admin, null)

    def "offset validation reports a missing group"() {
        given:
        def offsets = Mock(ListConsumerGroupOffsetsResult)
        admin.listConsumerGroupOffsets(_ as String) >> offsets
        offsets.partitionsToOffsetAndMetadata() >> KafkaFuture.completedFuture([:])
        admin.describeConsumerGroups(_ as Collection) >> new DescribeConsumerGroupsResult([group: failed(new GroupIdNotFoundException("missing"))])
        def subscription = new pl.allegro.tech.hermes.api.SubscriptionName("sub", topic.name)

        when:
        service.validateIfOffsetsCanBeMovedByConsumers(topic, subscription)

        then:
        def e = thrown(MovingSubscriptionOffsetsValidationException)
        e.message == "No consumer group for subscription ${subscription.qualifiedName} exists."

        when:
        service.validateIfOffsetsCanBeMoved(topic, subscription)

        then:
        def e2 = thrown(MovingSubscriptionOffsetsValidationException)
        e2.message == e.message
    }

    def "assignment validation handles missing groups and communication errors"() {
        given:
        def first = Mock(Subscription)
        def second = Mock(Subscription)
        first.qualifiedName >> new pl.allegro.tech.hermes.api.SubscriptionName("first", topic.name)
        second.qualifiedName >> new pl.allegro.tech.hermes.api.SubscriptionName("second", topic.name)
        def topicResult = Mock(DescribeTopicsResult)
        def topicDescription = Mock(TopicDescription)
        admin.describeTopics(_ as Collection) >> topicResult
        topicResult.allTopicNames() >> KafkaFuture.completedFuture([topic: topicDescription])
        topicDescription.partitions() >> (1..partitionCount).collect { Mock(org.apache.kafka.common.TopicPartitionInfo) }
        def description = Mock(ConsumerGroupDescription)
        def member = Mock(MemberDescription)
        def assignment = Mock(MemberAssignment)
        description.members() >> [member]
        member.assignment() >> assignment
        assignment.topicPartitions() >> ([new TopicPartition("topic", 0), new TopicPartition("topic", 1)] as Set)
        admin.describeConsumerGroups(_ as Collection) >> new DescribeConsumerGroupsResult([
                existing: KafkaFuture.completedFuture(description),
                second: error == null ? KafkaFuture.completedFuture(description) : failed(error)
        ])

        expect:
        service.allSubscriptionsHaveConsumersAssigned(topic, [first, second]) == expected
        output.toString().contains("Failed to check assignments") == logged

        where:
        error                                     | partitionCount | expected | logged
        null                                      | 2              | true     | false
        null                                      | 1              | false    | false
        new GroupIdNotFoundException("missing")   | 1              | false    | false
        new TimeoutException("timeout")           | 1              | false    | true
    }

    private static KafkaFuture failed(Exception e) {
        def future = new KafkaFutureImpl()
        future.completeExceptionally(e)
        future
    }
}
