package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class SubscriptionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldDeserializeSubscription() throws Exception {
        // given
        String json = "{" +
                "\"name\": \"test\", " +
                "\"topicName\": \"g1.t1\", " +
                "\"endpoint\": \"http://localhost:8888\"" +
                "}";

        // when
        Subscription subscription = mapper.readValue(json, Subscription.class);

        // then
        assertThat(subscription.getName()).isEqualTo("test");
        assertThat(subscription.getEndpoint().getEndpoint()).isEqualTo("http://localhost:8888");
    }

    @Test
    public void shouldDeserializeSubscriptionWithoutTopicName() throws Exception {
        // given
        String json = "{\"name\": \"test\", \"endpoint\": \"http://localhost:8888\"}";

        // when
        Subscription subscription = mapper.readValue(json, Subscription.class);

        // then
        assertThat(subscription.getName()).isEqualTo("test");
        assertThat(subscription.getEndpoint().getEndpoint()).isEqualTo("http://localhost:8888");
    }

    @Test
    public void shouldDeserializeSubscriptionWithoutBackoff() throws Exception {
        // given
        String json = "{\"name\": \"test\", \"endpoint\": \"http://localhost:8888\", \"subscriptionPolicy\": {\"messageTtl\": 100}}";

        // when
        Subscription subscription = mapper.readValue(json, Subscription.class);

        // then
        assertThat(subscription.getSerialSubscriptionPolicy().getMessageBackoff()).isEqualTo(100);
    }

    @Test
    public void shouldApplyPatchToSubscriptionPolicy() {
        //given
        PatchData patch = patchData().set("rate", 8).build();

        //when
        SubscriptionPolicy subscription = subscriptionPolicy()
                .withRate(1)
                .applyPatch(patch).build();

        //then
        assertThat(subscription.getRate()).isEqualTo(8);
    }

    @Test
    public void shouldAnonymizeBasicAuthenticationData() {
        // given
        Subscription subscription = subscription("group.topic", "subscription").withEndpoint("http://service/path")
                                        .withAuthentication(new BasicAuthenticationData("username", "password")).build();

        // when & then
        assertThat(subscription.anonymize().getBasicAuthenticationData()).isEqualTo(new BasicAuthenticationData("username", "*****"));
    }

    @Test
    public void shouldAnonymizeOAuth2AuthenticationData() {
        // given
        Subscription subscription = subscription("group.topic", "subscription").withEndpoint("http://service/path")
                                        .withAuthentication(
                                                new OAuth2AuthenticationData("username", "password", "key", "secret", "password", 
                                                        new EndpointAddress("http://service/authorize"))
                                        ).build();

        // when & then
        assertThat(subscription.anonymize().getOAuth2AuthenticationData()).isEqualTo(
                new OAuth2AuthenticationData("username", "*****", "key", "*****", "password", 
                        new EndpointAddress("http://service/authorize"))
        );
    }

}
