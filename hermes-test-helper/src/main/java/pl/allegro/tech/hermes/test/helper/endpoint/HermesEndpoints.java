package pl.allegro.tech.hermes.test.helper.endpoint;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.endpoints.AllTopicClientsEndpoint;
import pl.allegro.tech.hermes.api.endpoints.BlacklistEndpoint;
import pl.allegro.tech.hermes.api.endpoints.FilterEndpoint;
import pl.allegro.tech.hermes.api.endpoints.GroupEndpoint;
import pl.allegro.tech.hermes.api.endpoints.ModeEndpoint;
import pl.allegro.tech.hermes.api.endpoints.OAuthProviderEndpoint;
import pl.allegro.tech.hermes.api.endpoints.OfflineRetransmissionEndpoint;
import pl.allegro.tech.hermes.api.endpoints.OwnerEndpoint;
import pl.allegro.tech.hermes.api.endpoints.QueryEndpoint;
import pl.allegro.tech.hermes.api.endpoints.ReadinessEndpoint;
import pl.allegro.tech.hermes.api.endpoints.SchemaEndpoint;
import pl.allegro.tech.hermes.api.endpoints.SubscriptionEndpoint;
import pl.allegro.tech.hermes.api.endpoints.SubscriptionOwnershipEndpoint;
import pl.allegro.tech.hermes.api.endpoints.TopicEndpoint;
import pl.allegro.tech.hermes.api.endpoints.UnhealthyEndpoint;
import pl.allegro.tech.hermes.consumers.ConsumerEndpoint;
import pl.allegro.tech.hermes.test.helper.client.Hermes;

import java.util.List;

public class HermesEndpoints {

    private final GroupEndpoint groupEndpoint;

    private final TopicEndpoint topicEndpoint;

    private final SubscriptionEndpoint subscriptionEndpoint;

    private final SubscriptionOwnershipEndpoint subscriptionOwnershipEndpoint;

    private final AllTopicClientsEndpoint allTopicClientsEndpoint;

    private final SchemaEndpoint schemaEndpoint;

    private final QueryEndpoint queryEndpoint;

    private final OAuthProviderEndpoint oAuthProviderEndpoint;

    private final ConsumerEndpoint consumerEndpoint;

    private final OwnerEndpoint ownerEndpoint;

    private final BlacklistEndpoint blacklistEndpoint;

    private final UnhealthyEndpoint unhealthyEndpoint;

    private final ModeEndpoint modeEndpoint;

    private final FilterEndpoint filterEndpoint;

    private final ReadinessEndpoint readinessEndpoint;

    private final OfflineRetransmissionEndpoint offlineRetransmissionEndpoint;

    public HermesEndpoints(Hermes hermes) {
        this.groupEndpoint = hermes.createGroupEndpoint();
        this.topicEndpoint = hermes.createTopicEndpoint();
        this.subscriptionEndpoint = hermes.createSubscriptionEndpoint();
        this.subscriptionOwnershipEndpoint = hermes.createSubscriptionOwnershipEndpoint();
        this.schemaEndpoint = hermes.createSchemaEndpoint();
        this.queryEndpoint = hermes.createQueryEndpoint();
        this.blacklistEndpoint = hermes.createBlacklistEndpoint();
        this.oAuthProviderEndpoint = hermes.createOAuthProviderEndpoint();
        this.consumerEndpoint = hermes.createConsumerEndpoint();
        this.ownerEndpoint = hermes.createOwnerEndpoint();
        this.unhealthyEndpoint = hermes.unhealthyEndpoint();
        this.modeEndpoint = hermes.modeEndpoint();
        this.filterEndpoint = hermes.createFilterEndpoint();
        this.readinessEndpoint = hermes.createReadinessEndpoint();
        this.offlineRetransmissionEndpoint = hermes.createOfflineRetransmissionEndpoint();
        this.allTopicClientsEndpoint = hermes.createAllTopicClientsEndpoint();
    }

    public HermesEndpoints(String hermesFrontendUrl, String consumerUrl) {
        this(createHermesFromUrl(hermesFrontendUrl, consumerUrl));
    }

    private static Hermes createHermesFromUrl(String hermesFrontendUrl, String consumerUrl) {
        return new Hermes(hermesFrontendUrl, consumerUrl)
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

    public SubscriptionOwnershipEndpoint subscriptionOwnershipEndpoint() {
        return subscriptionOwnershipEndpoint;
    }

    public AllTopicClientsEndpoint allTopicClientsEndpoint() {
        return allTopicClientsEndpoint;
    }

    public SchemaEndpoint schema() {
        return schemaEndpoint;
    }

    public QueryEndpoint query() {
        return queryEndpoint;
    }

    public OwnerEndpoint owner() {
        return ownerEndpoint;
    }

    public UnhealthyEndpoint unhealthyEndpoint() {
        return unhealthyEndpoint;
    }

    public BlacklistEndpoint blacklist() {
        return blacklistEndpoint;
    }

    public ModeEndpoint modeEndpoint() {
        return modeEndpoint;
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

    public ConsumerEndpoint consumer() {
        return consumerEndpoint;
    }

    public FilterEndpoint filter() {
        return filterEndpoint;
    }

    public ReadinessEndpoint readiness() {
        return readinessEndpoint;
    }

    public OfflineRetransmissionEndpoint offlineRetransmission() {
        return offlineRetransmissionEndpoint;
    }
}

