package pl.allegro.tech.hermes.consumers.consumer.oauth

import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.clientCredentialsGrantOAuthPolicy

class OAuthSubscriptionHandlerTest extends Specification {

    def tokens = Mock(OAuthAccessTokens)

    def rateLimiter = Mock(OAuthTokenRequestRateLimiter)

    def conditions = new PollingConditions(timeout: 1)

    OAuthSubscriptionHandler handler

    def subscription = SubscriptionBuilder.subscription("group.topic", "subscription")
            .withOAuthPolicy(clientCredentialsGrantOAuthPolicy("provider1").build())
            .build()

    def setup() {
        handler = new OAuthSubscriptionHandler(subscription.qualifiedName, "provider1", tokens, rateLimiter)
    }

    def "should request token on init"() {
        when:
        handler.initialize()

        then:
        1 * tokens.loadToken(subscription.qualifiedName)
    }

    def "should refresh token and reduce token request rate on authentication failure"() {
        given:
        def result = MessageSendingResult.failedResult(401)
        rateLimiter.tryAcquire() >> true

        when:
        handler.handleFailed(subscription, result)

        then:
        conditions.eventually {
            assert { 1 * tokens.refreshToken(subscription.qualifiedName) }
            assert { 1 * rateLimiter.reduceRate() }
        }
    }

    def "should try to get new token when cache is empty"() {
        given:
        def result = MessageSendingResult.failedResult(new RuntimeException("something went wrong"))
        rateLimiter.tryAcquire() >> true
        tokens.tokenExists(subscription.qualifiedName) >> false

        when:
        handler.handleFailed(subscription, result)

        then:
        conditions.eventually {
            assert { 1 * tokens.refreshToken(subscription.qualifiedName) }
            assert { 1 * rateLimiter.reduceRate() }
        }
    }

    def "should reset token request rate on success"() {
        given:
        rateLimiter.tryAcquire() >> true

        when:
        handler.handleSuccess()

        then:
        1 * rateLimiter.resetRate()
    }
}
