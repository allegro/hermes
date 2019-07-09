package pl.allegro.tech.hermes.consumers.consumer.sender.http

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Header
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider
import pl.allegro.tech.hermes.schema.CompiledSchema
import pl.allegro.tech.hermes.schema.SchemaVersion
import spock.lang.Specification
import spock.lang.Unroll

class Http1RequestHeadersProviderTest extends Specification {

    HttpAuthorizationProvider authorizationProvider = new HttpAuthorizationProvider() {
        @Override
        Optional<String> authorizationToken() {
            return Optional.of("token")
        }
    }

    HttpRequestHeadersProvider requestHeadersProvider = new Http1RequestHeadersProvider(Optional.empty())
    HttpRequestHeadersProvider requestHeadersProviderWithAuthorization = new Http1RequestHeadersProvider(Optional.of(authorizationProvider))

    @Unroll
    def "should contain #header header with value"() {
        given:
        Message message = createMessage()

        when:
        HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message)

        then:
        headers.asMap().size() == 4
        headers.asMap().get(header) == value

        where:
        header  |   value
        "Hermes-Message-Id" |   "123"
        "Hermes-Retry-Count" |   "0"
        "Content-Type" |   "application/json"
        "Keep-Alive" |   "true"
    }

    def "should contain topic and subscription name headers when message has subscription identity headers"() {
        given:
        Message message = createMessageWithSubscriptionData()

        when:
        HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message)

        then:
        def headersAsMap = headers.asMap()
        headersAsMap.get("Hermes-Topic-Name") == "topic1"
        headersAsMap.get("Hermes-Subscription-Name") == "subscription1"
    }

    def "should contain schema version header when message has schema"() {
        given:
        Message message = createMessageWithSchema()

        when:
        HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message)

        then:
        headers.asMap().get("Schema-Version") == "1"
    }

    def "should contain authorization header when authorization provider is present"() {
        given:
        Message message = createMessage()

        when:
        HttpRequestHeaders headers = requestHeadersProviderWithAuthorization.getHeaders(message)

        then:
        headers.asMap().get("Authorization") == "token"
    }

    def "should contain additional headers"() {
        given:
        Message message = createMessageWithAdditionalHeaders()

        when:
        HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message)

        then:
        headers.asMap().get("key") == "value"
    }

    Message createMessage() {
        new Message("123", null, null, ContentType.JSON, Optional.empty(), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.emptyList(), null, false)
    }

    Message createMessageWithSubscriptionData() {
        new Message("123", "topic1", null, ContentType.JSON, Optional.empty(), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.emptyList(), "subscription1", true)
    }

    Message createMessageWithSchema() {
        new Message("123", null, null, ContentType.JSON, Optional.of(new CompiledSchema<>(1, SchemaVersion.valueOf(1))), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.emptyList(), null, false)
    }

    Message createMessageWithAdditionalHeaders() {
        new Message("123", null, null, ContentType.JSON, Optional.empty(), 0l, 0l, null, 0l, Collections.emptyMap(), Collections.singletonList(new Header("key", "value")), null, false)
    }

}
