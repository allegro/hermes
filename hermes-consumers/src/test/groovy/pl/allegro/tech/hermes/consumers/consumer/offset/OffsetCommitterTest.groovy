package pl.allegro.tech.hermes.consumers.consumer.offset

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

class OffsetCommitterTest extends Specification {

    @Shared
    SubscriptionName SUBSCRIPTION_NAME = SubscriptionName.fromString('group.topic$sub')

    @Shared
    KafkaTopicName KAFKA_TOPIC_NAME = KafkaTopicName.valueOf("group_topic")

    private OffsetQueue queue = new OffsetQueue(
            new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")),
            200_000
    )

    private MockMessageCommitter messageCommitter = new MockMessageCommitter()

    private ConsumerPartitionAssignmentState state

    private OffsetCommitter committer

    def setup() {
        state = new ConsumerPartitionAssignmentState()
        def commitInterval = 10
        committer = new OffsetCommitter(queue, state, messageCommitter, commitInterval,
                new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")))
    }

    def "should not commit offsets with negative values"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, -123))
        queue.offerCommittedOffset(offset(1, -123))

        when:
        committer.run()

        then:
        messageCommitter.nothingCommitted(1)
    }

    def "should not commit offset with long max value"() {
        given:
        assignPartitions(1)
        def offsetTooLarge = Long.MAX_VALUE - 1 // we actually commit the offset we want to read next, so it'll be +1
        queue.offerInflightOffset(offset(1, offsetTooLarge))
        queue.offerCommittedOffset(offset(1, offsetTooLarge))

        when:
        committer.run()

        then:
        messageCommitter.nothingCommitted(1)
    }

    def "should commit smallest offset of uncommitted message"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 1))
        queue.offerInflightOffset(offset(1, 2))
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))

        queue.offerCommittedOffset(offset(1, 1))
        queue.offerCommittedOffset(offset(1, 4))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 2))
    }

    def "should increment offset by 1 only if it comes from committed offsets to match Kafka offset definition"() {
        given:
        assignPartitions(1, 2)
        queue.offerInflightOffset(offset(1, 1))
        queue.offerCommittedOffset(offset(1, 1))

        queue.offerInflightOffset(offset(2, 1))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 2), offset(2, 1))
    }

    def "should commit max offset of committed offsets when no smaller inflights exist"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))

        queue.offerCommittedOffset(offset(1, 3))
        queue.offerCommittedOffset(offset(1, 4))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 5))
    }

    def "should commit same offset twice when there are no new offsets to commit"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 5))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 5))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(2, offset(1, 5))
    }

    def "should not mix offsets from different partitions and topics"() {
        given:
        assignPartitions(1, 2)

        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))

        queue.offerInflightOffset(offset(2, 10))
        queue.offerInflightOffset(offset(2, 11))

        queue.offerCommittedOffset(offset(1, 3))
        queue.offerCommittedOffset(offset(1, 4))
        queue.offerCommittedOffset(offset(2, 11))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 5), offset(2, 10))
    }

    def "should get rid of leftover inflight offsets when revoked from topic partitions"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))

        when:
        revokeAllPartitions()
        committer.run()

        then:
        messageCommitter.nothingCommitted(1)
    }

    def "should get rid of inflight offsets from revoked partitions"() {
        given:
        assignPartitions(1, 2)
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(2, 3))

        when:
        revokePartitions(1)
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(2, 3))
    }

    def "should get rid of committed offsets from revoked partitions"() {
        given:
        assignPartitions(1)
        queue.offerCommittedOffset(offset(1, 3))

        when:
        revokePartitions(1)
        committer.run()

        then:
        messageCommitter.nothingCommitted(1)
    }

    def "should get rid of leftover committed offsets when revoked from topic partitions"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))
        queue.offerCommittedOffset(offset(1, 3))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 4))

        when:
        queue.offerInflightOffset(offset(1, 4))
        queue.offerCommittedOffset(offset(1, 4))

        and:
        revokeAllPartitions()

        and:
        committer.run()

        then:
        messageCommitter.nothingCommitted(2)
    }

    def "should not commit offsets in next iteration after reassigning partition"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 3))

        when:
        revokePartitions(1)
        committer.run()

        then:
        messageCommitter.nothingCommitted(2)

        when:
        assignPartitions(1)
        committer.run()

        then:
        messageCommitter.nothingCommitted(3)
    }

    def "should commit only offsets from current term"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 3))

        when:
        queue.offerCommittedOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))
        queue.offerCommittedOffset(offset(1, 4))
        revokePartitions(1)
        assignPartitions(1)

        and:
        committer.run()

        then:
        messageCommitter.nothingCommitted(2)

        when:
        queue.offerCommittedOffset(offsetFromTerm(1, 4, 0)) // message from previous term=0

        and:
        committer.run()

        then:
        messageCommitter.nothingCommitted(3)
    }

    def "should commit maximum commited offset no matter what order committed offset return"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))
        queue.offerInflightOffset(offset(1, 5))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 3))

        when:
        queue.offerCommittedOffset(offset(1, 4))
        queue.offerCommittedOffset(offset(1, 5))

        and:
        committer.run()

        then:
        messageCommitter.wereCommitted(2, offset(1, 3))

        when:
        queue.offerCommittedOffset(offset(1, 3))

        and:
        committer.run()

        then:
        messageCommitter.wereCommitted(3, offset(1, 6))
    }

    def "should drop maximum committed offset when lost partition assignment"() {
        given:
        assignPartitions(1)
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))
        queue.offerInflightOffset(offset(1, 5))

        when:
        committer.run()

        then:
        messageCommitter.wereCommitted(1, offset(1, 3))

        when:
        queue.offerCommittedOffset(offset(1, 4))
        queue.offerCommittedOffset(offset(1, 5))

        and:
        committer.run()

        then:
        messageCommitter.wereCommitted(2, offset(1, 3))

        when:
        queue.offerCommittedOffset(offset(1, 3))
        revokePartitions(1)
        assignPartitions(1)

        and:
        committer.run()

        then:
        messageCommitter.nothingCommitted(3)
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
