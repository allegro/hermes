package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import spock.lang.Specification

class MaxRateBalancerTest extends Specification {

    def balancer = new MaxRateBalancer()

    def "should assign equal rates initially"() {
        when:
            def maxRates = balancer.balance(100, [
                    new ConsumerRateInfo("consumer1", Optional.empty(), RateHistory.empty()),
                    new ConsumerRateInfo("consumer2", Optional.empty(), RateHistory.empty())
            ] as Set)

        then:
            maxRates == [
                    "consumer1": new MaxRate(50),
                    "consumer2": new MaxRate(50)
            ] as Map
    }

    def "should not change max rates if no busy consumer"() {
        given:
            def consumer1History = RateHistory.create(0.7)
            def consumer2History = RateHistory.create(0.7)

        when:
            def maxRates = balancer.balance(100, [
                    new ConsumerRateInfo("consumer1", Optional.of(new MaxRate(50)), consumer1History),
                    new ConsumerRateInfo("consumer2", Optional.of(new MaxRate(50)), consumer2History)
            ] as Set)

        then:
            maxRates == [
                    "consumer1": new MaxRate(50),
                    "consumer2": new MaxRate(50)
            ] as Map
    }
}
