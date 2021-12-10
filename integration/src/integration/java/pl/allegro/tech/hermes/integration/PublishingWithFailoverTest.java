package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class PublishingWithFailoverTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test
    public void shouldReturn202IfKafkaFailedToRespondButMessageCanBeBufferedInMemory() throws Exception {
        //given
        TestMessage message = TestMessage.of("hello", "world");

        Topic topic = operations.buildTopic(randomTopic("inMemory", "topic").build());
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(message.body(), message.body());
        //we must send first message to a working kafka because producer need to fetch metadata
        assertThat(publisher.publish(topic.getQualifiedName(), message.body()).getStatus()).isEqualTo(201);

        //when
        kafkaClusterOne.cutOffConnectionsBetweenBrokersAndClients();
        Response response = publisher.publish(topic.getQualifiedName(), TestMessage.simple().body());
        kafkaClusterOne.restoreConnectionsBetweenBrokersAndClients();

        //then
        assertThat(response.getStatus()).isEqualTo(202);
        remoteService.waitUntilReceived();
    }

}
