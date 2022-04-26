package pl.allegro.tech.hermes.consumers.consumer.offset

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

class ConsumerPartitionAssignmentStateTest extends Specification {

    @Shared
    ConsumerPartitionAssignmentState state = new ConsumerPartitionAssignmentState()

    def "should return -1 when no assignments happened"() {
        expect:
        state.currentTerm(subscriptionName('sub1')) == -1
    }

    def "should increment term when assigning partitions"() {
        given:
        def sub = subscriptionName('sub1')

        when:
        state.assign(sub, [1, 3])

        then:
        state.currentTerm(sub) == 0

        when:
        state.assign(sub, [1, 2, 3, 4])

        then:
        state.currentTerm(sub) == 1
        state.currentTerm(subscriptionName('sub2')) == -1  // not assigned yet
    }

    def "should check if partitions are assigned for current term"() {
        given:
        def sub1 = subscriptionName('sub1')
        def sub2 = subscriptionName('sub2')

        when:
        state.assign(sub1, [1, 2, 3])
        state.assign(sub2, [4, 5, 6])

        then:
        [1, 2, 3].forEach { partition ->
            state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, partition, 0))
            !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, partition, 1))
            !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub2, partition, 0))
        }
        [4, 5, 6].forEach { partition ->
            !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, partition, 0))
            state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub2, partition, 0))
            !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub2, partition, 1))
        }
        [7, 8, 9].forEach { partition ->
            !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, partition, 0))
            !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub2, partition, 0))
        }
    }

    def "should revoke partitions"() {
        given:
        def sub1 = subscriptionName('sub1')

        when:
        state.assign(sub1, [1, 2, 3])

        and:
        state.revoke(sub1, [1])

        then:
        !state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, 1, 0))
        [2, 3].forEach { partition ->
            state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, partition, 0))
        }

        when:
        state.revokeAll(sub1)

        then:
        [1, 2, 3].forEach { partition ->
            state.isAssignedPartitionAtCurrentTerm(subscriptionPartition(sub1, partition, 0))
        }
    }

    private SubscriptionName subscriptionName(String name) {
        return SubscriptionName.fromString("group.topic\$$name")
    }

    private static SubscriptionPartition subscriptionPartition(SubscriptionName subscription, partition, term) {
        new SubscriptionPartition(KafkaTopicName.valueOf('group_topic'), subscription, partition, term, 0L)
    }

}
