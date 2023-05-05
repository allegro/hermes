package pl.allegro.tech.hermes.consumers.consumer.result

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.TrackingMode
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.tracker.consumers.Trackers
import spock.lang.Specification

import java.time.Clock

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class DefaultErrorHandlerTest extends Specification {

    private OffsetQueue offsetQueue = new OffsetQueue(
            new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")),
            200_000
    )

    private UndeliveredMessageLog undeliveredLog = Mock(UndeliveredMessageLog)

    private InMemoryLogRepository sendingTracker = new InMemoryLogRepository()

    private Trackers trackers = new Trackers([sendingTracker])

    private Subscription subscription = subscription('group.topic', 'subscription')
            .withTrackingMode(TrackingMode.TRACK_ALL).build()

    private DefaultErrorHandler handler = new DefaultErrorHandler(
            offsetQueue, Stub(SubscriptionMetrics), undeliveredLog, Clock.systemUTC(), trackers, "cluster")

    def "should save tracking information on message failure but not commit message"() {
        given:
        Message message = MessageBuilder.withTestMessage().withPartitionOffset('kafka_topic', 0, 123L).build()
        MessageSendingResult result = MessageSendingResult.failedResult(500)

        when:
        handler.handleFailed(message, subscription, result)

        then:
        sendingTracker.hasFailedLog('kafka_topic', 0, 123L)
        offsetQueue.drainCommittedOffsets({ o -> assert !(o.partition == 0 && o.offset == 123L) })
    }

    def "should commit message and save tracking information on message discard"() {
        given:
        Message message = MessageBuilder.withTestMessage().withPartitionOffset('kafka_topic', 0, 123L).build()
        MessageSendingResult result = MessageSendingResult.failedResult(500)

        when:
        handler.handleDiscarded(message, subscription, result)

        then:
        sendingTracker.hasDiscardedLog('kafka_topic', 0, 123L)
        offsetQueue.drainCommittedOffsets({ o -> assert o.partition == 0 && o.offset == 123L })
    }
}
