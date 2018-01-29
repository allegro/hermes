package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.TopicPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class TouchTopicZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(TouchTopicZookeeperCommand.class);

    private final TopicName topicName;
    private final TopicPreconditions preconditions;

    TouchTopicZookeeperCommand(TopicName topicName, ZookeeperPaths paths) {
        super(paths);
        this.topicName = topicName;
        this.preconditions = new TopicPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {}

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureTopicExists(client, topicName);

        logger.info("Touching topic '{}' via client '{}'", topicName.qualifiedName(), client.getName());

        String topicPath = paths.topicPath(topicName);
        client.touch(topicPath);
    }

    @Override
    public void rollback(ZookeeperClient client) {}
}
