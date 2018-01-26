package pl.allegro.tech.hermes.infrastructure.zookeeper;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;

public class SubscriptionPreconditions {

    private final ZookeeperPaths paths;

    public SubscriptionPreconditions(ZookeeperPaths paths) {
        this.paths = paths;
    }

    public void ensureSubscriptionExists(ZookeeperClient client, TopicName topicName, String subscriptionName) {
        if (!client.pathExists(paths.subscriptionPath(topicName, subscriptionName))) {
            throw new SubscriptionNotExistsException(topicName, subscriptionName);
        }
    }
}
