package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.TopicPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class RemoveTopicZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(RemoveTopicZookeeperCommand.class);

    private final TopicName topicName;
    private final TopicPreconditions preconditions;

    private byte[] topicDataBackup;

    RemoveTopicZookeeperCommand(TopicName topicName, ZookeeperPaths paths) {
        super(paths);
        this.topicName = topicName;
        this.preconditions = new TopicPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        String topicPath = paths.topicPath(topicName);
        topicDataBackup = client.getData(topicPath);
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureTopicExists(client, topicName);
        preconditions.ensureTopicIsEmpty(client, topicName);

        logger.info("Removing topic '{}' via client '{}'", topicName.getName(), client.getName());

        String topicPath = paths.topicPath(topicName);
        client.deleteWithChildrenWithGuarantee(topicPath);
    }

    @Override
    public void rollback(ZookeeperClient client) {
        logger.info("Rolling back changes: topic '{}' removal via client '{}'", topicName.getName(), client.getName());

        String topicPath = paths.topicPath(topicName);
        String subscriptionsPath = paths.subscriptionsPath(topicName);

        CuratorFramework curator = client.getCuratorFramework();
        try {
            curator.inTransaction()
                    .create().forPath(topicPath, topicDataBackup).and()
                    .create().forPath(subscriptionsPath).and()
                    .commit();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }
}
