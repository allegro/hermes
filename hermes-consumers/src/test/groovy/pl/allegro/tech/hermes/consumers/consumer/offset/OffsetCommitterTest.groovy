package pl.allegro.tech.hermes.consumers.consumer.offset

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.test.helper.metrics.TestMetricsFacadeFactory
import spock.lang.Shared
import spock.lang.Specification

class OffsetCommitterTest extends Specification {

    @Shared
    SubscriptionName SUBSCRIPTION_NAME = SubscriptionName.fromString('group.topic$sub')

    @Shared
    KafkaTopicName KAFKA_TOPIC_NAME = KafkaTopicName.valueOf("group_topic")

    private PendingOffsets offsetsSlots = new PendingOffsets(SUBSCRIPTION_NAME, TestMetricsFacadeFactory.create(), 50, 2000)

    private OffsetCommitterTestHelper offsetCommitterTestHelper = new OffsetCommitterTestHelper()

    private ConsumerPartitionAssignmentState state

    private OffsetCommitter committer

    def setup() {
        state = new ConsumerPartitionAssignmentState()

        committer = new OffsetCommitter(state, TestMetricsFacadeFactory.create())
    }

    def "should not commit offsets with negative values"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, -123))
        offsetsSlots.markAsProcessed(offset(1, -123))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(1)
    }

    def "should not commit offset with long max value"() {
        given:
        assignPartitions(1)
        def offsetTooLarge = Long.MAX_VALUE - 1 // we actually commit the offset we want to read next, so it'll be +1
        offsetsSlots.markAsInflight(offset(1, offsetTooLarge))
        offsetsSlots.markAsProcessed(offset(1, offsetTooLarge))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(1)
    }

    def "should commit smallest offset of uncommitted message"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 1))
        offsetsSlots.markAsInflight(offset(1, 2))
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsInflight(offset(1, 4))

        offsetsSlots.markAsProcessed(offset(1, 1))
        offsetsSlots.markAsProcessed(offset(1, 4))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 2))
    }

    def "should increment offset by 1 only if it comes from committed offsets to match Kafka offset definition"() {
        given:
        assignPartitions(1, 2)
        offsetsSlots.markAsInflight(offset(1, 1))
        offsetsSlots.markAsProcessed(offset(1, 1))

        offsetsSlots.markAsInflight(offset(2, 1))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 2), offset(2, 1))
    }

    def "should commit max offset of committed offsets when no smaller inflights exist"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsInflight(offset(1, 4))

        offsetsSlots.markAsProcessed(offset(1, 3))
        offsetsSlots.markAsProcessed(offset(1, 4))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 5))
    }

    def "should commit same offset twice when there are no new offsets to commit"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 5))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 5))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(2, offset(1, 5))
    }

    def "should not mix offsets from different partitions and topics"() {
        given:
        assignPartitions(1, 2)

        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsInflight(offset(1, 4))

        offsetsSlots.markAsInflight(offset(2, 10))
        offsetsSlots.markAsInflight(offset(2, 11))

        offsetsSlots.markAsProcessed(offset(1, 3))
        offsetsSlots.markAsProcessed(offset(1, 4))
        offsetsSlots.markAsProcessed(offset(2, 11))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 5), offset(2, 10))
    }

    def "should get rid of leftover inflight offsets when revoked from topic partitions"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))

        when:
        revokeAllPartitions()
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(1)
    }

    def "should get rid of inflight offsets from revoked partitions"() {
        given:
        assignPartitions(1, 2)
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsInflight(offset(2, 3))

        when:
        revokePartitions(1)
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(2, 3))
    }

    def "should get rid of committed offsets from revoked partitions"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsProcessed(offset(1, 3))

        when:
        revokePartitions(1)
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(1)
    }

    def "should get rid of leftover committed offsets when revoked from topic partitions"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsProcessed(offset(1, 3))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 4))

        when:
        offsetsSlots.markAsInflight(offset(1, 4))
        offsetsSlots.markAsProcessed(offset(1, 4))

        and:
        revokeAllPartitions()

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(2)
    }

    def "should not commit offsets in next iteration after reassigning partition"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 3))

        when:
        revokePartitions(1)
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(2)

        when:
        assignPartitions(1)
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(3)
    }

    def "should commit only offsets from current term"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 3))

        when:
        offsetsSlots.markAsProcessed(offset(1, 3))
        offsetsSlots.markAsInflight(offset(1, 4))
        offsetsSlots.markAsProcessed(offset(1, 4))
        revokePartitions(1)
        assignPartitions(1)

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(2)

        when:
        offsetsSlots.markAsProcessed(offsetFromTerm(1, 4, 0)) // message from previous term=0

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(3)
    }

    def "should commit maximum commited offset no matter what order committed offset return"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsInflight(offset(1, 4))
        offsetsSlots.markAsInflight(offset(1, 5))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 3))

        when:
        offsetsSlots.markAsProcessed(offset(1, 4))
        offsetsSlots.markAsProcessed(offset(1, 5))

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(2, offset(1, 3))

        when:
        offsetsSlots.markAsProcessed(offset(1, 3))

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(3, offset(1, 6))
    }

    def "should drop maximum committed offset when lost partition assignment"() {
        given:
        assignPartitions(1)
        offsetsSlots.markAsInflight(offset(1, 3))
        offsetsSlots.markAsInflight(offset(1, 4))
        offsetsSlots.markAsInflight(offset(1, 5))

        when:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(1, offset(1, 3))

        when:
        offsetsSlots.markAsProcessed(offset(1, 4))
        offsetsSlots.markAsProcessed(offset(1, 5))

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.wereCommitted(2, offset(1, 3))

        when:
        offsetsSlots.markAsProcessed(offset(1, 3))
        revokePartitions(1)
        assignPartitions(1)

        and:
        offsetCommitterTestHelper.markCommittedOffsets(committer.calculateOffsetsToBeCommitted(offsetsSlots.getOffsetsSnapshotAndReleaseProcessedSlots()))

        then:
        offsetCommitterTestHelper.nothingCommitted(3)
    }

    private SubscriptionPartitionOffset offset(int partition, long offset) {
        offsetFromTerm(partition, offset, state.currentTerm(SUBSCRIPTION_NAME))
    }

    private SubscriptionPartitionOffset offsetFromTerm(int partition, long offset, long term) {
        def partitionOffset = new SubscriptionPartition(KAFKA_TOPIC_NAME, SUBSCRIPTION_NAME, partition, term)
        return new SubscriptionPartitionOffset(partitionOffset, offset)
    }

    private assignPartitions(int... partitions) {
        state.assign(SUBSCRIPTION_NAME, [*partitions])
    }

    private revokePartitions(int... partitions) {
        state.revoke(SUBSCRIPTION_NAME, [*partitions])
    }

    private revokeAllPartitions() {
        state.revokeAll(SUBSCRIPTION_NAME)
    }
}
