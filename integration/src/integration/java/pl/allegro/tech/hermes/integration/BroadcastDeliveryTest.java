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
        Topic topic = operations.buildTopic("publishAndConsumeGroup", "broadcastTopic");
        operations.createBroadcastSubscription(topic, "broadcastSubscription", endpointUrl);

        TestMessage message = TestMessage.random();
        remoteServices.stream().forEach(remoteService -> remoteService.expectMessages(message.body()));

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteServices.stream().forEach(service -> service.waitUntilReceived());
    }

    @Test
    public void shouldPublishAndRetryOnlyForUndeliveredConsumers() {
        // given
        String endpointUrl = setUpServicesAndGetEndpoint();
        Topic topic = operations.buildTopic("publishAndConsumeGroup", "broadcastTopic2");
        operations.createBroadcastSubscription(topic, "broadcastSubscription2", endpointUrl);

        TestMessage message = TestMessage.random();
        firstRemoteService.retryMessage(message.body(), 1);
        remoteServices.stream().skip(1).forEach(remoteService -> remoteService.expectMessages(message.body()));

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteServices.stream().skip(1).forEach(service -> service.waitUntilReceived());
        firstRemoteService.waitUntilReceived(60, 2);
    }

    @Test
    public void shouldNotRetryForBadRequestsFromConsumers() {
        // given
        String endpointUrl = setUpServicesAndGetEndpoint();
        Topic topic = operations.buildTopic("publishAndConsumeGroup", "broadcastTopic3");
        operations.createBroadcastSubscription(topic, "broadcastSubscription3", endpointUrl);

        TestMessage message = TestMessage.random();
        firstRemoteService.setReturnedStatusCode(400);
        remoteServices.stream().forEach(remoteService -> remoteService.expectMessages(message.body()));

        // when
        publisher.publish(topic.getQualifiedName(), message.body());

        // then
        remoteServices.stream().forEach(service -> service.waitUntilReceived(5));
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
