package pl.allegro.tech.hermes.integration.test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.pubsub.v1.PubsubMessage;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.Assertions;

import javax.jms.Message;

public final class HermesAssertions extends Assertions {

    private HermesAssertions() {
    }

    public static HttpResponseAssertion assertThat(Response response) {
        return new HttpResponseAssertion(response);
    }

    public static WiremockRequestAssertion assertThat(LoggedRequest request) {
        return new WiremockRequestAssertion(request);
    }

    public static JmsMessageAssertion assertThat(Message message) {
        return new JmsMessageAssertion(message);
    }

    public static GooglePubSubAssertion assertThat(PubsubMessage message) {
        return new GooglePubSubAssertion(message);
    }
}
