package pl.allegro.tech.hermes.integration.test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.pubsub.v1.PubsubMessage;
import org.assertj.core.api.Assertions;

import javax.jms.Message;
import javax.ws.rs.core.Response;

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
