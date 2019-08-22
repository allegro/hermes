package pl.allegro.tech.hermes.consumers.supervisor.workload

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId
import spock.lang.Specification

class ConsumerWorkloadEncoderDecoderTest extends Specification {

    def sub1 = SubscriptionName.fromString('pl.allegro.tech.consumers$sub1')
    def sub2 = SubscriptionName.fromString('pl.allegro.tech.consumers$sub2')

    def subscriptionsIds = new TestSubscriptionIds([
            SubscriptionId.from(sub1, -1422951212L),
            SubscriptionId.from(sub2, 2L)
    ])

    def encoder = new ConsumerWorkloadEncoder(subscriptionsIds, 100)

    def decoder = new ConsumerWorkloadDecoder(subscriptionsIds)

    def "should encode and decode workload"() {
        given:
        def assignedSubscriptions = [sub1, sub2] as Set

        when:
        def encoded = encoder.encode(assignedSubscriptions)

        then:
        encoded

        when:
        def decodedSubscriptions = decoder.decode(encoded)

        then:
        assignedSubscriptions == decodedSubscriptions
    }
}
