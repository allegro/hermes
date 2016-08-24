package pl.allegro.tech.hermes.api

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.GrantType.USERNAME_PASSWORD

class SubscriptionOAuthPolicyTest extends Specification {

    @Shared
    def objectMapper = new ObjectMapper()

    def "should serialize to json and deserialize back"() {
        given:
        def policy = new SubscriptionOAuthPolicy(USERNAME_PASSWORD, "myProvider", "user", "user1", "abc123")

        when:
        def json = objectMapper.writeValueAsString(policy)

        and:
        def deserialized = objectMapper.readValue(json, SubscriptionOAuthPolicy.class)

        then:
        deserialized instanceof SubscriptionOAuthPolicy
        policy == deserialized
    }
}
