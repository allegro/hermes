package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

class MaxRateBalancerTest extends Specification {

    def balancer = new MaxRateBalancer(0.1d, 1.0d, 1.0d) //new MaxRateBalancer()
    def conditions = new PollingConditions(timeout: 5)

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
            !maxRates.isPresent()
    }

    @Unroll
    def "should evenly distribute rate for all busy consumers"() {
        given:
            def consumerHistory = new RateHistory([1.0d, 1.0d, 1.0d] as List)
            def maxRate1 = Optional.of(new MaxRate(initial1))
            def maxRate2 = Optional.of(new MaxRate(initial2))

        expect:
            conditions.eventually {
                def maxRates = balancer.balance(100.0, [
                        new ConsumerRateInfo("consumer1", maxRate1, consumerHistory),
                        new ConsumerRateInfo("consumer2", maxRate2, consumerHistory)
                ] as Set).get()

                println maxRates

                maxRate1 = Optional.of(maxRates["consumer1"])
                maxRate2 = Optional.of(maxRates["consumer2"])

                assert Math.abs(maxRates["consumer1"].getMaxRate() - maxRates["consumer2"].getMaxRate()) <= 1d;
            }

        where:
        initial1 | initial2
        99.9     | 0.1
        99       | 1
        95       | 5
        80       | 20
        75       | 25
        60       | 40
        52       | 48
        50       | 50
    }

    def "should take away from not busy and give to busy"() {
        given:
        def busyHistory = new RateHistory([1.0d, 1.0d, 1.0d] as List)
        def notBusyHistory = new RateHistory([0.2d, 0.3d, 0.5d] as List)
        def busyMax = Optional.of(new MaxRate(70.0))
        def notBusyMax = Optional.of(new MaxRate(30.0))

        when:
        def maxRates = balancer.balance(100.0, [
                new ConsumerRateInfo("busy", busyMax, busyHistory),
                new ConsumerRateInfo("notBusy", notBusyMax, notBusyHistory)
        ] as Set).get()

        then:
        maxRates['notBusy'].getMaxRate() < 29.0
        maxRates['busy'].getMaxRate() > 71.0
    }
}
