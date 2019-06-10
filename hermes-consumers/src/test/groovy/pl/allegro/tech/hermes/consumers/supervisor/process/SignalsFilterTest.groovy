package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.queue.MpscQueue
import pl.allegro.tech.hermes.consumers.queue.WaitFreeDrainMpscQueue
import pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SignalsFilterTest extends Specification {

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault())

    private final MpscQueue<Signal> taskQueue = new WaitFreeDrainMpscQueue<>(10)

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

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals)

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

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals)

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
                Signal.of(SignalType.START, subscription('A'), payload, 2048),
        ]

        when:
        Set<Signal> filteredSignals = filter.filterSignals(signals)

        then:
        filteredSignals == [] as Set
        taskQueue.drain({ s -> s == Signal.of(SignalType.START, subscription('A')) })
    }

    private SubscriptionName subscription(String suffix) {
        return SubscriptionName.fromString("group.topic\$sub$suffix")
    }
}
