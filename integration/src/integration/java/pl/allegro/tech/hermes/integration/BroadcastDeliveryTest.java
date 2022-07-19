package pl.allegro.tech.hermes.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.test.helper.endpoint.RemoteServiceEndpoint;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class BroadcastDeliveryTest extends IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(BroadcastDeliveryTest.class);

    private List<RemoteServiceEndpoint> remoteServices;
    private RemoteServiceEndpoint firstRemoteService;

    @AfterMethod
    public void cleanup() {
        this.remoteServices.forEach(service -> {
            try {
                service.stop();
            } catch (Exception ex) {
                logger.warn("Failed to stop remote service.", ex);
            }
        });
    }

    @Test
    public void shouldPublishAndConsumeMessageByAllServices() {
        // given
        String endpointUrl = setUpServicesAndGetEndpoint();
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "broadcastTopic").build());
        operations.createBroadcastSubscription(topic, "broadcastSubscription", endpointUrl);

        TestMessage message = TestMessage.random();
        remoteServices.forEach(remoteService -> remoteService.expectMessages(message.body()));

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteServices.forEach(RemoteServiceEndpoint::waitUntilReceived);
    }

    @Test
    public void shouldPublishAndRetryOnlyForUndeliveredConsumers() {
        // given
        String endpointUrl = setUpServicesAndGetEndpoint();
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "broadcastTopic2").build());
        operations.createBroadcastSubscription(topic, "broadcastSubscription2", endpointUrl);

        TestMessage message = TestMessage.random();
        firstRemoteService.retryMessage(message.body(), 1);
        remoteServices.stream().skip(1).forEach(remoteService -> remoteService.expectMessages(message.body()));

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteServices.stream().skip(1).forEach(RemoteServiceEndpoint::waitUntilReceived);
        firstRemoteService.waitUntilReceived(60, 2);
    }

    @Test
    public void shouldNotRetryForBadRequestsFromConsumers() {
        // given
        String endpointUrl = setUpServicesAndGetEndpoint();
        Topic topic = operations.buildTopic(randomTopic("publishAndConsumeGroup", "broadcastTopic3").build());
        operations.createBroadcastSubscription(topic, "broadcastSubscription3", endpointUrl);

        TestMessage message = TestMessage.random();
        firstRemoteService.setReturnedStatusCode(400);
        remoteServices.forEach(remoteService -> remoteService.expectMessages(message.body()));

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteServices.forEach(service -> service.waitUntilReceived(5));
    }

    private RemoteServiceEndpoint createRemoteServiceEndpoint() {
        WireMockServer service = new WireMockServer(Ports.nextAvailable());
        service.start();
        return new RemoteServiceEndpoint(service);
    }


    private String setUpServicesAndGetEndpoint() {
        remoteServices = Stream.generate(this::createRemoteServiceEndpoint).limit(4).collect(toList());
        firstRemoteService = remoteServices.get(0);

        return this.remoteServices.stream().map(RemoteServiceEndpoint::getUrl).map(Object::toString).collect(joining(";"));
    }

}
