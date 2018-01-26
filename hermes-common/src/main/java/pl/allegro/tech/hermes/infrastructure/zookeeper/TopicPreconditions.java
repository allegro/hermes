package pl.allegro.tech.hermes.infrastructure.zookeeper;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicNotEmptyException;
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;

import java.util.List;

public class TopicPreconditions {

    private final ZookeeperPaths paths;

    public TopicPreconditions(ZookeeperPaths paths) {
        this.paths = paths;
    }

    public void ensureTopicExists(ZookeeperClient client, TopicName topicName) {
        if(!client.pathExists(paths.topicPath(topicName))) {
            throw new TopicNotExistsException(topicName);
        }
    }

    public void ensureTopicIsEmpty(ZookeeperClient client, TopicName topicName) {
        String subscriptionsPath = paths.subscriptionsPath(topicName);
        List<String> children = client.childrenOf(subscriptionsPath);
        boolean anyNodeNotEmpty = children.stream()
                .anyMatch(sub -> !client.isPathEmpty(subscriptionsPath + "/" + sub));
        if (!children.isEmpty() && anyNodeNotEmpty) {
            throw new TopicNotEmptyException(topicName);
        }
    }
}
