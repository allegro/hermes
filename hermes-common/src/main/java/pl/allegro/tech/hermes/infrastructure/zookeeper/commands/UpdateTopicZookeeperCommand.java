package pl.allegro.tech.hermes.infrastructure.zookeeper.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.infrastructure.zookeeper.TopicPreconditions;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;

class UpdateTopicZookeeperCommand extends ZookeeperCommand {
    private static final Logger logger = LoggerFactory.getLogger(UpdateTopicZookeeperCommand.class);

    private final Topic topic;
    private final ObjectMapper mapper;
    private final TopicPreconditions preconditions;

    private byte[] topicDataBackup;

    UpdateTopicZookeeperCommand(Topic topic, ZookeeperPaths paths, ObjectMapper mapper) {
        super(paths);
        this.topic = topic;
        this.mapper = mapper;
        this.preconditions = new TopicPreconditions(paths);
    }

    @Override
    public void backup(ZookeeperClient client) {
        String topicPath = paths.topicPath(topic.getName());
        topicDataBackup = client.getData(topicPath);
    }

    @Override
    public void execute(ZookeeperClient client) {
        preconditions.ensureTopicExists(client, topic.getName());

        logger.info("Updating topic {} via client {}", topic.getName(), client.getName());

        String topicPath = paths.topicPath(topic.getName());
        client.setData(topicPath, marshall(mapper, topic));
    }

    @Override
    public void rollback(ZookeeperClient client) {
        String topicPath = paths.topicPath(topic.getName());
        client.setData(topicPath, topicDataBackup);
    }
}
