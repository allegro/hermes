package pl.allegro.tech.hermes.test.helper.endpoint;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.endpoints.*;
import pl.allegro.tech.hermes.test.helper.client.Hermes;

import java.util.List;

public class HermesEndpoints {

    private final GroupEndpoint groupEndpoint;

    private final TopicEndpoint topicEndpoint;

    private final SubscriptionEndpoint subscriptionEndpoint;

    private final SchemaEndpoint schemaEndpoint;

    private final QueryEndpoint queryEndpoint;

    private final OAuthProviderEndpoint oAuthProviderEndpoint;

    public HermesEndpoints(Hermes hermes) {
        this.groupEndpoint = hermes.createGroupEndpoint();
        this.topicEndpoint = hermes.createTopicEndpoint();
        this.subscriptionEndpoint = hermes.createSubscriptionEndpoint();
        this.schemaEndpoint = hermes.createSchemaEndpoint();
        this.queryEndpoint = hermes.createQueryEndpoint();
        this.oAuthProviderEndpoint = hermes.createOAuthProviderEndpoint();
    }

    public HermesEndpoints(String hermesFrontendUrl) {
        this(createHermesFromUrl(hermesFrontendUrl));
    }

    private static Hermes createHermesFromUrl(String hermesFrontendUrl) {
        return new Hermes(hermesFrontendUrl)
                .withManagementConfig(JerseyClientFactory.createConfig())
                .withPublisherConfig(JerseyClientFactory.createConfig());
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

    public SchemaEndpoint schema() {
        return schemaEndpoint;
    }

    public QueryEndpoint query() {
        return queryEndpoint;
    }

    public List<String> findTopics(Topic topic, boolean tracking) {
        return topicEndpoint.list(topic.getName().getGroupName(), tracking);
    }

    public List<String> findSubscriptions(String group, String topic, boolean tracked) {
        return subscriptionEndpoint.list(group + "." + topic, tracked);
    }

    public OAuthProviderEndpoint oAuthProvider() {
        return oAuthProviderEndpoint;
    }
}

