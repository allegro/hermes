package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ConsumerProcessTest extends Specification {

    private static final long UNHEALTHY_AFTER_MS = 2048;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ConsumerProcessWaiter waiter = new ConsumerProcessWaiter()

    private ConsumerStub consumer = new ConsumerStub()

    private Retransmitter retransmitter = Mock(Retransmitter)

    private Subscription subscription = SubscriptionBuilder
            .subscription(SubscriptionName.fromString('group.topic$sub')).build()

    private ConsumerProcess process = new ConsumerProcess(
            Signal.of(Signal.SignalType.START, subscription.qualifiedName, subscription),
            consumer,
            retransmitter,
            { a -> shutdownRun = true },
            Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault()),
            UNHEALTHY_AFTER_MS
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
        long unhealthyAfter = -1
        ConsumerProcess process = new ConsumerProcess(
                Signal.of(Signal.SignalType.START, subscription.qualifiedName, subscription),
                consumer,
                retransmitter,
                { a -> shutdownRun = true },
                Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault()),
                unhealthyAfter)
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        !process.isHealthy()
        process.lastSeen() > unhealthyAfter
    }

    def "should tear down and initialize consumer on restart but not call shutdown hook"() {
        given:
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.RESTART, subscription.qualifiedName))
        waiter.waitForSignalProcessing()

        then:
        consumer.tearDownCount == 1
        consumer.initializationCount == 2

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()
        !shutdownRun
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
        consumer.modifiedSubscription == modifiedSubscription

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription.qualifiedName))
        waiter.waitForSignalProcessing()
    }
}
