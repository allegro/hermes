package pl.allegro.tech.hermes.consumers.consumer.result.undelivered

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import spock.lang.Specification

import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.withTestMessage
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription


class UndeliveredMessageHandlersSpec extends Specification {

    def "should deliver message to all message handlers"() {
        given:
        Message message = withTestMessage().build()

        Subscription subscription = subscription("a.b", "a").build()

        MessageSendingResult messageSendingResult = new MessageSendingResult(new Exception("asdfd"))

        UndeliveredMessageHandlers messageHandlers = new UndeliveredMessageHandlers()

        UndeliveredMessageHandler undeliveredMessageHandler1 = Mock()
        UndeliveredMessageHandler undeliveredMessageHandler2 = Mock()

        messageHandlers.addHandlers([undeliveredMessageHandler1, undeliveredMessageHandler2])

        when:
        messageHandlers.handleDiscarded(message, subscription, messageSendingResult)

        then:
        1 * undeliveredMessageHandler1.handleDiscarded(message, subscription, messageSendingResult)
        1 * undeliveredMessageHandler2.handleDiscarded(message, subscription, messageSendingResult)
    }
}