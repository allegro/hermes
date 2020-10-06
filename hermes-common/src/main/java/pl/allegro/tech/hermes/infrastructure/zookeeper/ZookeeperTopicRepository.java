package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicAlreadyExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicNotEmptyException;
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ZookeeperTopicRepository extends ZookeeperBasedRepository implements TopicRepository {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTopicRepository.class);

    private final GroupRepository groupRepository;

    public ZookeeperTopicRepository(CuratorFramework zookeeper,
                                    ObjectMapper mapper,
                                    ZookeeperPaths paths,
                                    GroupRepository groupRepository) {
        super(zookeeper, mapper, paths);
        this.groupRepository = groupRepository;
    }


    @Override
    public boolean topicExists(TopicName topicName) {
        return pathExists(paths.topicPath(topicName));
    }

    @Override
    public void ensureTopicExists(TopicName topicName) {
        if (!topicExists(topicName)) {
            throw new TopicNotExistsException(topicName);
        }
    }

    @Override
    public List<String> listTopicNames(String groupName) {
        groupRepository.ensureGroupExists(groupName);

        return childrenOf(paths.topicsPath(groupName));
    }

    @Override
    public List<Topic> listTopics(String groupName) {
        return listTopicNames(groupName).stream()
                .map(name -> getTopicDetails(new TopicName(groupName, name), true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public void createTopic(Topic topic) {
        groupRepository.ensureGroupExists(topic.getName().getGroupName());

        String topicPath = paths.topicPath(topic.getName());
        logger.info("Creating topic for path {}", topicPath);

        try {
            zookeeper.inTransaction()
                    .create().forPath(topicPath, mapper.writeValueAsBytes(topic))
                    .and()
                    .create().forPath(paths.subscriptionsPath(topic.getName()))
                    .and().commit();
        } catch (KeeperException.NodeExistsException ex) {
            throw new TopicAlreadyExistsException(topic.getName(), ex);
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    @Override
    public void removeTopic(TopicName topicName) {
        ensureTopicExists(topicName);
        ensureTopicHasNoSubscriptions(topicName);
        logger.info("Removing topic: " + topicName);
        remove(paths.topicPath(topicName));
    }

    @Override
    public void ensureTopicHasNoSubscriptions(TopicName topicName) {
        List<String> children = childrenOf(paths.subscriptionsPath(topicName));
        boolean anyNodeNotEmpty = children.stream()
                .anyMatch(sub -> !isEmpty(paths.subscriptionsPath(topicName) + "/" + sub));
        if (!children.isEmpty() && anyNodeNotEmpty) {
            throw new TopicNotEmptyException(topicName);
        }
    }

    @Override
    public void updateTopic(Topic topic) {
        ensureTopicExists(topic.getName());

        logger.info("Updating topic: " + topic.getName());
        overwrite(paths.topicPath(topic.getName()), topic);
    }

    @Override
    public void touchTopic(TopicName topicName) {
        ensureTopicExists(topicName);

        logger.info("Touching topic: " + topicName.qualifiedName());
        touch(paths.topicPath(topicName));
    }

    @Override
    public Topic getTopicDetails(TopicName topicName) {
        return getTopicDetails(topicName, false).get();
    }

    @Override
    public List<Topic> getTopicsDetails(Collection<TopicName> topicNames) {
        return topicNames.stream()
                .map(topicName -> getTopicDetails(topicName, true))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSubscribingRestricted(TopicName topicName) {
        return getTopicDetails(topicName).isSubscribingRestricted();
    }

    private Optional<Topic> getTopicDetails(TopicName topicName, boolean quiet) {
        ensureTopicExists(topicName);
        return readWithStatFrom(
                paths.topicPath(topicName),
                Topic.class,
                (topic, stat) -> {
                    topic.setCreatedAt(stat.getCtime());
                    topic.setModifiedAt(stat.getMtime());
                },
                quiet
        );
    }
}
