package pl.allegro.tech.hermes.consumers.consumer.oauth

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.OAuthHttpAuthorizationProvider
import spock.lang.Specification

class OAuthHttpAuthorizationProviderTest extends Specification {

    def subscriptionName = new SubscriptionName("subscription", TopicName.fromQualifiedName("group.topic"));

    def accessTokens = Mock(OAuthAccessTokens)

    def "should return bearer token"() {
        given:
        def provider = new OAuthHttpAuthorizationProvider(subscriptionName, accessTokens)
        def token = new OAuthAccessToken("abc", 3600)
        accessTokens.getTokenIfPresent(subscriptionName) >> Optional.of(token)

        when:
        def tokenHeaderValue = provider.authorizationToken().get()

        then:
        tokenHeaderValue == "Bearer abc"
    }
}
