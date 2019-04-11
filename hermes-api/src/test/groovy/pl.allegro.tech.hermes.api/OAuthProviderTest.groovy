package pl.allegro.tech.hermes.api

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

class OAuthProviderTest extends Specification {

    @Shared
    def objectMapper = new ObjectMapper()

    def "should serialize to json and deserialize back"() {
        given:
        def provider = new OAuthProvider("myProvider", "http://example.com/token", "client123", "secret123", 1, 8, 500, 700)

        when:
        def json = objectMapper.writeValueAsString(provider)

        and:
        def deserialized = objectMapper.readValue(json, OAuthProvider.class)

        then:
        provider == deserialized
        provider.tokenEndpoint == "http://example.com/token"
        provider.name == "myProvider"
        provider.clientId == "client123"
        provider.clientSecret == "secret123"
        provider.tokenRequestInitialDelay == 1
        provider.tokenRequestMaxDelay == 8
        provider.requestTimeout == 500
        provider.socketTimeout == 700
    }

    def "should deserialize provider with previously missing socket timeout"() {
        given:
        def json = """
        {
            "name": "myProvider",
            "tokenEndpoint": "http://example.com/token",
            "clientId": "client123",
            "clientSecret": "secret123",
            "tokenRequestInitialDelay": 1,
            "tokenRequestMaxDelay": 8,
            "requestTimeout": 500
        }
        """

        when:
        def deserialized = objectMapper.readValue(json, OAuthProviderWithOptionalSocketTimeout.class).toBasicOAuthProvider()

        then:
        deserialized.tokenEndpoint == "http://example.com/token"
        deserialized.name == "myProvider"
        deserialized.clientId == "client123"
        deserialized.clientSecret == "secret123"
        deserialized.tokenRequestInitialDelay == 1
        deserialized.tokenRequestMaxDelay == 8
        deserialized.requestTimeout == 500
        deserialized.socketTimeout == 0
    }

    def "should anonymize client secret"() {
        when:
        def provider = new OAuthProvider("myProvider", "http://example.com/token", "client123", "secret123", 1, 8, 500, 500)
                .anonymize()

        then:
        provider.clientSecret == "******"
    }

}
