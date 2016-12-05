package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

class MaxRateBalancerTest extends Specification {

    static final double MIN_MAX_RATE = 1.0d
    static final double MIN_CHANGE_PERCENT = 1.0d
    static final double BUSY_TOLERANCE = 0.1d

    def balancer = new MaxRateBalancer(BUSY_TOLERANCE, MIN_MAX_RATE, MIN_CHANGE_PERCENT)
    def conditions = new PollingConditions(timeout: 5)

    def "should assign equal rates initially"() {
        when:
            def maxRates = balancer.balance(100d, [
                    new ConsumerRateInfo("consumer1", Optional.empty(), RateHistory.empty()),
                    new ConsumerRateInfo("consumer2", Optional.empty(), RateHistory.empty())
            ] as Set)

        then:
            maxRates.get() == ["consumer1": new MaxRate(50d), "consumer2": new MaxRate(50d)] as Map
    }

    def "should not change max rates if no busy consumer"() {
        given:
            def consumer1History = RateHistory.create(0.7d)
            def consumer2History = RateHistory.create(0.7d)

        when:
            def maxRates = balancer.balance(100d, [
                    new ConsumerRateInfo("consumer1", Optional.of(new MaxRate(50d)), consumer1History),
                    new ConsumerRateInfo("consumer2", Optional.of(new MaxRate(50d)), consumer2History)
            ] as Set)

        then:
            !maxRates.isPresent()
    }

    @Unroll
    def "should evenly distribute rate for all busy consumers starting with max rates: #initial1 and #initial2"() {
        given:
            def consumerHistory = new RateHistory([1.0d, 1.0d, 1.0d] as List)
            def maxRate1 = Optional.of(new MaxRate(initial1 as double))
            def maxRate2 = Optional.of(new MaxRate(initial2 as double))

        expect:
            conditions.eventually {
                def subscriptionRate = 100d
                def maxRates = balancer.balance(subscriptionRate, [
                        new ConsumerRateInfo("consumer1", maxRate1, consumerHistory),
                        new ConsumerRateInfo("consumer2", maxRate2, consumerHistory)
                ] as Set).get()

                println maxRates

                maxRate1 = Optional.of(maxRates["consumer1"])
                maxRate2 = Optional.of(maxRates["consumer2"])

                def difference = Math.abs(maxRates["consumer1"].getMaxRate() - maxRates["consumer2"].getMaxRate())
                assert difference <=  subscriptionRate * (MIN_CHANGE_PERCENT / 100d);
            }

        where:
            initial1 | initial2
            99.9     | 0.1        // checking even though it's below min max rate
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
            def busyMax = Optional.of(new MaxRate(70d))
            def notBusyMax = Optional.of(new MaxRate(30d))

        when:
            def maxRates = balancer.balance(100d, [
                    new ConsumerRateInfo("busy", busyMax, busyHistory),
                    new ConsumerRateInfo("notBusy", notBusyMax, notBusyHistory)
            ] as Set).get()

        then:
            maxRates['notBusy'].getMaxRate() < 29d
            maxRates['busy'].getMaxRate() > 71d
    }

    def "should restart distribution on subscription rate change"() {
        when:
            def maxRates = balancer.balance(200d, [
                    new ConsumerRateInfo("consumer1", Optional.of(new MaxRate(50d)), RateHistory.create(0.7d)),
                    new ConsumerRateInfo("consumer2", Optional.of(new MaxRate(50d)), RateHistory.create(0.99d))
            ] as Set)

        then:
            maxRates.get() == ["consumer1": new MaxRate(100d), "consumer2": new MaxRate(100d)] as Map
    }

    def "should take everything from unoccupied consumer"() {
        when:
            def maxRates = balancer.balance(200d, [
                    new ConsumerRateInfo("consumer1", Optional.of(new MaxRate(100d)), RateHistory.create(0.0d)),
                    new ConsumerRateInfo("consumer2", Optional.of(new MaxRate(100d)), RateHistory.create(1.0d))
            ] as Set).get()

        then:
            maxRates["consumer1"].getMaxRate() == MIN_MAX_RATE
            maxRates["consumer2"].getMaxRate() == 200d - MIN_MAX_RATE
    }

    def "should preserve min max rate"() {
        when:
            def maxRates = balancer.balance(200d, [
                    new ConsumerRateInfo("unoccupied1",
                            Optional.of(new MaxRate(MIN_MAX_RATE)), RateHistory.create(0.0d)),
                    new ConsumerRateInfo("unoccupied2",
                            Optional.of(new MaxRate(MIN_MAX_RATE)), RateHistory.create(0.0d)),
                    new ConsumerRateInfo("busy1",
                            Optional.of(new MaxRate(100d - MIN_MAX_RATE)), RateHistory.create(1.0d)),
                    new ConsumerRateInfo("busy2",
                            Optional.of(new MaxRate(100d - MIN_MAX_RATE)), RateHistory.create(1.0d))
            ] as Set).get()

        then:
            maxRates["unoccupied1"].getMaxRate() == MIN_MAX_RATE
            maxRates["unoccupied2"].getMaxRate() == MIN_MAX_RATE
            maxRates["busy1"].getMaxRate() == 100d - MIN_MAX_RATE
            maxRates["busy2"].getMaxRate() == 100d - MIN_MAX_RATE
    }

    def "should move away from min max rate"() {
        when:
            def maxRates = balancer.balance(200d, [
                    new ConsumerRateInfo("minConsumer",
                            Optional.of(new MaxRate(MIN_MAX_RATE)), RateHistory.create(1.0d)),
                    new ConsumerRateInfo("greedyConsumer",
                            Optional.of(new MaxRate(200d - MIN_MAX_RATE)), RateHistory.create(0.8d))
            ] as Set).get()

        print maxRates
        then:
            maxRates["minConsumer"].getMaxRate() > MIN_MAX_RATE
            maxRates["greedyConsumer"].getMaxRate() < 200d - MIN_MAX_RATE
    }
}
