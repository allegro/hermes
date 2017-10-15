package pl.allegro.tech.hermes.integration.test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;

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
}
