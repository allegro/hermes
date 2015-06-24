package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.integration.env.ConsumersStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.integration.shame.Unreliable;

import static pl.allegro.tech.hermes.integration.env.SharedServices.services;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class OffsetCommitTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(services().serviceMock());
    }

    @Unreliable
    @Test(enabled = false)
    public void shouldNotRetractCommit() throws Exception {
        //given
        int messages = 50;
        ConsumersStarter secondConsumer = new ConsumersStarter();

        operations.buildSubscription("properCommitOffsetGroup", "topic", "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(TestMessage.simple().body());

        //when
        for (int i = 0; i < messages; i++) {
            publisher.publish("properCommitOffsetGroup.topic", TestMessage.simple().body());
        }
        wait.untilConsumerCommitsOffset();
        secondConsumer.start();
        wait.untilConsumersRebalance("properCommitOffsetGroup", "topic", "subscription", 2);

        for (int i = 0; i < messages; i++) {
            publisher.publish("properCommitOffsetGroup.topic", TestMessage.simple().body());
        }
        wait.untilAllOffsetsEqual("properCommitOffsetGroup", "topic", "subscription", messages);

        //then
        assertThat(services().zookeeper()).offsetsAreNotRetracted("properCommitOffsetGroup", "topic", "subscription", 2, messages);
        secondConsumer.stop();
    }

}
