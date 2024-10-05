package pl.allegro.tech.hermes.common.message.undelivered;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;

class UndeliveredMessagePaths {

  private static final String NODE_NAME = "undelivered";

  private final ZookeeperPaths zookeeperPaths;

  UndeliveredMessagePaths(ZookeeperPaths zookeeperPaths) {
    this.zookeeperPaths = zookeeperPaths;
  }

  String buildPath(TopicName topicName, String subscriptionName) {
    return zookeeperPaths.subscriptionPath(topicName, subscriptionName, NODE_NAME);
  }
}
