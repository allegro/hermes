package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DistributedZookeeperTopicRepository extends DistributedZookeeperRepository implements TopicRepository{

    private final ZookeeperPaths paths;
    private final ZookeeperCommandExecutor commandExecutor;
    private final ZookeeperCommandFactory commandFactory;
    private final TopicPreconditions topicPreconditions;
    private final GroupPreconditions groupPreconditions;

    public DistributedZookeeperTopicRepository(ZookeeperClientManager clientManager,
                                               ZookeeperCommandExecutor commandExecutor,
                                               ZookeeperCommandFactory commandFactory,
                                               ZookeeperPaths paths,
                                               ObjectMapper mapper) {
        super(clientManager, mapper);
        this.commandExecutor = commandExecutor;
        this.paths = paths;
        this.commandFactory = commandFactory;
        this.topicPreconditions = new TopicPreconditions(paths);
        this.groupPreconditions = new GroupPreconditions(paths);
    }

    @Override
    public boolean topicExists(TopicName topicName) {
        ZookeeperClient client = clientManager.getLocalClient();

        return client.pathExists(paths.topicPath(topicName));
    }

    @Override
    public void ensureTopicExists(TopicName topicName) {
        ZookeeperClient client = clientManager.getLocalClient();

        topicPreconditions.ensureTopicExists(client, topicName);
    }

    @Override
    public List<String> listTopicNames(String groupName) {
        ZookeeperClient client = clientManager.getLocalClient();

        return listTopicNames(client, groupName);
    }

    private List<String> listTopicNames(ZookeeperClient client, String groupName) {
        groupPreconditions.ensureGroupExists(client, groupName);

        return client.childrenOf(paths.topicsPath(groupName));
    }

    @Override
    public List<Topic> listTopics(String groupName) {
        ZookeeperClient client = clientManager.getLocalClient();

        return listTopicNames(client, groupName).stream()
                .map(name -> getTopicDetails(client, new TopicName(groupName, name), true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public void createTopic(Topic topic) {
        ZookeeperCommand command = commandFactory.createTopic(topic);
        executeWithErrorHandling(commandExecutor, command);
    }

    @Override
    public void removeTopic(TopicName topicName) {
        ZookeeperCommand command = commandFactory.removeTopic(topicName);
        executeWithErrorHandling(commandExecutor, command);
    }

    @Override
    public void updateTopic(Topic topic) {
        ZookeeperCommand command = commandFactory.updateTopic(topic);
        executeWithErrorHandling(commandExecutor, command);
    }

    @Override
    public void touchTopic(TopicName topicName) {
        ZookeeperCommand command = commandFactory.touchTopic(topicName);
        executeWithErrorHandling(commandExecutor, command);
    }

    @Override
    public Topic getTopicDetails(TopicName topicName) {
        ZookeeperClient client = clientManager.getLocalClient();

        return getTopicDetails(client, topicName, false).orElse(null);
    }

    @Override
    public boolean isSubscribingRestricted(TopicName topicName) {
        return getTopicDetails(topicName).isSubscribingRestricted();
    }

    private Optional<Topic> getTopicDetails(ZookeeperClient client, TopicName topicName, boolean quiet) {
        topicPreconditions.ensureTopicExists(client, topicName);

        String topicPath = paths.topicPath(topicName);
        return client.readFrom(topicPath, (data) -> mapper.readValue(data, Topic.class), quiet);
    }
}
