package pl.allegro.tech.hermes.consumers.consumer.sender

import pl.allegro.tech.hermes.consumers.consumer.rate.SerialConsumerRateLimiter
import pl.allegro.tech.hermes.consumers.consumer.sender.timeout.FutureAsyncTimeout
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate


class ThrottlingSendFutureProviderTest extends Specification {

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

    FutureAsyncTimeout futureAsyncTimeout = new FutureAsyncTimeout(Executors.newSingleThreadScheduledExecutor())
    Function<Exception, SingleMessageSendingResult> exceptionMapper = { e -> MessageSendingResult.failedResult(e) }

    def "should register successful send future in rate limiter"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }
        ThrottlingSendFutureProvider<SingleMessageSendingResult> futureProvider = new ThrottlingSendFutureProvider(
                serialConsumerRateLimiter,
                [],
                futureAsyncTimeout,
                1000,
                1000
        )

        when:
        CompletableFuture<MessageSendingResult> future = futureProvider.provide(successfulConsumer(), exceptionMapper)
        then:
        future.get().succeeded()

    }

    def "should asynchronously time out send future and register failed result in rate limiter"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ThrottlingSendFutureProvider<SingleMessageSendingResult> futureProvider = new ThrottlingSendFutureProvider(
                serialConsumerRateLimiter,
                [],
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture<MessageSendingResult> future = futureProvider.provide(slowConsumer(5_000), exceptionMapper)

        then:
        future.get().isTimeout()
    }

    def "should report failure when send result is not successful"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ThrottlingSendFutureProvider<SingleMessageSendingResult> futureProvider = new ThrottlingSendFutureProvider(
                serialConsumerRateLimiter,
                [],
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture<MessageSendingResult> future = futureProvider.provide(ordinarilyFailingConsumer(500), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should not report failure when future fails with ignorable error"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerSuccessfulSending()
        }
        Predicate<MessageSendingResult> ignorable = {m -> m.getStatusCode() == 404}
        ThrottlingSendFutureProvider<SingleMessageSendingResult> futureProvider = new ThrottlingSendFutureProvider(
                serialConsumerRateLimiter,
                [ignorable],
                futureAsyncTimeout,
                100,
                100
        )

        when:
        CompletableFuture<MessageSendingResult> future = futureProvider.provide(ordinarilyFailingConsumer(404), exceptionMapper)

        then:
        !future.get().succeeded()
    }

    def "should report failure when future completes exceptionally"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ThrottlingSendFutureProvider<SingleMessageSendingResult> futureProvider = new ThrottlingSendFutureProvider(
                serialConsumerRateLimiter,
                [],
                futureAsyncTimeout,
                100,
                100
        )

        when:
        def failWith = new IOException()
        def future = futureProvider.provide(abnormallyFailingConsumer(failWith), exceptionMapper)

        then:
        with(future.get()) {
            !succeeded()
            getFailure() == failWith
        }
    }

    def "should report failure when consumer throws exception"() {
        given:
        SerialConsumerRateLimiter serialConsumerRateLimiter = Mock(SerialConsumerRateLimiter) {
            1 * acquire()
            1 * registerFailedSending()
        }
        ThrottlingSendFutureProvider<SingleMessageSendingResult> futureProvider = new ThrottlingSendFutureProvider(
                serialConsumerRateLimiter,
                [],
                futureAsyncTimeout,
                100,
                100
        )

        when:
        def failWith = new IllegalStateException()
        def future = futureProvider.provide(abruptlyFailingConsumer(failWith), exceptionMapper)

        then:
        with(future.get()) {
            !succeeded()
            getFailure() == failWith
        }
    }


}
