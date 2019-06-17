package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset
import pl.allegro.tech.hermes.consumers.queue.MpscQueue
import pl.allegro.tech.hermes.consumers.queue.WaitFreeDrainMpscQueue
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static pl.allegro.tech.hermes.api.SubscriptionName.fromString
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.COMMIT
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.RETRANSMIT
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.STOP
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_SUBSCRIPTION
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_TOPIC

class SignalsFilterTest extends Specification {

    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault())

    private final MpscQueue<Signal> taskQueue = new WaitFreeDrainMpscQueue<>(10)

    private final SignalsFilter filter = new SignalsFilter(taskQueue, clock)

    def "should filter out contradicting signals like START & STOP or STOP & START for the same subscription"() {
        given:
        List<Signal> signals = [
                Signal.of(STOP, subscription('A')),
                Signal.of(STOP, subscription('B')),
                Signal.of(STOP, subscription('C')),
                Signal.of(START, subscription('D')),
                Signal.of(START, subscription('A'))
        ]

        when:
        List<Signal> filteredSignals = filter.filterSignals(signals)

        then:
        filteredSignals == [
                Signal.of(STOP, subscription('B')),
                Signal.of(STOP, subscription('C')),
                Signal.of(START, subscription('D'))
        ]
    }

    @Unroll
    def "should not remove duplicated #signalType signals"() {
        given:
        List<Signal> signals = [
                Signal.of(signalType, subscription('A'), firstSignalPayload),
                Signal.of(signalType, subscription('A'), secondSignalPayload)
        ]

        when:
        List<Signal> filteredSignals = filter.filterSignals(signals)

        then:
        filteredSignals == signals

        where:
        signalType          | firstSignalPayload             | secondSignalPayload
        UPDATE_TOPIC        | 'first-update'                 | 'second-update'
        UPDATE_SUBSCRIPTION | 'first-update'                 | 'second-update'
        COMMIT              | [offset(1, 10), offset(2, 11)] | [offset(1, 11)]
        RETRANSMIT          | null                           | null
    }

    def "should remove signals that should be executed later and put them back on task queue"() {
        given:
        Object payload = null
        List<Signal> signals = [
                Signal.of(START, subscription('A'), payload, 2048),
        ]

        when:
        List<Signal> filteredSignals = filter.filterSignals(signals)

        then:
        filteredSignals == []
        taskQueue.drain({ s -> s == Signal.of(START, subscription('A')) })
    }

    private static SubscriptionName subscription(String suffix) {
        return fromString("group.topic\$sub$suffix")
    }

    private static SubscriptionPartitionOffset offset(int partition, long offset) {
        return subscriptionPartitionOffset("group_topic", 'group.topic$sub', partition, offset)
    }
}
