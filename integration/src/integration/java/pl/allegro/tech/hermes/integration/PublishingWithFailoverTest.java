package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.integration.shame.Unreliable;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class PublishingWithFailoverTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private KafkaStarter kafkaStarter;

    @BeforeClass
    public void initialize() {
        this.kafkaStarter = SharedServices.services().kafkaStarter();
    }

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldReturn202IfKafkaFailedToRespondButMessageCanBeBufferedInMemory() throws Exception {
        //given
        TestMessage message = TestMessage.of("hello", "world");
        TopicName topic = TopicName.fromQualifiedName("inMemory.topic");

        operations.buildSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(message.body(), message.body());
        //we must send first message to a working kafka because producer need to fetch metadata
        assertThat(publisher.publish(topic.qualifiedName(), message.body()).getStatus()).isEqualTo(201);

        //when
        kafkaStarter.stop();
        //wait for kafka shutdown
        Thread.sleep(100);
        Response response = publisher.publish(topic.qualifiedName(), TestMessage.simple().body());
        kafkaStarter.start();

        //then
        assertThat(response.getStatus()).isEqualTo(202);
        remoteService.waitUntilReceived();
    }

}
