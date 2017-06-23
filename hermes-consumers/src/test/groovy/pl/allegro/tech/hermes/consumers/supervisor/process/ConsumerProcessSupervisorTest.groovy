package pl.allegro.tech.hermes.consumers.supervisor.process

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.Consumer
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService
import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Specification

import java.time.Clock

class ConsumerProcessSupervisorTest extends Specification {

    ConfigFactory configFactory = new ConfigFactory()
    Retransmitter retransmitter = Stub(Retransmitter)
    Consumer consumer = Stub(Consumer)

    SubscriptionName subscription = SubscriptionName.fromString('group.topic$sub')

    ConsumerProcessSupervisor consumerProcessSupervisor
    HermesMetrics hermesMetrics

    def setup() {
        hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"))

        consumerProcessSupervisor = new ConsumerProcessSupervisor(
                new ConsumersExecutorService(configFactory, hermesMetrics),
                retransmitter,
                new Clock.SystemClock(),
                hermesMetrics,
                configFactory)

        consumer.consume(_) >> {
            Runnable signalsInterrupt ->
                signalsInterrupt.run()
        }
    }

    def "should not handle commit for non existing consumer process"() {
        given:
        consumerProcessSupervisor.accept(Signal.of(Signal.SignalType.START, subscription, consumer))
        consumerProcessSupervisor.run()

        when:
        consumerProcessSupervisor.accept(Signal.of(Signal.SignalType.CLEANUP, subscription))
        consumerProcessSupervisor.accept(Signal.of(Signal.SignalType.COMMIT, subscription))
        consumerProcessSupervisor.run()

        then:
        hermesMetrics.counter("supervisor.signal.dropped." + Signal.SignalType.COMMIT.name()).getCount() == 1
    }
}
