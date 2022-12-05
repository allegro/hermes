package pl.allegro.tech.hermes.consumers.consumer

import com.google.common.base.Throwables
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.Function

import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static io.netty.handler.codec.http.HttpResponseStatus.TOO_MANY_REQUESTS;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription


class ResilientMessageSenderTest extends Specification {

    Consumer<CompletableFuture<MessageSendingResult>> successfulConsumer() {
        return { cf -> return cf.complete(MessageSendingResult.succeededResult())
        }
    }

    Consumer<CompletableFuture<MessageSendingResult>> slowConsumer(long completeAfterMs) {
        return { cf ->
            return cf.completeAsync(
                    {
                        sleep(completeAfterMs)
                        MessageSendingResult.succeededResult()
                    }
            )
        }
    }

    Consumer<CompletableFuture<MessageSendingResult>> ordinarilyFailingConsumer(int statusCode) {
        return { cf -> cf.complete(MessageSendingResult.failedResult(statusCode)) }
    }

    Consumer<CompletableFuture<MessageSendingResult>> abnormallyFailingConsumer(Exception e) {
        return { cf -> cf.completeExceptionally(e) }
    }

    Consumer<CompletableFuture<MessageSendingResult>> abruptlyFailingConsumer(Exception e) {
        return { cf -> throw e }
    }

    Consumer<CompletableFuture<MessageSendingResult>> consumer(MessageSendingResult messageSendingResult) {
        return {cf -> cf.complete(messageSendingResult)}
    }

    FutureAsyncTimeout futureAsyncTimeout = new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor())
    Function<Exception, SingleMessageSendingResult> exceptionMapper = { e -> MessageSendingResult.failedResult(e) }

    Subscription subscription = subscription(SubscriptionName.fromString("group.topic\$subscription")).build()


    def "should report successful sending"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }
        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                1000,
                1000
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(successfulConsumer(), exceptionMapper)

        then:
        future.get().succeeded()
    }

    def "should asynchronously time out send future and report failed sending"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(slowConsumer(5_000), exceptionMapper)

        then:
        future.get().isTimeout()
    }

    def "should treat 4xx response for subscription with no 4xx retry as success"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }

        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(ordinarilyFailingConsumer(404), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should report failed sending on error response other than 4xx for subscription with no 4xx retry"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }

        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(ordinarilyFailingConsumer(500), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should report failed sending on 4xx response for subscription with 4xx retry"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        def subscription = subscription(SubscriptionName.fromString("group.topic\$subscription"))
                .withSubscriptionPolicy(subscriptionPolicy().applyDefaults()
                        .withClientErrorRetry()
                        .build()).build()

        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(ordinarilyFailingConsumer(500), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should report successful sending on retry after"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }

        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(consumer(MessageSendingResult.retryAfter(100)), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should report failed sending on service unavailable without retry after"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }

        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(ordinarilyFailingConsumer(SERVICE_UNAVAILABLE.code()), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should not report failed sending on too many requests without retry after"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }

        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture future = rateLimitingMessageSender.send(ordinarilyFailingConsumer(TOO_MANY_REQUESTS.code()), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should report failed sending when future completes exceptionally"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        def failWith = new IOException()
        def future = rateLimitingMessageSender.send(abnormallyFailingConsumer(failWith), exceptionMapper)

        then:
        with(future.get()) {
            !succeeded()
            Throwables.getRootCause(getFailure()) == failWith
        }
    }

    def "should report failed sending when consumer throws exception"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ResilientMessageSender rateLimitingMessageSender = new ResilientMessageSender(
                serialConsumerRateLimiter,
                subscription,
                futureAsyncTimeout,
                100,
                100
        )

        when:
        def failWith = new IllegalStateException()
        def future = rateLimitingMessageSender.send(abruptlyFailingConsumer(failWith), exceptionMapper)

        then:
        with(future.get()) {
            !succeeded()
            Throwables.getRootCause(getFailure()) == failWith
        }
    }
}
