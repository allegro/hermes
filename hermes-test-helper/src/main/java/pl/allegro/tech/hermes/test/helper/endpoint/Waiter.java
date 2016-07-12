package pl.allegro.tech.hermes.test.helper.endpoint;

import com.jayway.awaitility.Duration;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class Waiter {

    private final HermesEndpoints endpoints;

    public Waiter(HermesEndpoints endpoints) {
        this.endpoints = endpoints;
    }

    public void untilSubscriptionCreated(Topic topic, Subscription subscription) {
        untilSubscriptionCreated(topic, subscription.getName(), subscription.isTrackingEnabled());
    }

    public void untilSubscriptionCreated(Topic topic, String subscription, boolean isTracked) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() ->
            endpoints.subscription().list(topic.getQualifiedName(), isTracked).contains(subscription)
        );
    }

    public void untilGroupCreated(String group) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() ->
            endpoints.group().list().contains(group)
        );
    }

    public void untilTopicCreated(Topic topic) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() ->
            endpoints.findTopics(topic, topic.isTrackingEnabled()).contains(topic.getQualifiedName())
        );
    }

    public void untilTopicUpdated(Topic reference) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() ->
                endpoints.topic().get(reference.getQualifiedName()).equals(reference)
        );
    }

    public void untilOAuthProviderCreated(String oAuthProviderName) {
        waitAtMost(adjust(Duration.ONE_MINUTE)).until(() ->
                endpoints.oAuthProvider().get(oAuthProviderName) != null
        );
    }
}

