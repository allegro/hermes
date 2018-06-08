package pl.allegro.tech.hermes.consumers.supervisor.process

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.config.Configs
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.Consumer
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import java.time.Clock

class ConsumerProcessSupervisorSignalsTest extends Specification {

    MutableConfigFactory configFactory = new MutableConfigFactory()
    Retransmitter retransmitter = Stub(Retransmitter)
    ConsumerFactory consumerFactory = Stub()

    Subscription subscription = SubscriptionBuilder
            .subscription(SubscriptionName.fromString('group.topic$sub')).build()

    ConsumerProcessSupervisor consumerProcessSupervisor
    HermesMetrics hermesMetrics

    def setup() {
        configFactory.overrideProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER, 100)
        hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"))

        consumerProcessSupervisor = new ConsumerProcessSupervisor(
                new ConsumersExecutorService(configFactory, hermesMetrics),
                retransmitter,
                new Clock.SystemClock(),
                hermesMetrics,
                configFactory,
                consumerFactory)
    }

    def cleanup() {
        consumerProcessSupervisor.shutdown()
    }

    def "should restart unhealthy process"() {
        given:
        def consumer1 = Stub(Consumer)
        def consumer2 = Stub(Consumer)
        def consumers = [consumer1, consumer2]

        def conditions = new PollingConditions(timeout: 2)
        def createConsumerFactoryMethodCallsCount = 0

        consumer1.consume(_) >> {
            Runnable signalsInterrupt -> signalsInterrupt.run()
        }
        consumer2.consume(_) >> {
            Runnable signalsInterrupt -> signalsInterrupt.run()
        }

        consumerFactory.createConsumer(subscription) >>
                { subscription -> consumers[createConsumerFactoryMethodCallsCount++]}

        consumerProcessSupervisor.accept(
                Signal.of(Signal.SignalType.START, subscription.getQualifiedName(), subscription))
        consumerProcessSupervisor.run()

        when:
        consumerProcessSupervisor.accept(
                Signal.of(Signal.SignalType.RESTART_UNHEALTHY, subscription.getQualifiedName()))

        then:
        conditions.eventually {
            consumerProcessSupervisor.run()
            assert createConsumerFactoryMethodCallsCount == 2
            assert consumerProcessSupervisor.countRunningProcesses() == 1
        }
    }

    @Unroll
    def "should #signalName process"() {
        given:
        def delay = 0.5 // every 0.5 seconds check test condition

        // we need object variable to be updated in the consumer process because if we use long variable
        // it will not be allocated on heap and changes will not be visible
        def timestampContainer = new TimestampContainer()

        def consumer = Stub(Consumer)

        // 4 * delay is just a chosen rational timeout in which condition should be fulfilled
        def conditions = new PollingConditions(timeout: 4 * delay, delay: delay)

        consumer.consume(_) >> { Runnable signalsInterrupt ->
            signalsInterrupt.run()
            timestampContainer.update()
        }

        consumerFactory.createConsumer(subscription) >> consumer

        consumerProcessSupervisor.accept(
                Signal.of(Signal.SignalType.START, subscription.getQualifiedName(), subscription))

        consumerProcessSupervisor.run()

        when:
        consumerProcessSupervisor.accept(
                Signal.of(signalType, subscription.getQualifiedName(), subscription))

        consumerProcessSupervisor.run()

        then:
        // This condition will run every 0.5 seconds and wait at most 4 loop runs in which consumer should be stopped.
        // 2 * delay means that within 4 condition loops, 2 consecutive timestampContainer.timestamp values should not
        // change (so this indicates that consumer process was stopped).
        conditions.within(4 * delay) {
            assert System.currentTimeMillis() - timestampContainer.timestamp > 2 * delay * 1000
            assert consumerProcessSupervisor.countRunningProcesses() == 0
        }

        where:
        signalType             |   signalName
        Signal.SignalType.STOP |   "stop"
        Signal.SignalType.KILL |   "kill"
    }


    def "should force kill stopped process if it is running too long"() {
        given:
        def delay = 0.5
        def timestampContainer = new TimestampContainer()

        def consumer = Stub(Consumer)
        def conditions = new PollingConditions(timeout: 4 * delay, delay: delay)

        consumer.consume(_) >> { Runnable signalsInterrupt ->
            while (true) {
                Thread.sleep(200)
                timestampContainer.update()
            }
        }

        consumerFactory.createConsumer(subscription) >> consumer

        consumerProcessSupervisor.accept(
                Signal.of(Signal.SignalType.START, subscription.getQualifiedName(), subscription))

        consumerProcessSupervisor.run()

        when:
        consumerProcessSupervisor.accept(
                Signal.of(Signal.SignalType.STOP, subscription.getQualifiedName(), subscription))

        then:
        conditions.within(4 * delay) {
            consumerProcessSupervisor.run()
            assert System.currentTimeMillis() - timestampContainer.timestamp > 2 * delay * 1000
            assert consumerProcessSupervisor.countRunningProcesses() == 0
        }
    }

    class TimestampContainer {
        def timestamp = System.currentTimeMillis()

        def update() {
            timestamp = System.currentTimeMillis()
        }
    }
}
