package pl.allegro.tech.hermes.test.helper.endpoint;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import pl.allegro.tech.hermes.api.endpoints.TopicEndpoint;
import pl.allegro.tech.hermes.test.helper.client.Hermes;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;


public class HermesPublisher {

    private final TopicEndpoint topicEndpoint;

    public HermesPublisher(String hermesFrontendUrl) {
        Hermes hermes = hermes(hermesFrontendUrl);
        this.topicEndpoint = hermes.createTopicEndpoint();
    }
    
    public Response publish(String qualifiedTopicName, String message) {
        return topicEndpoint.publishMessage(qualifiedTopicName, message);
    }

    public Response publish(String qualifiedTopicName, String message, Response.Status expectedStatus) {
        Response response = topicEndpoint.publishMessage(qualifiedTopicName, message);
        assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());

        return response;
    }

    public Response publish(String qualifiedTopicName, byte[] message) {
        return topicEndpoint.publishMessage(qualifiedTopicName, message);
    }

    public Response publish(String qualifiedTopicName, byte[] message, Response.Status expectedStatus) {
        Response response = topicEndpoint.publishMessage(qualifiedTopicName, message);
        assertThat(response.getStatus()).isEqualTo(expectedStatus.getStatusCode());

        return response;
    }
    
    private Hermes hermes(String hermesFrontendUrl) {
        return new Hermes(hermesFrontendUrl).withManagementConfig(integrationTestsConfig()).withPublisherConfig(integrationTestsConfig());
    }

    private static ClientConfig integrationTestsConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 10);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        clientConfig.property(ClientProperties.READ_TIMEOUT, 5000);
        return clientConfig;
    }
    
}
