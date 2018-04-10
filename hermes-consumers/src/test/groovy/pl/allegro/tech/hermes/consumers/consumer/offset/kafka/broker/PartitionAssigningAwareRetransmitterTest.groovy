package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker

import spock.lang.Specification

import static pl.allegro.tech.hermes.api.SubscriptionName.fromString
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset


class PartitionAssigningAwareRetransmitterTest extends Specification {

    def topic = "group.topic"
    def subscriptionName = "$topic\$subscription"
    def offsetMover = Mock(PartitionAssigningAwareRetransmitter.OffsetMover)
    def offset = subscriptionPartitionOffset(topic, subscriptionName, 1, 1)

    def retransmitter = new PartitionAssigningAwareRetransmitter(fromString(subscriptionName), 10, offsetMover)

    def "should move offset when partitions are already assigned"() {
        when:
        retransmitter.moveOffsetOrSchedule(offset)

        then:
        1 * offsetMover.move(offset)
        retransmitter.isQueueEmpty()
    }

    def "should schedule retransmission if consumer does not have partitions assigned yet"() {
        when:
        retransmitter.moveOffsetOrSchedule(offset)

        then:
        1 * offsetMover.move(offset) >> { throw new PartitionNotAssignedException() }
        !retransmitter.isQueueEmpty()
    }

    def "should trigger scheduled retransmission when partitions are assigned"() {
        when:
        retransmitter.moveOffsetOrSchedule(offset)
        retransmitter.onPartitionsAssigned([])

        then:
        2 * offsetMover.move(offset) >> { throw new PartitionNotAssignedException() }
        retransmitter.isQueueEmpty()
    }

    def "should not move offset after rebalance if nothing was scheduled"() {
        when:
        retransmitter.onPartitionsAssigned([])

        then:
        0 * offsetMover.move(_)
    }
}