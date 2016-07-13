package pl.allegro.tech.hermes.consumers.consumer.offset

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter
import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Specification

class OffsetCommitterTest extends Specification {

    private OffsetQueue queue = new OffsetQueue(new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")))

    private MessageCommitter messageCommitter = Mock(MessageCommitter)

    private OffsetCommitter committer = new OffsetCommitter(queue, [messageCommitter], 10, new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")))

    def "should commit smallest offset of uncommitted message - 1"() {
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
        1 * messageCommitter.commitOffset(offset(1, 1))
    }

    def "should commit max offset of inflight when no offsets found after committing"() {
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
        queue.offerInflightOffset(offset(1, 3))
        queue.offerInflightOffset(offset(1, 4))

        queue.offerCommittedOffset(offset(1, 3))

        when:
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 3))

        when:
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 3))
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
        1 * messageCommitter.commitOffset(offset(2, 9))
    }

    def "should get rid of leftover inflight offset commits when removing subscription on the second iteration"() {
        given:
        queue.offerInflightOffset(offset(1, 3))

        when:
        committer.removeUncommittedOffsets(SubscriptionName.fromString("group.topic\$sub"))
        committer.run()

        then:
        1 * messageCommitter.commitOffset(offset(1, 2))

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
