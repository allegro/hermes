package pl.allegro.tech.hermes.mock;

import groovy.json.JsonOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import static org.assertj.core.api.Assertions.assertThat;

class HermesMockExtensionTest {

    private static final int port = Ports.nextAvailable();

    @RegisterExtension
    static final HermesMockExtension hermes = new HermesMockExtension(port);

    private HermesPublisher publisher;

    @BeforeEach
    void setup() {
        publisher = new HermesPublisher(String.format("http://localhost:%d", port));
    }

    @Test
    void shouldPublishMessage() {
        // given
        String topic = "first-sample-topic";
        hermes.define().jsonTopic(topic);

        // when
        publishMessage(topic);

        // then
        hermes.expect().singleMessageOnTopic(topic);
    }

    @Test
    void shouldInjectHermesMock(HermesMock hermesMock) {
        // given
        String topic = "second-sample-topic";
        hermes.define().jsonTopic(topic);

        // when
        publishMessage(topic);

        // then
        assertThat(hermesMock.query().allRequests()).hasSize(1);
        assertThat(hermesMock.query().allRequestsOnTopic(topic)).hasSize(1);
    }

    private void publishMessage(String topic) {
        String body = JsonOutput.toJson(TestMessage.random());
        publisher.publish(topic, body);
    }
}
