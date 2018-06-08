package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SignalsFilterTest extends Specification {

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault())

    private final MonitoredMpscQueue<Signal> taskQueue = new MonitoredMpscQueue<>(Mock(HermesMetrics), "queue", 10)

    private final SignalsFilter filter = new SignalsFilter(taskQueue, clock)

    @Shared
    def SIGNALS_ALLOWED_FOR_RUNNING_PROCESSES = SignalType.values()
            .findAll { ![SignalType.CLEANUP, SignalType.FORCE_KILL_DYING].contains(it) }
            .collect { Signal.of(it, subscription('A')) }

    @Shared
    def SIGNALS_NOT_ALLOWED_FOR_DYING_PROCESSES = SignalType.values()
            .findAll { ![SignalType.CLEANUP, SignalType.START, SignalType.FORCE_KILL_DYING].contains(it) }
            .collect { Signal.of(it, subscription('A')) }

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
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers, [] as Set)

        then:
        filteredSignals == [
                Signal.of(SignalType.STOP, subscription('B')),
                Signal.of(SignalType.STOP, subscription('C')),
                Signal.of(SignalType.START, subscription('D'))
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
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers, [] as Set)

        then:
        filteredSignals == [
                Signal.of(SignalType.UPDATE_TOPIC, subscription('A')),
        ] as Set
        filteredSignals
    }

    def "should remove signals that should be executed later and put them back on task queue"() {
        given:
        Object payload = null
        List<Signal> signals = [
                Signal.of(SignalType.FORCE_KILL_DYING, subscription('A'), payload, 2048),
        ]

        Set<SubscriptionName> existingConsumers = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers, [] as Set)

        then:
        filteredSignals == [] as Set
        taskQueue.drain({ s -> s == Signal.of(SignalType.FORCE_KILL_DYING, subscription('A')) })
    }

    def "should filter out signals for consumer processes that do not exist"() {
        given:
        List<Signal> signals = [
                Signal.of(SignalType.KILL, subscription('A')),
                Signal.of(SignalType.KILL, subscription('B')),
        ]

        Set<SubscriptionName> existingConsumers = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers, [] as Set)

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
        Set<Signal> filteredSignals = filter.filterSignals(signals, existingConsumers, [] as Set)

        then:
        filteredSignals == [
                Signal.of(SignalType.START, subscription('A'))
        ] as Set
    }

    @Unroll
    def "should allow processing signals other than CLEANUP for running processes"() {
        given:
        Set<SubscriptionName> runningProcesses = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, runningProcesses, [] as Set)

        then:
        filteredSignals == expectedSignals

        where:
        signals << SIGNALS_ALLOWED_FOR_RUNNING_PROCESSES.collect {[it]} +
                [[Signal.of(SignalType.CLEANUP, subscription('A'))]]

        expectedSignals << SIGNALS_ALLOWED_FOR_RUNNING_PROCESSES.collect { [it] as Set } + [[] as Set]
    }

    @Unroll
    def "should not allow processing signals other than CLEANUP & START & FORCE_KILL_DYING for dying processes"() {
        given:
        Set<SubscriptionName> dyingConsumers = [subscription('A')]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals, [] as Set, dyingConsumers)

        then:
        filteredSignals == expectedSignals

        where:
        signals << SIGNALS_NOT_ALLOWED_FOR_DYING_PROCESSES.collect {[it]} +
                [[Signal.of(SignalType.CLEANUP, subscription('A'))]] +
                [[Signal.of(SignalType.START, subscription('A'))]] +
                [[Signal.of(SignalType.FORCE_KILL_DYING, subscription('A'))]]

        expectedSignals << SIGNALS_NOT_ALLOWED_FOR_DYING_PROCESSES.collect { [] as Set } +
                [[Signal.of(SignalType.CLEANUP, subscription('A'))] as Set] +
                [[Signal.of(SignalType.START, subscription('A'))] as Set] +
                [[Signal.of(SignalType.FORCE_KILL_DYING, subscription('A'))] as Set]
    }

    private SubscriptionName subscription(String suffix) {
        return SubscriptionName.fromString("group.topic\$sub$suffix")
    }
}
