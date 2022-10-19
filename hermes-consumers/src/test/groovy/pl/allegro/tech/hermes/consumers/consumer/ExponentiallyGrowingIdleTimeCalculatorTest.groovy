package pl.allegro.tech.hermes.consumers.consumer

import pl.allegro.tech.hermes.consumers.consumer.idletime.ExponentiallyGrowingIdleTimeCalculator
import pl.allegro.tech.hermes.consumers.consumer.idletime.IdleTimeCalculator
import spock.lang.Specification


class ExponentiallyGrowingIdleTimeCalculatorTest extends Specification {

    def initialIdleTime = 10
    def maxIdleTime = 1000
    def base = 2

    IdleTimeCalculator calculator

    def setup() {
        this.calculator = new ExponentiallyGrowingIdleTimeCalculator(base, initialIdleTime, maxIdleTime)
    }

    def "initial-idle-time is returned first"() {
        expect:
        calculator.idleTime == initialIdleTime
    }

    def "each consecutive idle-time is multiplied by base"() {
        expect:
        (0..6).each {
            assert calculator.increaseIdleTime() == initialIdleTime * (base ** it)
        }
    }

    def "idle-time should be lower or equal to max-idle-time"() {
        expect:
        10.times {
            assert calculator.increaseIdleTime() <= maxIdleTime
        }
    }

    def "initial-idle-time is returned after reset"() {
        given:
        3.times { calculator.increaseIdleTime() }

        when:
        calculator.reset()

        then:
        calculator.idleTime == initialIdleTime
    }
}
