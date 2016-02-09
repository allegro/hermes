package pl.allegro.tech.hermes.test.helper.endpoint;

import pl.allegro.tech.hermes.api.endpoints.TopicEndpoint;
import pl.allegro.tech.hermes.test.helper.client.Hermes;

import javax.ws.rs.core.Response;


public class HermesPublisher {

    private final TopicEndpoint topicEndpoint;

    public HermesPublisher(String hermesFrontendUrl) {
        Hermes hermes = hermes(hermesFrontendUrl);
        this.topicEndpoint = hermes.createTopicEndpoint();
    }
    
    public Response publish(String qualifiedTopicName, String message) {
        return topicEndpoint.publishMessage(qualifiedTopicName, message);
    }

    public Response publish(String qualifiedTopicName, byte[] message) {
        return topicEndpoint.publishMessage(qualifiedTopicName, message);
    }

    private Hermes hermes(String hermesFrontendUrl) {
        return new Hermes(hermesFrontendUrl)
                .withManagementConfig(JerseyClientFactory.createConfig())
                .withPublisherConfig(JerseyClientFactory.createConfig());
    }
}
