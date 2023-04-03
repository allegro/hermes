package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.time.ModifiableClock
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ConsumerProcessTest extends Specification {

    private static final Duration UNHEALTHY_AFTER_MS = Duration.ofMillis(2048)
    private static final Duration MAX_GRACEFUL_STOP_PERIOD = Duration.ofSeconds(15)

    private ExecutorService executor = Executors.newSingleThreadExecutor()

    private ConsumerProcessWaiter waiter = new ConsumerProcessWaiter()

    private Retransmitter retransmitter = Mock(Retransmitter)

    private Subscription subscription = SubscriptionBuilder
            .subscription(SubscriptionName.fromString('group.topic$sub')).build()

    private ConsumerStub consumer = new ConsumerStub(subscription)

    private ModifiableClock clock = new ModifiableClock()

    private ConsumerProcess process = new ConsumerProcess(
            Signal.of(Signal.SignalType.START, subscription.qualifiedName, subscription),
            consumer,
            retransmitter,
            clock,
            UNHEALTHY_AFTER_MS,
            MAX_GRACEFUL_STOP_PERIOD,
            { a -> shutdownRun = true }
    )

    private boolean shutdownRun = false

    def "should run main loop till stop signal sent"() {
        when:
        Future processFuture = executor.submit(process)
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        processFuture.done
        consumer.consumptionStarted
        consumer.initialized
        consumer.tearDown
    }

    def "should run shutdown callback on Consumer stop"() {
        when:
        executor.submit(process)
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        shutdownRun
    }

    def "should stop forcefully after graceful period"() {
        given:
        consumer.markAsNeverReadyToBeTornDown()

        when:
        Future processFuture = executor.submit(process)
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        !processFuture.done
        consumer.consumptionStarted
        consumer.initialized
        !consumer.tearDown

        when:
        clock.advance(MAX_GRACEFUL_STOP_PERIOD.plusSeconds(1))
        waiter.waitForSignalProcessing()

        then:
        processFuture.done
        consumer.tearDown
    }

    def "should refresh healthcheck when signals are processed"() {
        given:
        clock.advanceMinutes(5)
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        process.healthcheckRefreshTime() == clock.instant().toEpochMilli()
        process.isHealthy()
    }

    def "should process be unhealthy when last seen period is greater than unhealthy period"() {
        given:
        Duration unhealthyAfter = Duration.ofMillis(-1)
        ConsumerProcess process = new ConsumerProcess(
                Signal.of(Signal.SignalType.START, subscription.qualifiedName, subscription),
                consumer,
                retransmitter,
                clock,
                unhealthyAfter,
                Duration.ofMillis(100),
                { a -> shutdownRun = true })
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        !process.isHealthy()
        process.lastSeen() > unhealthyAfter.toMillis()
    }

    def "should stop, retransmit and start consumer on retransmission"() {
        given:
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.RETRANSMIT, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        1 * retransmitter.reloadOffsets(_,_)

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()
    }

    def "should update subscription"() {
        given:
        Subscription modifiedSubscription = SubscriptionBuilder.subscription("group.topic", "sub1").build()
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.UPDATE_SUBSCRIPTION, subscription.qualifiedName, modifiedSubscription))
        waiter.waitForSignalProcessing()

        then:
        consumer.updated
        consumer.subscription == modifiedSubscription

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()
    }
}
