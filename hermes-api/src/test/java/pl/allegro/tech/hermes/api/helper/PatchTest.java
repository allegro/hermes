package pl.allegro.tech.hermes.api.helper;

import org.junit.Test;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.helpers.Patch;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class PatchTest {

    @Test
    public void shouldApplyPatch() {
        // given
        SubscriptionPolicy policy = SubscriptionPolicy.create(new HashMap<>());
        PatchData patch = patchData().set("rate", 10).set("messageTtl", 30).build();

        // when
        SubscriptionPolicy patched = Patch.apply(policy, patch);

        // when & then
        assertThat(patched.getRate()).isEqualTo(10);
        assertThat(patched.getMessageTtl()).isEqualTo(30);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionForNullValues() {
        // when
        Patch.apply(null, null);

        // then
        // exception is thrown
    }

    @Test
    public void shouldIgnoreUnknownFields() {
        //given
        SubscriptionPolicy policy = subscriptionPolicy().withRate(10).build();
        PatchData patch = patchData().set("unknown", 10).set("messageTtl", 30).build();

        // when
        SubscriptionPolicy patched = Patch.apply(policy, patch);

        // then
        assertThat(patched.getMessageTtl()).isEqualTo(30);
    }

    @Test
    public void shouldPatchNestedObjects() {
        // given
        Subscription subscription = subscription("group.topic", "sub").build();
        PatchData patch = patchData().set(
                "subscriptionPolicy", patchData().set("rate", 200).set("messageTtl", 8).build().getPatch()
        ).build();

        // when
        SubscriptionPolicy result = Patch.apply(subscription, patch).getSerialSubscriptionPolicy();

        // then
        assertThat(result.getMessageTtl()).isEqualTo(8);
        assertThat(result.getRate()).isEqualTo(200);
    }

    @Test
    public void shouldNotResetPrimitiveFields() {
        // given
        Topic topic = topic("group.topic").withTrackingEnabled(true).build();
        PatchData patch = patchData().set("schemaVersionAwareSerializationEnabled", true).build();

        // when
        Topic patched = Patch.apply(topic, patch);

        // then
        assertThat(patched.isTrackingEnabled()).isTrue();
        assertThat(patched.isSchemaVersionAwareSerializationEnabled()).isTrue();
    }
}
