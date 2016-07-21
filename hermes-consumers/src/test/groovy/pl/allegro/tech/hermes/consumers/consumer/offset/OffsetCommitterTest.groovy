package pl.allegro.tech.hermes.consumers.consumer.offset

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter
import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Specification

class OffsetCommitterTest extends Specification {

    private OffsetQueue queue = new OffsetQueue(
            new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")),
            new ConfigFactory()
    )

    private MessageCommitter messageCommitter = Mock(MessageCommitter)

    private OffsetCommitter committer = new OffsetCommitter(
            queue, [messageCommitter], 10, new HermesMetrics(new MetricRegistry(), new PathsCompiler("host"))
    )

    def "should commit smallest offset of uncommitted message"() {
        given:
        queue.offerInflightOffset(offset(1, 1))
        queue.offerInflightOffset(offset(1, 2))
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))

        queue.offerCommittedOffset(offset(1, 1))
        queue.offerCommittedOffset(offset(1, 4))

        when:
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 2))
    }

    def "should commit max offset of committed offsets when no smaller inflights exist"() {
        given:
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))

        queue.offerCommittedOffset(offset(1, 3))
        queue.offerCommittedOffset(offset(1, 4))

        when:
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 4))
    }

    def "should commit same offset twice when there are no new offsets to commit"() {
        given:
        queue.offerInflightOffset(offset(1, 5))

        when:
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 5))

        when:
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 5))
    }

    def "should not mix offsets from different partitions and topics"() {
        given:
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
        1 * messageCommitter.commitOffset(offset(1, 4))
        1 * messageCommitter.commitOffset(offset(2, 10))
    }

    def "should get rid of leftover inflight offset commits on second iteration when removing subscription"() {
        given:
        queue.offerInflightOffset(offset(1, 3))

        when:
        committer.removeUncommittedOffsets(SubscriptionName.fromString('group.topic$sub'))
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 3))

        when:
        committer.run()

        then:
        0 * messageCommitter.commitOffset(_)
    }

    def "should retry committing offsets that failed to commit on first try in next iteration"() {
        given:
        queue.offerInflightOffset(offset(1, 1))
        queue.offerCommittedOffset(offset(1, 1))

        when:
        committer.run()
        committer.run()

        then:
        2 * messageCommitter.commitOffset(offset(1, 1)) >> { throw new IllegalStateException() } >> {}
    }

    private SubscriptionPartitionOffset offset(int partition, long offset) {
        return SubscriptionPartitionOffset.subscriptionPartitionOffset("group_topic", "group.topic\$sub", partition, offset)
    }
}
