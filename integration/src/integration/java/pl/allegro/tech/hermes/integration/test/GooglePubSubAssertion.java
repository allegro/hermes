package pl.allegro.tech.hermes.integration.test;

import com.google.pubsub.v1.PubsubMessage;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class GooglePubSubAssertion extends AbstractAssert<GooglePubSubAssertion, PubsubMessage> {

    GooglePubSubAssertion(PubsubMessage actual) {
        super(actual, GooglePubSubAssertion.class);
    }

    public GooglePubSubAssertion hasAttribute(String name) {
        assertThat(actual.getAttributesMap()).containsKey(name);
        return this;
    }

    public GooglePubSubAssertion hasBody(String body) {
        assertThat(actual.getData().toStringUtf8()).isEqualTo(body);
        return this;
    }
}
