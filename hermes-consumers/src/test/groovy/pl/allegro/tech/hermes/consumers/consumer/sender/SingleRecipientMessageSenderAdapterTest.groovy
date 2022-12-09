package pl.allegro.tech.hermes.consumers.consumer.sender

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender
import pl.allegro.tech.hermes.consumers.consumer.rate.ConsumerRateLimiter
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout
import spock.lang.Specification
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.testMessage

class SingleRecipientMessageSenderAdapterTest extends Specification {

    FutureAsyncTimeout futureAsyncTimeout = new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor())

    CompletableFutureAwareMessageSender successfulMessageSender = new CompletableFutureAwareMessageSender() {
        void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
            resultFuture.complete(MessageSendingResult.succeededResult())
        }

        void stop() {}
    }

    CompletableFutureAwareMessageSender failingMessageSender = new CompletableFutureAwareMessageSender() {
        void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
            resultFuture.completeExceptionally(new IllegalStateException())
        }

        void stop() {}
    }

    ResilientMessageSender rateLimitingMessageSender(ConsumerRateLimiter consumerRateLimiter) {
        Subscription subscription = subscription(SubscriptionName.fromString("group.topic\$subscription")).build()

        return new ResilientMessageSender(
                consumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                1000,
                1000,
        )
    }

    def "should register successful send in rate limiter"() {
        given:
        ConsumerRateLimiter consumerRateLimiter = Mock(ConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }
        ResilientMessageSender rateLimitingMessageSender = rateLimitingMessageSender(consumerRateLimiter)
        SingleRecipientMessageSenderAdapter adapter = new SingleRecipientMessageSenderAdapter(successfulMessageSender, rateLimitingMessageSender)

        when:
        CompletableFuture<MessageSendingResult> future = adapter.send(testMessage())

        then:
        future.get().succeeded()
    }

    def "should register unsuccessful send in rate limiter"() {
        given:
        ConsumerRateLimiter consumerRateLimiter = Mock(ConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ResilientMessageSender rateLimitingMessageSender = rateLimitingMessageSender(consumerRateLimiter)

        SingleRecipientMessageSenderAdapter adapter = new SingleRecipientMessageSenderAdapter(failingMessageSender, rateLimitingMessageSender)

        when:
        CompletableFuture<MessageSendingResult> future = adapter.send(testMessage())

        then:
        !future.get().succeeded()

    }

}
