package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicAlreadyExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

public class ZookeeperTopicRepository extends ZookeeperBasedRepository implements TopicRepository {

  private static final Logger logger = LoggerFactory.getLogger(ZookeeperTopicRepository.class);

  private final GroupRepository groupRepository;

  public ZookeeperTopicRepository(
      CuratorFramework zookeeper,
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
      createInTransaction(topicPath, topic, paths.subscriptionsPath(topic.getName()));
    } catch (KeeperException.NodeExistsException ex) {
      throw new TopicAlreadyExistsException(topic.getName(), ex);
    } catch (Exception ex) {
      throw new InternalProcessingException(ex);
    }
  }

  /**
   * To remove topic node, we must remove topic node and its children. The tree looks like this:
   *
   * <ul>
   *   <li>- topic
   *   <li>----- /subscriptions (required)
   *   <li>----- /preview (optional)
   *   <li>----- /metrics (optional)
   *   <li>--------------- /volume
   *   <li>--------------- /published
   * </ul>
   *
   * <p>One way to remove the whole tree for topic that would be to use <code>
   * deletingChildrenIfNeeded()</code>: e.g. <code>
   * zookeeper.delete().deletingChildrenIfNeeded().forPath(topicPath)</code>. However, <code>
   * deletingChildrenIfNeeded</code> is not atomic. It first tries to remove the node <code>topic
   * </code> and upon receiving <code>KeeperException.NotEmptyException</code> it tries to remove
   * children recursively and then retries the node removal. This means that there is a potentially
   * large time gap between removal of <code>topic/subscriptions</code> node and <code>topic</code>
   * node, especially when topic removal is being done in remote DC.
   *
   * <p>It turns out that <code>PathChildrenCache</code> used by <code>HierarchicalCacheLevel</code>
   * in Consumers and Frontend listens for <code>topics/subscriptions</code> changes and recreates
   * that node when deleted. If the recreation happens between the <code>topic/subscriptions</code>
   * and <code>topic</code> node removal than the whole removal process must be repeated resulting
   * in a lengthy loop that may even result in <code>StackOverflowException</code>. Example of that
   * scenario would be
   *
   * <ol>
   *   <li>DELETE <code>topic</code> - issued by management, fails with
   *       KeeperException.NotEmptyException
   *   <li>DELETE <code>topic/subscriptions</code> - issued by management, succeeds
   *   <li>CREATE <code>topic/subscriptions</code> - issued by frontend, succeeds
   *   <li>DELETE <code>topic</code> - issued by management, fails with
   *       KeeperException.NotEmptyException
   *   <li>[...]
   * </ol>
   *
   * <p>To solve this we must remove <code>topic</code> and <code>topic/subscriptions</code>
   * atomically. However, we must also remove other <code>topic</code> children. Transaction API
   * does not allow for optional deletes so we:
   *
   * <ol>
   *   <li>find all children paths
   *   <li>delete all children in one transaction
   * </ol>
   */
  @Override
  public void removeTopic(TopicName topicName) {
    ensureTopicExists(topicName);
    logger.info("Removing topic: " + topicName);

    List<String> pathsForRemoval = new ArrayList<>();
    String topicMetricsPath = paths.topicMetricsPath(topicName);
    if (pathExists(topicMetricsPath)) {
      pathsForRemoval.addAll(childrenPathsOf(topicMetricsPath));
      pathsForRemoval.add(topicMetricsPath);
    }

    String topicPreviewPath = paths.topicPreviewPath(topicName);
    if (pathExists(topicPreviewPath)) {
      pathsForRemoval.add(topicPreviewPath);
    }

    pathsForRemoval.add(paths.subscriptionsPath(topicName));
    pathsForRemoval.add(paths.topicPath(topicName));

    try {
      deleteInTransaction(pathsForRemoval);
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }

  @Override
  public void updateTopic(Topic topic) {
    ensureTopicExists(topic.getName());

    logger.info("Updating topic: " + topic.getName());
    try {
      overwrite(paths.topicPath(topic.getName()), topic);
    } catch (Exception e) {
      throw new InternalProcessingException(e);
    }
  }

  @Override
  public void touchTopic(TopicName topicName) {
    ensureTopicExists(topicName);

    logger.info("Touching topic: " + topicName.qualifiedName());
    try {
      touch(paths.topicPath(topicName));
    } catch (Exception ex) {
      throw new InternalProcessingException(ex);
    }
  }

  @Override
  public Topic getTopicDetails(TopicName topicName) {
    return getTopicDetails(topicName, false).get();
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
        quiet);
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
  public List<Topic> listAllTopics() {
    return groupRepository.listGroupNames().stream()
        .map(this::listTopics)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
