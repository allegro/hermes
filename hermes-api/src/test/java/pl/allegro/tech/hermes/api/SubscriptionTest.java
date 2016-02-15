package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;

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
        SubscriptionPolicy message = subscriptionPolicy().withRate(8).build();

        //when
        SubscriptionPolicy subscription = subscriptionPolicy()
                .withRate(1)
                .applyPatch(message).build();

        //then
        assertThat(subscription.getRate()).isEqualTo(message.getRate());
    }

    @Test
    public void shouldAnonymizePassword() {
        // given
        Subscription subscription = Subscription.Builder.subscription().withEndpoint(new EndpointAddress("http://user:password@service/path")).build();

        // when & then
        assertThat(subscription.anonymizePassword().getEndpoint()).isEqualTo(new EndpointAddress("http://user:*****@service/path"));
    }

}
