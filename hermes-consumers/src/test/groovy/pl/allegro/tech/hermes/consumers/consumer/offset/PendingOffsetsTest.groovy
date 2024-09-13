package pl.allegro.tech.hermes.consumers.consumer.offset

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.test.helper.metrics.TestMetricsFacadeFactory
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

class PendingOffsetsTest extends Specification {

    @Shared
    SubscriptionName SUBSCRIPTION_NAME = SubscriptionName.fromString('group.topic$sub')

    @Shared
    KafkaTopicName KAFKA_TOPIC_NAME = KafkaTopicName.valueOf("group_topic")

    @Shared
    Duration ACQUIRE_DURATION = Duration.ofMillis(100)

    private MetricsFacade testMetricsFacade = TestMetricsFacadeFactory.create()

    private ConsumerPartitionAssignmentState state = new ConsumerPartitionAssignmentState()

    def "should not allow for more inflight offsets than allowed"() {
        given:
        PendingOffsets offsetsSlots = createOffsetsSlots(2, 10)
        2.times {offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)}

        when:
        boolean isAcquired = offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        then:
        !isAcquired
    }

    def "should not allow for more total offsets than allowed"() {
        given:
        PendingOffsets offsetsSlots = createOffsetsSlots(2, 3)
        2.times {offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)}
        offsetsSlots.markAsInflight(offset(1, 1))
        offsetsSlots.markAsInflight(offset(1, 2))
        offsetsSlots.markAsProcessed(offset(1, 1))
        offsetsSlots.markAsProcessed(offset(1, 2))
        offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        when:
        boolean isAcquired = offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        then:
        !isAcquired
    }

    def "should free inflight offsets"() {
        given:
        PendingOffsets offsetsSlots = createOffsetsSlots(1, 10)
        offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)
        offsetsSlots.markAsInflight(offset(1, 1))

        when:
        boolean isAcquired = offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        then:
        !isAcquired

        when:
        offsetsSlots.markAsProcessed(offset(1, 1))
        boolean isAcquiredAfterRelease = offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        then:
        isAcquiredAfterRelease
    }

    def "should free offsetQueue and return offset snapshot"() {
        given:
        Map<SubscriptionPartitionOffset, MessageState> expectedOffsetSnapshot = Map.of(offset(1, 1), MessageState.PROCESSED, offset(1, 2), MessageState.PROCESSED)
        PendingOffsets offsetsSlots = createOffsetsSlots(2, 2)
        2.times{offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)}
        offsetsSlots.markAsInflight(offset(1, 1))
        offsetsSlots.markAsInflight(offset(1, 2))
        offsetsSlots.markAsProcessed(offset(1, 1))
        offsetsSlots.markAsProcessed(offset(1, 2))

        when:
        boolean isAcquired = offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        then:
        !isAcquired

        when:
        Map<SubscriptionPartitionOffset,MessageState> offsetSnapshot = offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()
        boolean isAcquiredAfterRelease = offsetsSlots.tryAcquireSlot(ACQUIRE_DURATION)

        then:
        offsetSnapshot == expectedOffsetSnapshot
        isAcquiredAfterRelease
    }

    private PendingOffsets createOffsetsSlots(int inflightSize, int offsetQueueSize) {
        return new PendingOffsets(SUBSCRIPTION_NAME, testMetricsFacade, inflightSize, offsetQueueSize)
    }

    private SubscriptionPartitionOffset offset(int partition, long offset) {
        offsetFromTerm(partition, offset, state.currentTerm(SUBSCRIPTION_NAME))
    }

    private SubscriptionPartitionOffset offsetFromTerm(int partition, long offset, long term) {
        def partitionOffset = new SubscriptionPartition(KAFKA_TOPIC_NAME, SUBSCRIPTION_NAME, partition, term)
        return new SubscriptionPartitionOffset(partitionOffset, offset)
    }
}
