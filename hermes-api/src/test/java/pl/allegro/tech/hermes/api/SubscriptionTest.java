package pl.allegro.tech.hermes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.GrantType.CLIENT_CREDENTIALS;
import static pl.allegro.tech.hermes.api.SubscriptionOAuthPolicy.GrantType.USERNAME_PASSWORD;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import pl.allegro.tech.hermes.api.helpers.Patch;

public class SubscriptionTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void shouldDeserializeSubscription() throws Exception {
    // given
    String json =
        "{"
            + "\"name\": \"test\", "
            + "\"topicName\": \"g1.t1\", "
            + "\"endpoint\": \"http://localhost:8888\""
            + "}";

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
    String json =
        "{\"name\": \"test\", \"endpoint\": \"http://localhost:8888\", \"subscriptionPolicy\": {\"messageTtl\": 100}}";

    // when
    Subscription subscription = mapper.readValue(json, Subscription.class);

    // then
    assertThat(subscription.getSerialSubscriptionPolicy().getMessageBackoff()).isEqualTo(100);
  }

  @Test
  public void shouldDeserializeSubscriptionWithDefaultTracking() throws Exception {
    // given
    String json =
        "{"
            + "\"name\": \"test\", "
            + "\"topicName\": \"g1.t1\", "
            + "\"endpoint\": \"http://localhost:8888\""
            + "}";

    // when
    Subscription subscription = mapper.readValue(json, Subscription.class);

    // then
    assertThat(subscription.isTrackingEnabled()).isFalse();
    assertThat(subscription.getTrackingMode()).isEqualTo(TrackingMode.TRACKING_OFF);
  }

  @Test
  public void shouldDeserializeSubscriptionWithTrackAllMode() throws Exception {
    // given
    String json =
        "{"
            + "\"name\": \"test\", "
            + "\"topicName\": \"g1.t1\", "
            + "\"endpoint\": \"http://localhost:8888\", "
            + "\"trackingMode\": \"trackingAll\""
            + "}";

    // when
    Subscription subscription = mapper.readValue(json, Subscription.class);

    // then
    assertThat(subscription.isTrackingEnabled()).isTrue();
    assertThat(subscription.getTrackingMode()).isEqualTo(TrackingMode.TRACK_ALL);
  }

  @Test
  public void shouldDeserializeSubscriptionWithTrackEnabled() throws Exception {
    // given
    String json =
        "{"
            + "\"name\": \"test\", "
            + "\"topicName\": \"g1.t1\", "
            + "\"endpoint\": \"http://localhost:8888\", "
            + "\"trackingEnabled\": \"true\""
            + "}";

    // when
    Subscription subscription = mapper.readValue(json, Subscription.class);

    // then
    assertThat(subscription.isTrackingEnabled()).isTrue();
    assertThat(subscription.getTrackingMode()).isEqualTo(TrackingMode.TRACK_ALL);
  }

  @Test
  public void shouldDeserializeSubscriptionWithTrackEnabledAndTrackMode() throws Exception {
    // given
    String json =
        "{"
            + "\"name\": \"test\", "
            + "\"topicName\": \"g1.t1\", "
            + "\"endpoint\": \"http://localhost:8888\", "
            + "\"trackingEnabled\": \"true\", "
            + "\"trackingMode\": \"discardedOnly\""
            + "}";

    // when
    Subscription subscription = mapper.readValue(json, Subscription.class);

    // then
    assertThat(subscription.isTrackingEnabled()).isTrue();
    assertThat(subscription.getTrackingMode()).isEqualTo(TrackingMode.TRACK_DISCARDED_ONLY);
  }

  @Test
  public void shouldApplyPatchToSubscriptionPolicy() {
    // given
    PatchData patch = patchData().set("rate", 8).build();

    // when
    SubscriptionPolicy subscription = subscriptionPolicy().withRate(1).applyPatch(patch).build();

    // then
    assertThat(subscription.getRate()).isEqualTo(8);
  }

  @Test
  public void shouldAnonymizePassword() {
    // given
    Subscription subscription =
        subscription("group.topic", "subscription")
            .withEndpoint("http://user:password@service/path")
            .build();

    // when & then
    assertThat(subscription.anonymize().getEndpoint())
        .isEqualTo(new EndpointAddress("http://user:*****@service/path"));
  }

  @Test
  public void shouldApplyPatchChangingSubscriptionOAuthPolicyGrantType() {
    // given
    Subscription subscription =
        subscription("group.topic", "subscription")
            .withOAuthPolicy(
                new SubscriptionOAuthPolicy(CLIENT_CREDENTIALS, "myProvider", "repo", null, null))
            .build();
    PatchData oAuthPolicyPatchData =
        patchData()
            .set("grantType", SubscriptionOAuthPolicy.GrantType.USERNAME_PASSWORD.getName())
            .set("username", "user1")
            .set("password", "abc123")
            .build();
    PatchData patch = patchData().set("oAuthPolicy", oAuthPolicyPatchData).build();

    // when
    Subscription updated = Patch.apply(subscription, patch);

    // then
    SubscriptionOAuthPolicy updatedPolicy = updated.getOAuthPolicy();
    assertThat(updatedPolicy.getGrantType()).isEqualTo(USERNAME_PASSWORD);
    assertThat(updatedPolicy.getUsername()).isEqualTo("user1");
  }

  @Test
  public void shouldReadIntBackoffMultiplier() throws Exception {
    // given
    String json =
        "{\"name\": \"test\", \"endpoint\": \"http://localhost:8888\", \"subscriptionPolicy\": {\"messageBackoff\": 1000, \"backoffMultiplier\": 3}}";

    // when
    Subscription subscription = mapper.readValue(json, Subscription.class);

    // then
    assertThat(subscription.getSerialSubscriptionPolicy().getBackoffMultiplier()).isEqualTo(3);
  }
}
