package pl.allegro.tech.hermes.consumers.supervisor.process

import com.codahale.metrics.MetricRegistry
import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.core.ConditionFactory
import pl.allegro.tech.hermes.api.DeliveryType
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.config.Configs
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.function.Consumer

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.COMMIT
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.RETRANSMIT
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.STOP
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_SUBSCRIPTION
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_TOPIC
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust

class ConsumerProcessSupervisorTest extends Specification {

    Topic topic1 = TopicBuilder.topic('group.topic').build()
    Subscription subscription1 = subscription(topic1.getQualifiedName(), 'sub1').build()
    SubscriptionName subscriptionName = subscription1.getQualifiedName()

    def signals = [
            start             : Signal.of(START, subscriptionName, subscription1),
            stop              : Signal.of(STOP, subscriptionName),
            retransmit        : Signal.of(RETRANSMIT, subscriptionName),
            updateSubscription: Signal.of(UPDATE_SUBSCRIPTION, subscriptionName, subscription1),
            updateTopic       : Signal.of(UPDATE_TOPIC, subscriptionName, topic1),
            commit            : Signal.of(COMMIT, subscriptionName)
    ]

    ConsumerProcessSupervisor supervisor
    HermesMetrics metrics
    ConsumerStub consumer

    Clock clock
    long currentTime = 1

    long unhealthyAfter = 3000
    int killAfter = 100

    def setup() {
        clock = new CurrentTimeClock()
        consumer = new ConsumerStub(subscription1)
        MutableConfigFactory configFactory = new MutableConfigFactory()
        ConsumerProcessSupplier processFactory = {
            Subscription subscription, Signal startSignal, Consumer<SubscriptionName> onConsumerStopped ->
                return new ConsumerProcess(startSignal, consumer, Stub(Retransmitter), clock, unhealthyAfter, onConsumerStopped)
        }

        configFactory.overrideProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER, killAfter)
        metrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"))

        supervisor = new ConsumerProcessSupervisor(
                new ConsumersExecutorService(configFactory, metrics),
                clock,
                metrics,
                configFactory,
                processFactory)
    }

    def cleanup() {
        supervisor.shutdown()
    }

    def "should spawn consumer process for START signal"() {
        when:
        runAndWait(supervisor.accept(signals.start))

        then:
        supervisor.existingConsumers().contains(subscriptionName)
    }

    def "should shutdown consumer process for STOP signal"() {
        given:
        runAndWait(supervisor.accept(signals.start))

        when:
        runAndWait(supervisor.accept(signals.stop))

        then:
        supervisor.countRunningProcesses() == 0
    }

    def "should restart unhealthy process"() {
        given:
        runAndWait(supervisor.accept(signals.start))

        when:
        consumer.whenUnhealthy {
            currentTime += unhealthyAfter
            runAndWait(supervisor)
        }

        then:
        await().until {
            supervisor.run()
            assert !supervisor.runningSubscriptionsStatus().isEmpty()
            assert supervisor.runningSubscriptionsStatus().first().signalTimesheet[signals.start.type] == currentTime
        }
    }

    def "should eventually try to kill process that is not stopping correctly"() {
        given:
        runAndWait(supervisor.accept(signals.start))

        when:
        consumer.whenBlockedOnTeardown {
            runAndWait(supervisor.accept(signals.stop))
            currentTime += 2 * killAfter
            runAndWait(supervisor)
        }

        then:
        await().until {
            assert consumer.tearDownCount > 0
            assert consumer.wasInterrupted
        }
    }

    def "should pass signals to running process"() {
        given:
        runAndWait(supervisor.accept(signals.start))
        def signalsToPass = signals.values() - signals.start - signals.stop

        when:
        signalsToPass.forEach { supervisor.accept(it) }
        runAndWait(supervisor)

        then:
        await().until {
            assert !supervisor.runningSubscriptionsStatus().isEmpty()
            signalsToPass.forEach {
                assert supervisor.runningSubscriptionsStatus().first().signalTimesheet[it.type] == currentTime
            }
        }
    }

    def "should drop signals when no running process was found"() {
        given:
        runAndWait(supervisor.accept(signals.start))
        def signalsToDrop = signals.values() - signals.start

        when:
        consumer.whenBlockedOnTeardown {
            runAndWait(supervisor.accept(signals.stop))
            signalsToDrop.forEach { supervisor.accept(it) }
            runAndWait(supervisor)
        }

        then:
        signalsToDrop.forEach {
            assert metrics.counter("supervisor.signal.dropped." + it.type.name()).getCount() == 1
        }
    }

    @Unroll
    def "should stop consumer process when subscription delivery type is changed"() {
        given:
        Subscription existingSubscription = subscription(topic1.getQualifiedName(), 'sub1')
                .withDeliveryType(existingDeliveryType)
                .build()
        consumer.updateSubscription(existingSubscription)

        runAndWait(supervisor.accept(signals.start))

        Subscription subscriptionWithNewDeliveryType = subscription(topic1.getQualifiedName(), 'sub1')
                .withDeliveryType(newDeliveryType)
                .build()

        when:
        runAndWait(supervisor.accept(Signal.of(UPDATE_SUBSCRIPTION, existingSubscription.getQualifiedName(), subscriptionWithNewDeliveryType)))

        then:
        supervisor.countRunningProcesses() == 0

        where:
        existingDeliveryType | newDeliveryType
        DeliveryType.SERIAL  | DeliveryType.BATCH
        DeliveryType.BATCH   | DeliveryType.SERIAL

    }


    private static runAndWait(ConsumerProcessSupervisor supervisor) {
        supervisor.run()
        // this helps to pass tests consistently on CI
        Thread.sleep(adjust(30))
    }

    private static ConditionFactory await() {
        Awaitility.await().pollInterval(adjust(50), MILLISECONDS).atMost(adjust(500), MILLISECONDS)
    }

    class CurrentTimeClock extends Clock {
        ZoneId getZone() { return null }

        Clock withZone(ZoneId zone) { return null }

        Instant instant() { return Instant.ofEpochMilli(currentTime) }
    }
}
