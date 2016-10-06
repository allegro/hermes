package pl.allegro.tech.hermes.consumers.consumer.oauth

import spock.lang.Specification

class OAuthTokenRequestRateLimiterTest extends Specification {

    def "should reduce rate according to provided reduction factor"() {
        given:
        def rateLimiter = new OAuthTokenRequestRateLimiter(10.0, 0.2, 2.0, 1000)

        when:
        rateLimiter.reduceRate()
        rateLimiter.reduceRate()

        then:
        rateLimiter.currentRate == 2.5d
    }

    def "should not reduce rate more than minimal rate"() {
        given:
        def rateLimiter = new OAuthTokenRequestRateLimiter(10.0, 5.0, 2.0, 1000)

        when:
        rateLimiter.reduceRate()
        rateLimiter.reduceRate()

        then:
        rateLimiter.currentRate == 5d
    }

    def "should reset to initial rate"() {
        given:
        def rateLimiter = new OAuthTokenRequestRateLimiter(10.0, 0.1, 2.0, 1000)

        when:
        rateLimiter.reduceRate()
        rateLimiter.reduceRate()
        rateLimiter.reduceRate()

        and:
        rateLimiter.resetRate()

        then:
        rateLimiter.currentRate == 10d
    }
}
