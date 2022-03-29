package pl.allegro.tech.hermes.consumers.consumer.sender.http

import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.BasicAuthProvider
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProviderFactory
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import spock.lang.Specification

class HttpAuthorizationProviderFactoryTest extends Specification {

    private HttpAuthorizationProviderFactory factory = new HttpAuthorizationProviderFactory(null)

    def "should return empty when URL has no credentials"() {
        given:
        def subscription = SubscriptionBuilder.subscription("group.topic", "subscription")
                .withEndpoint(EndpointAddress.of("http://example.com"))
                .build()
        expect:
        !factory.create(subscription).present
    }

    def "should return BasicAuth provider when URL contains credentials"() {
        given:
        def subscription = SubscriptionBuilder.subscription("group.topic", "subscription")
                .withEndpoint(EndpointAddress.of("http://user:password@example.com"))
                .build()

        expect:
        factory.create(subscription).get() instanceof BasicAuthProvider
    }

}
