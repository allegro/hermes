package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionId
import pl.allegro.tech.hermes.consumers.subscription.id.SubscriptionIds
import spock.lang.Specification

class ConsumerMaxRatesEncoderDecoderTest extends Specification {

    def subscriptionsIds = Stub(SubscriptionIds)

    def encoder = new ConsumerMaxRatesEncoder(subscriptionsIds, 100)

    def decoder = new ConsumerMaxRatesDecoder(subscriptionsIds)

    def "should encode and decode rate histories"() {
        given:
        def sub1 = SubscriptionName.fromString('pl.allegro.tech.consumers$sub1')
        def sub2 = SubscriptionName.fromString('pl.allegro.tech.consumers$sub2')
        def id1 = SubscriptionId.from(sub1, -1422951212L)
        def id2 = SubscriptionId.from(sub2, 2L)

        subscriptionsIds.getSubscriptionId(sub1) >> Optional.of(id1)
        subscriptionsIds.getSubscriptionId(sub2) >> Optional.of(id2)
        subscriptionsIds.getSubscriptionId(id1.value) >> Optional.of(id1)
        subscriptionsIds.getSubscriptionId(id2.value) >> Optional.of(id2)

        def maxRates = new ConsumerMaxRates()
        maxRates.setMaxRate(sub1, new MaxRate(1000d))
        maxRates.setMaxRate(sub2, new MaxRate(2000d))

        when:
        def encoded = encoder.encode(maxRates)

        then:
        encoded

        when:
        def maxRatesDecoded = decoder.decode(encoded)

        then:
        maxRates == maxRatesDecoded
    }
}