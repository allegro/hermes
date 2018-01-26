package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.TopicAlreadyExistsException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.GroupPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class CreateTopicZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(CreateTopicZookeeperCommand.class);

    private final Topic topic;
    private final ObjectMapper mapper;
    private final GroupPreconditions preconditions;

    CreateTopicZookeeperCommand(Topic topic, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.topic = topic;
        this.mapper = mapper;
        this.preconditions = new GroupPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {}

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureGroupExists(client, topic.getName().getGroupName());

        String topicPath = paths.topicPath(topic.getName());
        String subscriptionsPath = paths.subscriptionsPath(topic.getName());

        logger.info("Creating topic for path {} via client {}", topicPath, client.getName());

        CuratorFramework curator = client.getCuratorFramework();
        try {
            curator.transaction().forOperations(
                    curator.transactionOp().create().forPath(topicPath, mapper.writeValueAsBytes(topic)),
                    curator.transactionOp().create().forPath(subscriptionsPath)
            );
        } catch (KeeperException.NodeExistsException e) {
            throw new TopicAlreadyExistsException(topic.getName(), e);
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @Override
    public void rollback(ZookeeperClient client) {
        String topicPath = paths.topicPath(topic.getName());
        client.deleteWithChildren(topicPath);
    }
}
