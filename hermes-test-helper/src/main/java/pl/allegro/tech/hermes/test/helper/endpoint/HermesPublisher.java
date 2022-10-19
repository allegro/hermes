package pl.allegro.tech.hermes.test.helper.endpoint;

import pl.allegro.tech.hermes.api.endpoints.TopicEndpoint;
import pl.allegro.tech.hermes.test.helper.client.Hermes;

import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

public class HermesPublisher {

    private final TopicEndpoint topicEndpoint;
    private final WebTarget webTarget;

    public HermesPublisher(String hermesFrontendUrl) {
        Hermes hermes = hermes(hermesFrontendUrl);
        this.topicEndpoint = hermes.createTopicEndpoint();
        this.webTarget = hermes.createWebTargetForPublishing();
    }

    public Response publish(String qualifiedTopicName, String message) {
        return topicEndpoint.publishMessage(qualifiedTopicName, message);
    }

    public Response publish(String qualifiedTopicName, String message, Map<String, String> headers) {
        String contentType = headers.getOrDefault("Content-Type", MediaType.TEXT_PLAIN);
        return webTarget.path(qualifiedTopicName).request().headers(new MultivaluedHashMap<>(headers))
                .post(Entity.entity(message, contentType));
    }

    public Response publish(String qualifiedTopicName, byte[] message) {
        return topicEndpoint.publishMessage(qualifiedTopicName, message);
    }

    public Response publishAvro(String qualifiedTopicName, byte[] message, Map<String, String> headers) {
        return webTarget.path(qualifiedTopicName).request().headers(new MultivaluedHashMap<>(headers))
                .post(Entity.entity(message, "avro/binary"));
    }

    private Hermes hermes(String hermesFrontendUrl) {
        return new Hermes(hermesFrontendUrl, hermesFrontendUrl)
                .withManagementConfig(JerseyClientFactory.createConfig())
                .withPublisherConfig(JerseyClientFactory.createConfig());
    }
}
