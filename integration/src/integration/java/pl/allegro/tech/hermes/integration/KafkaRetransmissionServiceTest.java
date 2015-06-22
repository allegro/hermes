package pl.allegro.tech.hermes.integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.integration.env.HermesIntegrationEnvironment;
import pl.allegro.tech.hermes.integration.helper.Waiter;
import pl.allegro.tech.hermes.integration.shame.Unreliable;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static pl.allegro.tech.hermes.integration.env.SharedServices.services;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.message.TestMessage.simpleMessages;

public class KafkaRetransmissionServiceTest extends HermesIntegrationEnvironment {

    private HermesEndpoints endpoints;
    private HermesPublisher publisher;
    private HermesAPIOperations operations;
    private RemoteServiceEndpoint remoteService;
    private Waiter wait;

    @BeforeClass
    public void initialize() {
        publisher = new HermesPublisher(FRONTEND_URL);
        endpoints = new HermesEndpoints(MANAGEMENT_ENDPOINT_URL);
        operations = new HermesAPIOperations(endpoints);
        wait = new Waiter(endpoints, services().zookeeper(), services().kafkaZookeeper());
    }

    @BeforeMethod
    public void initializeAlways() {
        remoteService = new RemoteServiceEndpoint(services().serviceMock());
    }

    @Test(enabled = false)
    @Unreliable
    public void shouldMoveOffsetNearGivenTimestamp() throws InterruptedException {
        // given
        TopicName topicName = new TopicName("resetOffsetGroup", "topic");
        String subscription = "subscription";

        operations.buildSubscription(topicName, subscription, HTTP_ENDPOINT_URL);
        wait.untilSubscriptionIsCreated(topicName, subscription);
        remoteService.expectMessages(simpleMessages(6));

        sendMessagesOnTopic(topicName.qualifiedName(), 4);
        Thread.sleep(1000); //wait 1s because our date time format has seconds precision
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Thread.sleep(1000);
        sendMessagesOnTopic(topicName.qualifiedName(), 2);
        remoteService.waitUntilReceived();
        wait.untilConsumerCommitsOffset();

        // when
        remoteService.expectMessages(simpleMessages(2));
        Response response = endpoints.subscription().retransmit(topicName.qualifiedName(), subscription, dateTime);
        wait.untilSubscriptionEndsReiteration(topicName, subscription);

        // then
        assertThat(response).hasStatus(Response.Status.OK);
        remoteService.waitUntilReceived();
    }

    private void sendMessagesOnTopic(String qualifiedName, int n) {
        remoteService.expectMessages(simpleMessages(n));
        for (TestMessage message: simpleMessages(n)) {
            publisher.publish(qualifiedName, message.body());
        }
        remoteService.waitUntilReceived();
    }
}

