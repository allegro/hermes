package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ConsumerProcessTest extends Specification {

    private static final Duration UNHEALTHY_AFTER_MS = Duration.ofMillis(2048)

    private ExecutorService executor = Executors.newSingleThreadExecutor()

    private ConsumerProcessWaiter waiter = new ConsumerProcessWaiter()

    private Retransmitter retransmitter = Mock(Retransmitter)

    private Subscription subscription = SubscriptionBuilder
            .subscription(SubscriptionName.fromString('group.topic$sub')).build()

    private ConsumerStub consumer = new ConsumerStub(subscription)

    private ConsumerProcess process = new ConsumerProcess(
            Signal.of(Signal.SignalType.START, subscription.qualifiedName, subscription),
            consumer,
            retransmitter,
            Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault()),
            UNHEALTHY_AFTER_MS,
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

    def "should refresh healthcheck when signals are processed"() {
        given:
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        process.healthcheckRefreshTime() == 1024
        process.isHealthy()
    }

    def "should process be unhealthy when last seen period is greater than unhealthy period"() {
        given:
        Duration unhealthyAfter = Duration.ofMillis(-1)
        ConsumerProcess process = new ConsumerProcess(
                Signal.of(Signal.SignalType.START, subscription.qualifiedName, subscription),
                consumer,
                retransmitter,
                Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault()),
                unhealthyAfter,
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
