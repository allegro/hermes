package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.integration.env.ConsumersStarter;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import static pl.allegro.tech.hermes.integration.env.SharedServices.services;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class OffsetCommitTest extends IntegrationTest {

    private RemoteServiceEndpoint remoteService;

    private KafkaNamesMapper kafkaNamesMapper = new KafkaNamesMapper(KAFKA_NAMESPACE);

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

        Topic topic = operations.buildTopic("properCommitOffsetGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);
        remoteService.expectMessages(TestMessage.simple().body());

        //when
        for (int i = 0; i < messages; i++) {
            publisher.publish("properCommitOffsetGroup.topic", TestMessage.simple().body());
        }
        wait.untilConsumerCommitsOffset();
        secondConsumer.start();
        wait.untilConsumersRebalance(topic, "subscription", 2);

        for (int i = 0; i < messages; i++) {
            publisher.publish("properCommitOffsetGroup.topic", TestMessage.simple().body());
        }
        wait.untilAllOffsetsEqual(topic, "subscription", messages);

        //then
        assertThat(services().zookeeper(), kafkaNamesMapper).offsetsAreNotRetracted(topic, "subscription", 2, messages);
        secondConsumer.stop();
    }

}
