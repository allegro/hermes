package pl.allegro.tech.hermes.management.infrastructure.kafka.service

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ConsumerGroupDescription
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult
import org.apache.kafka.clients.admin.MemberAssignment
import org.apache.kafka.clients.admin.MemberDescription
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.ConsumerGroupState
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupIdNotFoundException
import org.apache.kafka.common.errors.TimeoutException
import org.apache.kafka.common.internals.KafkaFutureImpl
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper
import pl.allegro.tech.hermes.management.infrastructure.kafka.BrokersClusterCommunicationException
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

class ConsumerGroupsDescriberSpec extends Specification {
    def admin = Mock(AdminClient)
    def mapper = new JsonToAvroMigrationKafkaNamesMapper("", "_")
    def describer = new ConsumerGroupsDescriber(mapper, admin, null, "primary")
    Topic topic = TopicBuilder.topic("pl.allegro.test.Foo").build()
    def offsets = Mock(ListConsumerGroupOffsetsResult)

    def "missing group from #operation returns empty"() {
        given:
        admin.listConsumerGroupOffsets(_ as String) >> offsets
        offsets.partitionsToOffsetAndMetadata() >> (operation == "offsets" ? failed(new GroupIdNotFoundException("missing")) : KafkaFuture.completedFuture([:]))
        admin.describeConsumerGroups(_ as Collection) >> new DescribeConsumerGroupsResult([group: failed(new GroupIdNotFoundException("missing"))])

        expect:
        !describer.describeConsumerGroup(topic, "sub").present

        where:
        operation << ["offsets", "description"]
    }

    def "dead group from an older broker returns empty"() {
        given:
        def description = Mock(ConsumerGroupDescription)
        description.state() >> ConsumerGroupState.DEAD
        admin.listConsumerGroupOffsets(_ as String) >> offsets
        offsets.partitionsToOffsetAndMetadata() >> KafkaFuture.completedFuture([:])
        admin.describeConsumerGroups(_ as Collection) >> new DescribeConsumerGroupsResult([group: KafkaFuture.completedFuture(description)])

        expect:
        !describer.describeConsumerGroup(topic, "sub").present
    }

    def "existing group retains state, members and offsets"() {
        given:
        def partition = new TopicPartition(mapper.toKafkaTopics(topic).primary.name().asString(), 0)
        def description = Mock(ConsumerGroupDescription)
        def member = Mock(MemberDescription)
        def assignment = Mock(MemberAssignment)
        def checker = Mock(LogEndOffsetChecker)
        describer = new ConsumerGroupsDescriber(mapper, admin, checker, "primary")
        checker.check(partition) >> 42L
        description.state() >> ConsumerGroupState.STABLE
        description.groupId() >> "group"
        description.members() >> [member]
        member.consumerId() >> "consumer"
        member.clientId() >> "client"
        member.host() >> "localhost"
        member.assignment() >> assignment
        assignment.topicPartitions() >> ([partition] as Set)
        admin.listConsumerGroupOffsets(_ as String) >> offsets
        offsets.partitionsToOffsetAndMetadata() >> KafkaFuture.completedFuture([(partition): new OffsetAndMetadata(12L)])
        admin.describeConsumerGroups(_ as Collection) >> new DescribeConsumerGroupsResult([group: KafkaFuture.completedFuture(description)])

        when:
        def group = describer.describeConsumerGroup(topic, "sub").get()

        then:
        group.groupId == "group"
        group.stable
        group.members.size() == 1
        def groupMember = group.members.first()
        groupMember.consumerId == "consumer"
        groupMember.partitions.first().currentOffset == 12L
        groupMember.partitions.first().logEndOffset == 42L
    }

    def "communication failure from #operation is propagated"() {
        given:
        admin.listConsumerGroupOffsets(_ as String) >> offsets
        offsets.partitionsToOffsetAndMetadata() >> (operation == "offsets" ? failed(new TimeoutException("timeout")) : KafkaFuture.completedFuture([:]))
        admin.describeConsumerGroups(_ as Collection) >> new DescribeConsumerGroupsResult([group: failed(new TimeoutException("timeout"))])

        when:
        describer.describeConsumerGroup(topic, "sub")

        then:
        def e = thrown(BrokersClusterCommunicationException)
        e.cause.cause instanceof TimeoutException

        where:
        operation << ["offsets", "description"]
    }

    private static KafkaFuture failed(Exception e) {
        def future = new KafkaFutureImpl()
        future.completeExceptionally(e)
        future
    }
}
