package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SignalsFilterTest extends Specification {

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault())

    private final MonitoredMpscQueue<Signal> taskQueue = new MonitoredMpscQueue<>(Mock(HermesMetrics), "queue", 10)

    private final SignalsFilter filter = new SignalsFilter(taskQueue, clock)

    def "should filter out contradicting signals like START & STOP or STOP & START for the same subscription"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.STOP, subscription('A')),
                Signal.of(SignalType.STOP, subscription('B')),
                Signal.of(SignalType.STOP, subscription('C')),
                Signal.of(SignalType.START, subscription('D')),
                Signal.of(SignalType.START, subscription('A'))
        ]

        Set<SubscriptionName> existingConsumers = [
                subscription('A'), subscription('B'), subscription('C'), subscription('D')
        ]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers)

        then:
        filteredSignals == [
                Signal.of(SignalType.STOP, subscription('B')),
                Signal.of(SignalType.STOP, subscription('C')),
                Signal.of(SignalType.START, subscription('D'))
        ] as Set
    }

    def "should remove all pending KILL_UNHEALTHY signals if START signals appears"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.KILL_UNHEALTHY, subscription('A')),
                Signal.of(SignalType.UPDATE_TOPIC, subscription('B')),
                Signal.of(SignalType.STOP, subscription('C')),
                Signal.of(SignalType.KILL_UNHEALTHY, subscription('A')),
                Signal.of(SignalType.START, subscription('D')),
                Signal.of(SignalType.START, subscription('A'))
        ]

        Set<SubscriptionName> existingConsumers = [
                subscription('A'), subscription('B'), subscription('C'), subscription('D')
        ]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers)

        then:
        filteredSignals == [
                Signal.of(SignalType.UPDATE_TOPIC, subscription('B')),
                Signal.of(SignalType.STOP, subscription('C')),
                Signal.of(SignalType.START, subscription('D')),
                Signal.of(SignalType.START, subscription('A'))
        ] as Set
    }

    def "should remove duplicate signals keeping the most recent one"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.UPDATE_TOPIC, subscription('A'), 'first-update'),
                Signal.of(SignalType.UPDATE_TOPIC, subscription('A'), 'second-update'),
        ]

        Set<SubscriptionName> existingConsumers = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers)

        then:
        filteredSignals == [
                Signal.of(SignalType.UPDATE_TOPIC, subscription('A')),
        ] as Set
        filteredSignals
    }

    def "should remove signals that should be executed later and put them back on task queue"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.KILL_UNHEALTHY, subscription('A'), 2048),
        ]

        Set<SubscriptionName> existingConsumers = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers)

        then:
        filteredSignals == [] as Set
        taskQueue.drain({ s -> s == Signal.of(SignalType.KILL_UNHEALTHY, subscription('A')) })
    }

    def "should filter out signals for consumer processes that do not exist"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.KILL, subscription('A')),
                Signal.of(SignalType.KILL, subscription('B')),
        ]

        Set<SubscriptionName> existingConsumers = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers)

        then:
        filteredSignals == [
                Signal.of(SignalType.KILL, subscription('A'))
        ] as Set
    }

    def "should allow on processing START signal for processes that do not exist"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.START, subscription('A')),
        ]

        Set<SubscriptionName> existingConsumers = []

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers)

        then:
        filteredSignals == [
                Signal.of(SignalType.START, subscription('A'))
        ] as Set
    }


    private SubscriptionName subscription(String suffix) {
        return SubscriptionName.fromString("group.topic\$sub$suffix")
    }
}
