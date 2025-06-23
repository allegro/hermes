package pl.allegro.tech.hermes.consumers.consumer.result

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.TrackingMode
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.tracker.consumers.Trackers
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class DefaultSuccessHandlerTest extends Specification {


    private InMemoryLogRepository sendingTracker = new InMemoryLogRepository()

    private Trackers trackers = new Trackers([sendingTracker])

    private Subscription subscription = subscription('group.topic', 'subscription')
            .withTrackingMode(TrackingMode.TRACK_ALL).build()

    private DefaultSuccessHandler handler = new DefaultSuccessHandler(Stub(MetricsFacade), trackers, subscription.qualifiedName, subscription.getMetricsConfig())

    def "should save tracking information on message success"() {
        given:
        Message message = MessageBuilder.withTestMessage().withPartitionOffset('kafka_topic', 0, 123L).build()
        MessageSendingResult result = MessageSendingResult.failedResult(500)

        when:
        handler.handleSuccess(message, subscription, result)

        then:
        sendingTracker.hasSuccessfulLog('kafka_topic', 0, 123L)
    }
}
