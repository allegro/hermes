package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import static pl.allegro.tech.hermes.metrics.PathContext.pathContext;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.metrics.PathContext;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

public class ZookeeperCounterStorage implements CounterStorage {

  static final String TOPIC_PUBLISHED =
      "/groups/" + GROUP + "/topics/" + TOPIC + "/metrics/published";
  static final String TOPIC_VOLUME_COUNTER =
      "/groups/" + GROUP + "/topics/" + TOPIC + "/metrics/volume";
  static final String SUBSCRIPTION_DELIVERED =
      "/groups/"
          + GROUP
          + "/topics/"
          + TOPIC
          + "/subscriptions/"
          + SUBSCRIPTION
          + "/metrics/delivered";
  static final String SUBSCRIPTION_DISCARDED =
      "/groups/"
          + GROUP
          + "/topics/"
          + TOPIC
          + "/subscriptions/"
          + SUBSCRIPTION
          + "/metrics/discarded";
  static final String SUBSCRIPTION_VOLUME_COUNTER =
      "/groups/"
          + GROUP
          + "/topics/"
          + TOPIC
          + "/subscriptions/"
          + SUBSCRIPTION
          + "/metrics/volume";

  private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperCounterStorage.class);

  private final MetricsDeltaCalculator deltaCalculator = new MetricsDeltaCalculator();
  private final SharedCounter sharedCounter;

  private final SubscriptionRepository subscriptionRepository;
  private final PathsCompiler pathsCompiler;
  private final String zookeeperRoot;

  public ZookeeperCounterStorage(
      SharedCounter sharedCounter,
      SubscriptionRepository subscriptionRepository,
      PathsCompiler pathsCompiler,
      String zookeeperRoot) {
    this.sharedCounter = sharedCounter;
    this.subscriptionRepository = subscriptionRepository;
    this.pathsCompiler = pathsCompiler;
    this.zookeeperRoot = zookeeperRoot;
  }

  @Override
  public void setTopicPublishedCounter(TopicName topicName, long count) {
    String topicPublishedCounter = topicPublishedCounter(topicName);
    incrementSharedCounter(topicPublishedCounter, count);
  }

  @Override
  public long getTopicPublishedCounter(TopicName topicName) {
    return sharedCounter.getValue(topicPublishedCounter(topicName));
  }

  @Override
  public long getSubscriptionDeliveredCounter(TopicName topicName, String subscriptionName) {
    return sharedCounter.getValue(subscriptionDeliveredCounter(topicName, subscriptionName));
  }

  @Override
  public void setSubscriptionDeliveredCounter(
      TopicName topicName, String subscriptionName, long count) {
    try {
      subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
      incrementSharedCounter(subscriptionDeliveredCounter(topicName, subscriptionName), count);
    } catch (SubscriptionNotExistsException e) {
      LOGGER.debug(
          "Trying to report metric on not existing subscription {} {}",
          topicName,
          subscriptionName);
    }
  }

  @Override
  public void setSubscriptionDiscardedCounter(
      TopicName topicName, String subscriptionName, long count) {
    try {
      subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
      incrementSharedCounter(subscriptionDiscardedCounter(topicName, subscriptionName), count);
    } catch (SubscriptionNotExistsException e) {
      LOGGER.debug(
          "Trying to report metric on not existing subscription {} {}",
          topicName,
          subscriptionName);
    }
  }

  @Override
  public void incrementVolumeCounter(TopicName topicName, String subscriptionName, long value) {
    try {
      subscriptionRepository.ensureSubscriptionExists(topicName, subscriptionName);
      incrementSharedCounter(volumeCounterPath(topicName, subscriptionName), value);
    } catch (SubscriptionNotExistsException e) {
      LOGGER.debug(
          "Trying to report metric on not existing subscription {} {}",
          topicName,
          subscriptionName);
    }
  }

  @Override
  public void incrementVolumeCounter(TopicName topicName, long value) {
    incrementSharedCounter(topicVolumeCounter(topicName), value);
  }

  private void incrementSharedCounter(String metricPath, long count) {
    long delta = deltaCalculator.calculateDelta(metricPath, count);

    if (delta != 0 && !sharedCounter.increment(metricPath, delta)) {
      deltaCalculator.revertDelta(metricPath, delta);
    }
  }

  private String topicPublishedCounter(TopicName topicName) {
    PathContext pathContext =
        pathContext().withGroup(topicName.getGroupName()).withTopic(topicName.getName()).build();
    return pathsCompiler.compile(appendRootPath(TOPIC_PUBLISHED), pathContext);
  }

  private String subscriptionDeliveredCounter(TopicName topicName, String subscriptionName) {
    PathContext pathContext = subscriptionPathContext(topicName, subscriptionName);
    return pathsCompiler.compile(appendRootPath(SUBSCRIPTION_DELIVERED), pathContext);
  }

  private String subscriptionDiscardedCounter(TopicName topicName, String subscriptionName) {
    PathContext pathContext = subscriptionPathContext(topicName, subscriptionName);
    return pathsCompiler.compile(appendRootPath(SUBSCRIPTION_DISCARDED), pathContext);
  }

  private String volumeCounterPath(TopicName topicName, String subscriptionName) {
    PathContext pathContext = subscriptionPathContext(topicName, subscriptionName);
    return pathsCompiler.compile(appendRootPath(SUBSCRIPTION_VOLUME_COUNTER), pathContext);
  }

  private String topicVolumeCounter(TopicName topicName) {
    PathContext pathContext =
        pathContext().withGroup(topicName.getGroupName()).withTopic(topicName.getName()).build();
    return pathsCompiler.compile(appendRootPath(TOPIC_VOLUME_COUNTER), pathContext);
  }

  private PathContext subscriptionPathContext(TopicName topicName, String subscriptionName) {
    return pathContext()
        .withGroup(topicName.getGroupName())
        .withTopic(topicName.getName())
        .withSubscription(subscriptionName)
        .build();
  }

  private String appendRootPath(String path) {
    return zookeeperRoot + path;
  }
}
