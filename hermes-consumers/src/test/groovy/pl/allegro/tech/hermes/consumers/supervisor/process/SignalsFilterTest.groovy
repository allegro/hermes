package pl.allegro.tech.hermes.consumers.supervisor.process

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset
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
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.RETRANSMIT
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.STOP
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_SUBSCRIPTION
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.UPDATE_TOPIC

class SignalsFilterTest extends Specification {

    private Clock clock = Clock.fixed(Instant.ofEpochMilli(1024), ZoneId.systemDefault())

    private MpscQueue<Signal> taskQueue = new WaitFreeDrainMpscQueue<>(10)

    private SignalsFilter filter = new SignalsFilter(taskQueue, clock)

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
    def "should remove duplicated signals #signalType"() {
        given:
        List<Signal> signals = [
                Signal.of(signalType, subscription('A')),
                Signal.of(signalType, subscription('A')),
                Signal.of(signalType, subscription('B'))
        ]

        when:
        List<Signal> filteredSignals = filter.filterSignals(signals)

        then:
        filteredSignals == [
                Signal.of(signalType, subscription('A')),
                Signal.of(signalType, subscription('B'))
        ]

        where:
        signalType << [STOP, START]
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
        return subscriptionPartitionOffset(fromString('group.topic$sub'),
                new PartitionOffset(KafkaTopicName.valueOf('group_topic'), offset, partition), -1)
    }
}
