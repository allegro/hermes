package pl.allegro.tech.hermes.frontend.cache.topic;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.TopicCallback;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.blacklist.BlacklistZookeeperNotifyingCache;
import pl.allegro.tech.hermes.frontend.blacklist.TopicBlacklistCallback;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry;

public class NotificationBasedTopicsCache
    implements TopicCallback, TopicsCache, TopicBlacklistCallback {

  private static final Logger logger = LoggerFactory.getLogger(NotificationBasedTopicsCache.class);

  private final ConcurrentMap<String, CachedTopic> topicCache = new ConcurrentHashMap<>();

  private final GroupRepository groupRepository;
  private final TopicRepository topicRepository;
  private final MetricsFacade metricsFacade;
  private final KafkaNamesMapper kafkaNamesMapper;
  private final ThroughputRegistry throughputRegistry;

  public NotificationBasedTopicsCache(
      InternalNotificationsBus notificationsBus,
      BlacklistZookeeperNotifyingCache blacklistZookeeperNotifyingCache,
      GroupRepository groupRepository,
      TopicRepository topicRepository,
      MetricsFacade metricsFacade,
      ThroughputRegistry throughputRegistry,
      KafkaNamesMapper kafkaNamesMapper) {
    this.groupRepository = groupRepository;
    this.topicRepository = topicRepository;
    this.metricsFacade = metricsFacade;
    this.kafkaNamesMapper = kafkaNamesMapper;
    this.throughputRegistry = throughputRegistry;
    notificationsBus.registerTopicCallback(this);
    blacklistZookeeperNotifyingCache.addCallback(this);
  }

  @Override
  public void onTopicCreated(Topic topic) {
    topicCache.put(topic.getName().qualifiedName(), cachedTopic(topic));
  }

  @Override
  public void onTopicRemoved(Topic topic) {
    if (topicCache.containsKey(topic.getName().qualifiedName())) {
      Topic cachedTopic = topicCache.get(topic.getName().qualifiedName()).getTopic();
      if (cachedTopic.equals(topic)) {
        topicCache.remove(topic.getName().qualifiedName());
      } else {
        logger.warn(
            "Received event about removed topic but cache contains different topic under the same name."
                + "Cached topic {}, removed topic {}",
            cachedTopic,
            topic);
      }
    }
  }

  @Override
  public void onTopicChanged(Topic topic) {
    topicCache.put(topic.getName().qualifiedName(), cachedTopic(topic));
  }

  @Override
  public void onTopicBlacklisted(String qualifiedTopicName) {
    Optional<Topic> topic =
        Optional.ofNullable(
            Optional.ofNullable(topicCache.get(qualifiedTopicName))
                .map(CachedTopic::getTopic)
                .orElseGet(
                    () ->
                        topicRepository.getTopicDetails(
                            TopicName.fromQualifiedName(qualifiedTopicName))));

    topic.ifPresent(t -> topicCache.put(qualifiedTopicName, bannedTopic(t)));
  }

  @Override
  public void onTopicUnblacklisted(String qualifiedTopicName) {
    Optional<Topic> topic =
        Optional.ofNullable(
            Optional.ofNullable(topicCache.get(qualifiedTopicName))
                .map(CachedTopic::getTopic)
                .orElseGet(
                    () ->
                        topicRepository.getTopicDetails(
                            TopicName.fromQualifiedName(qualifiedTopicName))));

    topic.ifPresent(t -> topicCache.put(qualifiedTopicName, cachedTopic(t)));
  }

  @Override
  public Optional<CachedTopic> getTopic(String qualifiedTopicName) {
    return Optional.ofNullable(topicCache.get(qualifiedTopicName));
  }

  @Override
  public List<CachedTopic> getTopics() {
    return ImmutableList.copyOf(topicCache.values());
  }

  @Override
  public void start() {
    for (String groupName : groupRepository.listGroupNames()) {
      for (Topic topic : topicRepository.listTopics(groupName)) {
        topicCache.put(topic.getQualifiedName(), cachedTopic(topic));
      }
    }
  }

  private CachedTopic cachedTopic(Topic topic) {
    return new CachedTopic(
        topic, metricsFacade, throughputRegistry, kafkaNamesMapper.toKafkaTopics(topic));
  }

  private CachedTopic bannedTopic(Topic topic) {
    return new CachedTopic(
        topic, metricsFacade, throughputRegistry, kafkaNamesMapper.toKafkaTopics(topic), true);
  }
}
