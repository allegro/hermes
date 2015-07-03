package pl.allegro.tech.hermes.test.helper.endpoint;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.endpoints.GroupEndpoint;
import pl.allegro.tech.hermes.api.endpoints.SubscriptionEndpoint;
import pl.allegro.tech.hermes.api.endpoints.TopicEndpoint;
import pl.allegro.tech.hermes.test.helper.client.Hermes;

import java.util.List;

public class HermesEndpoints {

    private final GroupEndpoint groupEndpoint;

    private final TopicEndpoint topicEndpoint;

    private final SubscriptionEndpoint subscriptionEndpoint;

    public HermesEndpoints(String hermesFrontendUrl) {
        Hermes hermes = new Hermes(hermesFrontendUrl)
                .withManagementConfig(integrationTestsConfig())
                .withPublisherConfig(integrationTestsConfig());
        this.groupEndpoint = hermes.createGroupEndpoint();
        this.topicEndpoint = hermes.createTopicEndpoint();
        this.subscriptionEndpoint = hermes.createSubscriptionEndpoint();
    }

    public HermesEndpoints(Hermes hermes) {
        this.groupEndpoint = hermes.createGroupEndpoint();
        this.topicEndpoint = hermes.createTopicEndpoint();
        this.subscriptionEndpoint = hermes.createSubscriptionEndpoint();
    }

    public GroupEndpoint group() {
        return groupEndpoint;
    }

    public TopicEndpoint topic() {
        return topicEndpoint;
    }

    public SubscriptionEndpoint subscription() {
        return subscriptionEndpoint;
    }

    private static ClientConfig integrationTestsConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 10);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 5000);
        clientConfig.property(ClientProperties.READ_TIMEOUT, 5000);
        return clientConfig;
    }

    public List<String> findTopics(Topic topic, boolean tracking) {
        return topicEndpoint.list(topic.getName().getGroupName(), tracking);
    }

    public List<String> findSubscriptions(String group, String topic, boolean tracked) {
        return subscriptionEndpoint.list(group + "." + topic, tracked);
    }

}

