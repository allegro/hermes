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

    public static ZookeeperAssertion assertThat(CuratorFramework zookeeper, KafkaNamesMapper kafkaNamesMapper) {
        return new ZookeeperAssertion(zookeeper, ZookeeperAssertion.class, kafkaNamesMapper);
    }

    public static WireMockRequestAssertion assertThat(LoggedRequest request) {
        return new WireMockRequestAssertion(request);
    }

    public static JmsMessageAssertion assertThat(Message message) {
        return new JmsMessageAssertion(message);
    }
}
