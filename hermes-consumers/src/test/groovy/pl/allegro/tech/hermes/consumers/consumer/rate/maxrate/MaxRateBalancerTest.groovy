package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import pl.allegro.tech.hermes.common.config.Configs
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class MaxRateBalancerTest extends Specification {

    def balancer = new MaxRateBalancer()
    def conditions = new PollingConditions()

    def "should assign equal rates initially"() {
        when:
            def maxRates = balancer.balance(100, [
                    new ConsumerRateInfo("consumer1", Optional.empty(), RateHistory.empty()),
                    new ConsumerRateInfo("consumer2", Optional.empty(), RateHistory.empty())
            ] as Set)

        then:
            maxRates.get() == [
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
            maxRates.get() == [
                    "consumer1": new MaxRate(50),
                    "consumer2": new MaxRate(50)
            ] as Map
    }

    def "should evenly distribute rate for all busy consumers"() {
        given:
            def consumerHistory = new RateHistory([1.0, 1.0, 1.0] as List)
            def maxRate1 = Optional.of(new MaxRate(99.0))
            def maxRate2 = Optional.of(new MaxRate(1.0))

        expect:
            conditions.eventually {
                def maxRates = balancer.balance(100.0, [
                        new ConsumerRateInfo("consumer1", maxRate1, consumerHistory),
                        new ConsumerRateInfo("consumer2", maxRate2, consumerHistory)
                ] as Set).get()

                maxRate1 = maxRates["consumer1"]
                maxRate2 = maxRates["consumer2"]

                assert maxRates["consumer1"].getMaxRate() == 50
                assert maxRates["consumer2"].getMaxRate() == 50
            }
    }
}
