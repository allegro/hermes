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

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ConsumerProcessWaiter waiter = new ConsumerProcessWaiter()

    private ConsumerStub consumer = new ConsumerStub()

    private Retransmitter retransmitter = Mock(Retransmitter)

    private SubscriptionName subscription = SubscriptionName.fromString('group.topic$sub')

    private ConsumerProcess process = new ConsumerProcess(
            subscription,
            consumer,
            retransmitter,
            { a -> shutdownRun = true },
            Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault())
    )

    private boolean shutdownRun = false

    def "should run main loop till stop signal sent"() {
        when:
        Future processFuture = executor.submit(process)
        process.accept(Signal.of(Signal.SignalType.STOP, subscription))
        waiter.waitForSignalProcessing()

        then:
        processFuture.done
        consumer.consumptionStarted
        consumer.initialized
        consumer.tornDown
    }

    def "should run shutdown callback on Consumer stop"() {
        when:
        executor.submit(process)
        process.accept(Signal.of(Signal.SignalType.STOP, subscription))
        waiter.waitForSignalProcessing()

        then:
        shutdownRun
    }

    def "should refresh healthcheck when signals are processed"() {
        given:
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription))
        waiter.waitForSignalProcessing()

        then:
        process.healtcheckRefreshTime() == 1024
    }

    def "should tear down and initialize consumer on restart but not call shutdown hook"() {
        given:
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.RESTART, subscription))
        waiter.waitForSignalProcessing()

        then:
        consumer.tearDownCount == 1
        consumer.initializationCount == 2

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription))
        waiter.waitForSignalProcessing()
        !shutdownRun
    }

    def "should stop, retransmit and start consumer on retransmission"() {
        given:
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.RETRANSMIT, subscription))
        waiter.waitForSignalProcessing()

        then:
        1 * retransmitter.reloadOffsets(_,_)

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription))
        waiter.waitForSignalProcessing()
    }

    def "should update subscription"() {
        given:
        Subscription modifiedSubscription = SubscriptionBuilder.subscription("group.topic", "sub1").build()
        executor.submit(process)

        when:
        process.accept(Signal.of(Signal.SignalType.UPDATE_SUBSCRIPTION, subscription, modifiedSubscription))
        waiter.waitForSignalProcessing()

        then:
        consumer.updated
        consumer.modifiedSubscription == modifiedSubscription

        cleanup:
        process.accept(Signal.of(Signal.SignalType.STOP, subscription))
        waiter.waitForSignalProcessing()
    }
}
