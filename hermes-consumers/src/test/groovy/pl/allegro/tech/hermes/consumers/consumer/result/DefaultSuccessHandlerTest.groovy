package pl.allegro.tech.hermes.consumers.consumer.result

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.offset.BetterOffsetQueue
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.tracker.consumers.Trackers
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class DefaultSuccessHandlerTest extends Specification {

    private BetterOffsetQueue offsetQueue = new BetterOffsetQueue(new HermesMetrics(new MetricRegistry(), new PathsCompiler("host")))

    private InMemoryLogRepository sendingTracker = new InMemoryLogRepository()

    private Trackers trackers = new Trackers([sendingTracker])

    private Subscription subscription = subscription('group.topic', 'subscription').withTrackingEnabled(true).build()

    private DefaultSuccessHandler handler = new DefaultSuccessHandler(
            offsetQueue, Stub(HermesMetrics), trackers
    )

    def "should commit message and save tracking information on message success"() {
        given:
        Message message = MessageBuilder.withTestMessage().withPartitionOffset('kafka_topic', 0, 123L).build()
        MessageSendingResult result = MessageSendingResult.failedResult(500)

        when:
        handler.handle(message, subscription, result)

        then:
        sendingTracker.hasSuccessfulLog('kafka_topic', 0, 123L)
        offsetQueue.drainCommittedOffsets({ o -> assert o.partition == 0 && o.offset == 123L })
    }

}
