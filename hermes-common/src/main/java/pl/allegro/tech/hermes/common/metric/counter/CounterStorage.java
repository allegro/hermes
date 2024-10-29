package pl.allegro.tech.hermes.common.metric.counter;

import pl.allegro.tech.hermes.api.TopicName;

public interface CounterStorage {

  void setTopicPublishedCounter(TopicName topicName, long count);

  void setSubscriptionDeliveredCounter(TopicName topicName, String subscriptionName, long count);

  long getTopicPublishedCounter(TopicName topicName);

  long getSubscriptionDeliveredCounter(TopicName topicName, String subscriptionName);

  void setSubscriptionDiscardedCounter(TopicName topicName, String subscription, long value);

  void incrementVolumeCounter(TopicName topicName, String subscriptionName, long value);

  void incrementVolumeCounter(TopicName topicName, long value);
}
