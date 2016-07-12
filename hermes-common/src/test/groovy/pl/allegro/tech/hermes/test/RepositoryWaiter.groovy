package pl.allegro.tech.hermes.test

import org.apache.curator.framework.CuratorFramework
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

class RepositoryWaiter extends ZookeeperWaiter {

    private final ZookeeperPaths paths

    RepositoryWaiter(CuratorFramework zookeeper, ZookeeperPaths paths) {
        super(zookeeper)
        this.paths =  paths
    }

    void untilOAuthProviderCreated(String oAuthProviderName) {
        untilZookeeperPathIsCreated(paths.oAuthProviderPath(oAuthProviderName))
    }

    void untilGroupCreated(String groupName) {
        untilZookeeperPathIsCreated(paths.groupPath(groupName))
    }

    void untilTopicCreated(String groupName, String topicName) {
        untilZookeeperPathIsCreated(paths.topicPath(new TopicName(groupName, topicName)))
    }

    void untilSubscriptionCreated(TopicName topic, String subscription) {
        untilZookeeperPathIsCreated(paths.subscriptionPath(topic, subscription))
    }
}
