package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class NegotiatedMaxRateProviderTest extends Specification {

    static final HISTORY_SIZE = 2

    def maxRateRegistry = Mock(MaxRateRegistry)
    def maxRateSupervisor = Mock(MaxRateSupervisor)
    def hermesMetrics = Mock(SubscriptionMetrics)
    def sendCounters = Mock(SendCounters)

    def subscription = subscription("group.topic", "subscription").build()

    def consumer = new ConsumerInstance("consumer", subscription.getQualifiedName());

    def freshProvider = createProvider()
    def initializedProvider = createProvider()

    def setup() {
        1 * sendCounters.getRate() >> 0.5
        1 * maxRateRegistry.getRateHistory(consumer) >> RateHistory.empty()
        maxRateRegistry.getMaxRate(consumer) >> Optional.empty()
        initializedProvider.tickForHistory()
    }

    def "should report history on start"() {
        given:
        final rate = 0.5
        sendCounters.getRate() >> rate
        maxRateRegistry.getRateHistory(consumer) >> RateHistory.empty()
        maxRateRegistry.getMaxRate(consumer) >> Optional.empty()

        when:
        freshProvider.tickForHistory()

        then:
        1 * maxRateRegistry.writeRateHistory(consumer, RateHistory.create(rate))
    }

    def "should not report insignificant change"() {
        given:
        final initialRate = 0.5d
        final insignificantRate = 0.55d
        sendCounters.getRate() >>> [initialRate, insignificantRate]

        when:
        initializedProvider.tickForHistory()

        then:
        0 * maxRateRegistry.writeRateHistory(consumer, _ as RateHistory)
    }

    def "should update existing history on change"() {
        given:
        final initialRate = 0.5d
        final newRate = 0.7d
        sendCounters.getRate() >> newRate
        1 * maxRateRegistry.getRateHistory(consumer) >> RateHistory.create(initialRate)

        when:
        initializedProvider.tickForHistory()

        then:
        1 * maxRateRegistry.writeRateHistory(consumer,
                RateHistory.updatedRates(RateHistory.create(initialRate), newRate, HISTORY_SIZE))
    }

    def "should roll history"() {
        given:
        final initialRate = 0.5d
        final firstRate = 0.7d
        final secondRate = 0.9d
        sendCounters.getRate() >>> [firstRate, secondRate]
        2 * maxRateRegistry.getRateHistory(consumer) >>> [
                RateHistory.create(initialRate),
                RateHistory.updatedRates(RateHistory.create(initialRate), firstRate, HISTORY_SIZE)]
        initializedProvider.tickForHistory()

        when:
        initializedProvider.tickForHistory()

        then:
        1 * maxRateRegistry.writeRateHistory(consumer,
                RateHistory.updatedRates(RateHistory.create(firstRate), secondRate, HISTORY_SIZE))
    }

    private NegotiatedMaxRateProvider createProvider() {
        new NegotiatedMaxRateProvider(consumer.consumerId,
                maxRateRegistry,
                maxRateSupervisor,
                subscription,
                sendCounters,
                hermesMetrics,
                1d,
                0.1d,
                HISTORY_SIZE
        )
    }
}
