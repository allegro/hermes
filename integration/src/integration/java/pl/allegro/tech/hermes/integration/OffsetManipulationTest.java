package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.integration.env.HermesIntegrationEnvironment;
import pl.allegro.tech.hermes.integration.env.SharedServices;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.integration.helper.Waiter;

import static pl.allegro.tech.hermes.integration.env.SharedServices.services;

public class OffsetManipulationTest extends HermesIntegrationEnvironment {

    private HermesPublisher publisher;

    private HermesAPIOperations operations;

    private RemoteServiceEndpoint remoteService;

    private Waiter wait;

    private HermesConsumers consumers;

    @BeforeClass
    public void initialize() {
        this.publisher = new HermesPublisher(FRONTEND_URL);
        HermesEndpoints endpoints = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL, CONSUMER_ENDPOINT_URL);
        this.wait = new Waiter(endpoints, SharedServices.services().zookeeper(), services().kafkaZookeeper(), KAFKA_NAMESPACE);
        this.operations = new HermesAPIOperations(endpoints, wait);
        this.consumers = SharedServices.services().consumers();
    }

    @BeforeMethod
    public void initializeAlways() {
        this.remoteService = new RemoteServiceEndpoint(SharedServices.services().serviceMock());
    }

    @Test(enabled = false)
    public void shouldReadUncommitedOffsetAfterConsumerStart() {
        // given
        Topic topic = operations.buildTopic("uncommitedOffsetGroup", "topic");
        operations.createSubscription(topic, "subscription", HTTP_ENDPOINT_URL);

        consumers.stop();
        wait.untilConsumersStop();

        remoteService.expectMessages(TestMessage.simple().body());
        publisher.publish(topic.getQualifiedName(), TestMessage.simple().body());

        // when
        consumers.start();
        wait.untilConsumersStart();

        // then this might take a while because consumers need to start
        remoteService.waitUntilReceived(60);
    }
}
